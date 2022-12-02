package handshake.serverimpl;

import exceptions.TlsException;
import handshake.HandshakeController;
import handshake.certificate.CertificateProvider;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import crypto.encryption.objs.DualAesKey;
import crypto.hmac.HmacSha384;
import crypto.kdf.HkdfSha384;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ServerHandshakeController extends HandshakeController {

    public ServerHandshakeController(Socket remoteSocket) {
        super(remoteSocket);
    }

    @Override
    public DualAesKey negotiateApplicationKey() throws IOException, TlsException {
        // [Client Key Exchange Generation]
        // [Server Key Exchange Generation] Generate key pair and random
        this.generateX22519KeyPair();

        // [Client Hello] Receive client's key pair and random
        var receivedPacketAndData = this.synchronizedTransceiver.receiveData();
        this.addTraffic(receivedPacketAndData.packet());

        // [Server Hello] Send key pair and random to client
        var sendPacket = this.synchronizedTransceiver.sendData(this.getRandomWithPublicKey());
        this.addTraffic(sendPacket);

        // [Client Handshake Keys Calc]
        // [Server Handshake Keys Calc] Negotiate handshake key
        this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        receivedPacketAndData.data(), 32, 64));

        CertificateProvider certificateProvider = CertificateProvider.getInstance();

        // [Server Certificate] Send encrypted certificate to client
        var certificate = certificateProvider.getCertificate();
        if (certificate == null) {
            throw new TlsException("无法获取证书");
        }

        sendPacket = this.synchronizedTransceiver.sendData(
                Aes.encrypt(certificate, this.handshakeKey.serverKey()));
        this.addTraffic(sendPacket);

        // [Server Certificate Verify] Send encrypted traffic signature to client
        var trafficSignature = certificateProvider.signTraffic(
                this.getTrafficConcat());
        if (trafficSignature == null) {
            throw new TlsException("通信签名失败");
        }

        sendPacket = this.synchronizedTransceiver.sendData(
                Aes.encrypt(trafficSignature, this.handshakeKey.serverKey()));
        this.addTraffic(sendPacket);

        // [Server Handshake Finished] Send encrypted traffic hash to client
        sendPacket = this.synchronizedTransceiver.sendData(Aes.encrypt(
                HmacSha384.mac(
                        HkdfSha384.expand(this.serverSecret, Utf8.decode("finished"), 32),
                        this.getTrafficHash()),
                this.handshakeKey.serverKey()));
        this.addTraffic(sendPacket);

        // [Server Application Keys Calc]
        // [Client Application Keys Calc]
        this.calculateApplicationKey();

        // [Client Handshake Finished] Receive encrypted traffic hash
        receivedPacketAndData = this.synchronizedTransceiver.receiveData();
        byte[] trafficHashEncrypted = receivedPacketAndData.data();

        if (!HmacSha384.verify(
                HkdfSha384.expand(this.clientSecret, Utf8.decode("finished"), 32),
                this.getTrafficHash(),
                Aes.decrypt(trafficHashEncrypted, this.handshakeKey.clientKey()))) {
            throw new TlsException("通信哈希值(Client Finished)验证失败");
        }

        return this.applicationKey;
    }
}
