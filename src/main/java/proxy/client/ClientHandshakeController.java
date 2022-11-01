package proxy.client;

import certificate.CertificateValidator;
import crypto.encryption.Aes;
import crypto.encryption.DualAesKey;
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
        // Generate key pair and random, send to server
        this.generateX22519KeyPair();

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
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        // Negotiate handshake key
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
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        var certificate= Aes.decrypt(
                trafficSignatureEncrypted,this.handshakeKey.getServerKey());
        var trafficSignature=Aes.decrypt(
                trafficSignatureEncrypted,this.handshakeKey.getServerKey());

        CertificateValidator certificateValidator=CertificateValidator.getInstance();

        if(!certificateValidator.validateCertificate(certificate)){
            Log.error("Certificate validation failed");
            return null;
        }

        if(!certificateValidator.validateTrafficSignature(
                certificate, ByteArrayUtil.concat(this.getTrafficConcat()),trafficSignature)){
            Log.error("Traffic signature validation failed");
            return null;
        }

        // Server traffic signature does not include traffic signature
        this.addTraffic(trafficSignatureEncrypted);

        Log.info("SUCCESS");

        return null;
    }

    private void closeHostSocket() {
        try {
            this.hostSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
