package proxy.server;

import certificate.CertificateProvider;
import crypto.encryption.Aes;
import crypto.encryption.DualAesKey;
import proxy.HandshakeController;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ServerHandshakeController extends HandshakeController {
    private final Socket clientSocket;

    public ServerHandshakeController(Socket clientSocket) {
        super();
        this.clientSocket = clientSocket;
    }

    @Override
    public DualAesKey negotiateApplicationKey() {
        // Generate key pair and random
        this.generateX22519KeyPair();

        // Receive client's key pair and random
        byte[] clientRandomWithPublicKey = new byte[64];

        try {
            int readLength = this.clientSocket.getInputStream().read(clientRandomWithPublicKey);
            if (readLength != 64) {
                throw new IOException("Client random data not of length 64");
            }

            this.addTraffic(clientRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // Send key pair and random to client
        try {
            var selfRandomWithPublicKey = this.getRandomWithPublicKey();
            this.clientSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(selfRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // Negotiate handshake key
        this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        clientRandomWithPublicKey, 32, 64));

        CertificateProvider certificateProvider=CertificateProvider.getInstance();

        // Send encrypted certificate to client
        try {
            var encryptedCertificate = Aes.encrypt(
                    certificateProvider.getCertificate(),this.handshakeKey.getServerKey());
            this.clientSocket.getOutputStream().write(encryptedCertificate);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(encryptedCertificate);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // Send encrypted traffic signature to client
        try {
            var encryptedTrafficSignature = Aes.encrypt(
                    certificateProvider.signTraffic(
                            this.getTrafficConcat()), this.handshakeKey.getServerKey());
            this.clientSocket.getOutputStream().write(encryptedTrafficSignature);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(encryptedTrafficSignature);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        return null;
    }

    private void closeClientSocket() {
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
