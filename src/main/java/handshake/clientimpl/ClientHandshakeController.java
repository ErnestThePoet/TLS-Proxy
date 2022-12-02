package handshake.clientimpl;

import exceptions.TlsException;
import handshake.HandshakeController;
import handshake.certificate.CertificateValidator;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import crypto.encryption.objs.DualAesKey;
import crypto.hmac.HmacSha384;
import crypto.kdf.HkdfSha384;
import utils.ByteArrayUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandshakeController extends HandshakeController {
    private final String host;

    public ClientHandshakeController(Socket remoteSocket, String host) {
        super(remoteSocket);
        this.host = host;
    }

    @Override
    public DualAesKey negotiateApplicationKey() throws IOException, TlsException {
        // [Server Key Exchange Generation]
        // [Client Key Exchange Generation] Generate key pair and random
        this.generateX22519KeyPair();

        // [Client Hello] Send key pair and random to server
        var sendPacket = this.synchronizedTransceiver.sendData(
                this.getRandomWithPublicKey());
        this.addTraffic(sendPacket);

        // [Server Hello] Receive server's key pair and random
        var receivedPacketAndData = this.synchronizedTransceiver.receiveData();
        this.addTraffic(receivedPacketAndData.packet());

        // [Server Handshake Keys Calc]
        // [Client Handshake Keys Calc] Negotiate handshake key
        this.calculateHandshakeKey(
                Arrays.copyOfRange(receivedPacketAndData.data(), 32, 64));

        // [Server Certificate] Receive encrypted certificate
        receivedPacketAndData = this.synchronizedTransceiver.receiveData();
        this.addTraffic(receivedPacketAndData.packet());
        var certificateEncrypted = receivedPacketAndData.data();

        // [Server Certificate Verify] Receive encrypted traffic signature
        receivedPacketAndData = this.synchronizedTransceiver.receiveData();
        var trafficSignatureEncrypted = receivedPacketAndData.data();

        var certificate = Aes.decrypt(
                certificateEncrypted, this.handshakeKey.serverKey());
        var trafficSignature = Aes.decrypt(
                trafficSignatureEncrypted, this.handshakeKey.serverKey());

        CertificateValidator certificateValidator = CertificateValidator.getInstance();

        var certificateValidationResult =
                certificateValidator.validateCertificate(certificate, this.host);
        if (!certificateValidationResult.success()) {
            throw new TlsException("服务器证书验证失败："
                    + certificateValidationResult.message());
        }

        if (!certificateValidator.validateTrafficSignature(
                certificate,
                ByteArrayUtil.concat(this.getTrafficConcat()), trafficSignature)) {
            throw new TlsException("通信签名验证失败");
        }

        // Server traffic signature does not include traffic signature
        this.addTraffic(receivedPacketAndData.packet());

        // [Server Handshake Finished] Receive encrypted traffic hash
        receivedPacketAndData = this.synchronizedTransceiver.receiveData();

        if (!HmacSha384.verify(
                HkdfSha384.expand(this.serverSecret, Utf8.decode("finished"), 32),
                this.getTrafficHash(),
                Aes.decrypt(receivedPacketAndData.data(), this.handshakeKey.serverKey()))) {
            throw new TlsException("通信哈希值(Server Finished)验证失败");
        }

        this.addTraffic(receivedPacketAndData.packet());

        // [Server Application Keys Calc]
        // [Client Application Keys Calc]
        this.calculateApplicationKey();

        // [Client Handshake Finished] Send encrypted traffic hash to server
        this.synchronizedTransceiver.sendData(Aes.encrypt(
                HmacSha384.mac(
                        HkdfSha384.expand(this.clientSecret, Utf8.decode("finished"), 32),
                        this.getTrafficHash()),
                this.handshakeKey.clientKey()));

        return this.applicationKey;
    }
}
