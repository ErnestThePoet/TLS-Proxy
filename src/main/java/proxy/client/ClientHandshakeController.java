package proxy.client;

import certificate.CertificateValidator;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import crypto.encryption.DualAesKey;
import crypto.hmac.HmacSha384;
import crypto.kdf.HkdfSha384;
import proxy.HandshakeController;
import utils.ByteArrayUtil;
import utils.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandshakeController extends HandshakeController {
    private final Socket hostSocket;


    public ClientHandshakeController(Socket hostSocket) {
        super();
        this.hostSocket = hostSocket;
    }

    @Override
    public DualAesKey negotiateApplicationKey() {
        // [Client Key Exchange Generation] Generate key pair and random
        this.generateX22519KeyPair();

        // [Client Hello] Send key pair and random to server
        try {
            var selfRandomWithPublicKey = this.getRandomWithPublicKey();
            this.hostSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.hostSocket.getOutputStream().flush();
            this.addTraffic(selfRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        // Receive server's key pair and random
        byte[] serverRandomWithPublicKey = new byte[64];

        try {
            int readLength = this.hostSocket.getInputStream().read(serverRandomWithPublicKey);
            if (readLength != 64) {
                throw new IOException("Server random data not of length 64");
            }

            this.addTraffic(serverRandomWithPublicKey);

            // Synchronize
            this.hostSocket.getOutputStream().write(new byte[1]);
            this.hostSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        // [Client Handshake Keys Calc] Negotiate handshake key
        this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        serverRandomWithPublicKey, 32, 64));

        // Receive encrypted certificate
        byte[] certificateEncrypted=new byte[4096];
        int certificateEncryptedLength;

        try{
            certificateEncryptedLength=
                    this.hostSocket.getInputStream().read(certificateEncrypted);
            certificateEncrypted=Arrays.copyOf(certificateEncrypted,certificateEncryptedLength);
            this.addTraffic(certificateEncrypted);

            // Synchronize
            this.hostSocket.getOutputStream().write(new byte[1]);
            this.hostSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        // Receive encrypted traffic signature
        byte[] trafficSignatureEncrypted=new byte[4096];
        int trafficSignatureEncryptedLength;

        try{
            trafficSignatureEncryptedLength=
                    this.hostSocket.getInputStream().read(trafficSignatureEncrypted);
            trafficSignatureEncrypted=
                    Arrays.copyOf(trafficSignatureEncrypted,trafficSignatureEncryptedLength);

            // Synchronize
            this.hostSocket.getOutputStream().write(new byte[1]);
            this.hostSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        var certificate= Aes.decrypt(
                trafficSignatureEncrypted,this.handshakeKey.getServerKey());
        var trafficSignature=Aes.decrypt(
                trafficSignatureEncrypted,this.handshakeKey.getServerKey());

        CertificateValidator certificateValidator=CertificateValidator.getInstance();

        if(!certificateValidator.validateCertificate(certificate)){
            Log.error("Certificate validation failed");
            this.closeHostSocket();
            return null;
        }

        if(!certificateValidator.validateTrafficSignature(
                certificate, ByteArrayUtil.concat(this.getTrafficConcat()),trafficSignature)){
            Log.error("Traffic signature validation failed");
            this.closeHostSocket();
            return null;
        }

        // Server traffic signature does not include traffic signature
        this.addTraffic(trafficSignatureEncrypted);


        // Receive encrypted traffic hash
        byte[] trafficHashEncrypted=new byte[4096];
        int trafficHashEncryptedLength;
        try{
            trafficHashEncryptedLength=
                    this.hostSocket.getInputStream().read(trafficHashEncrypted);
            trafficHashEncrypted=
                    Arrays.copyOf(trafficHashEncrypted,trafficHashEncryptedLength);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        if(!HmacSha384.verify(HkdfSha384.expand(this.serverSecret, Utf8.decode("finished"),32),
                this.getTrafficHash(),
                Aes.decrypt(trafficHashEncrypted,this.handshakeKey.getServerKey()))){
            Log.error("Traffic hash (Server Finished) verification failed");
            this.closeHostSocket();
            return null;
        }

        this.addTraffic(trafficHashEncrypted);

        // [Client Application Keys Calc]
        this.calculateApplicationKey();

        // [Client Handshake Finished] Send encrypted traffic hash to server
        try {
            var encryptedTrafficHash = Aes.encrypt(
                    HmacSha384.mac(
                            HkdfSha384.expand(this.clientSecret, Utf8.decode("finished"),32),
                            this.getTrafficHash()),
                    this.handshakeKey.getClientKey());
            this.hostSocket.getOutputStream().write(encryptedTrafficHash);
            this.hostSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        return this.applicationKey;
    }

    private void closeHostSocket() {
        try {
            this.hostSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
