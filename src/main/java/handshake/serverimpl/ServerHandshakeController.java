package handshake.serverimpl;

import handshake.HandshakeController;
import handshake.certificate.CertificateProvider;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import crypto.encryption.DualAesKey;
import crypto.hmac.HmacSha384;
import crypto.kdf.HkdfSha384;
import utils.Log;

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
        // [Server Key Exchange Generation] Generate key pair and random
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

        // [Server Hello] Send key pair and random to client
        try {
            var selfRandomWithPublicKey = this.getRandomWithPublicKey();
            this.clientSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(selfRandomWithPublicKey);

            // Synchronize
            int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
            if (syncLength != 1) {
                throw new IOException("Client sync data not of length 1");
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // [Server Handshake Keys Calc] Negotiate handshake key
        this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        clientRandomWithPublicKey, 32, 64));

        CertificateProvider certificateProvider=CertificateProvider.getInstance();

        // [Server Certificate] Send encrypted certificate to client
        try {
            var encryptedCertificate = Aes.encrypt(
                    certificateProvider.getCertificate(),this.handshakeKey.getServerKey());
            this.clientSocket.getOutputStream().write(encryptedCertificate);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(encryptedCertificate);

            // Synchronize
            int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
            if (syncLength != 1) {
                throw new IOException("Client sync data not of length 1");
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // [Server Certificate Verify] Send encrypted traffic signature to client
        try {
            var encryptedTrafficSignature = Aes.encrypt(
                    certificateProvider.signTraffic(
                            this.getTrafficConcat()), this.handshakeKey.getServerKey());
            this.clientSocket.getOutputStream().write(encryptedTrafficSignature);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(encryptedTrafficSignature);

            // Synchronize
            int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
            if (syncLength != 1) {
                throw new IOException("Client sync data not of length 1");
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // [Server Handshake Finished] Send encrypted traffic hash to client
        try {
            var encryptedTrafficHash = Aes.encrypt(
                    HmacSha384.mac(
                            HkdfSha384.expand(this.serverSecret, Utf8.decode("finished"),32),
                            this.getTrafficHash()),
                    this.handshakeKey.getServerKey());
            this.clientSocket.getOutputStream().write(encryptedTrafficHash);
            this.clientSocket.getOutputStream().flush();
            this.addTraffic(encryptedTrafficHash);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        // [Server Application Keys Calc]
        this.calculateApplicationKey();

        // Receive encrypted traffic hash
        byte[] trafficHashEncrypted=new byte[4096];
        int trafficHashEncryptedLength;
        try{
            trafficHashEncryptedLength=
                    this.clientSocket.getInputStream().read(trafficHashEncrypted);
            trafficHashEncrypted=
                    Arrays.copyOf(trafficHashEncrypted,trafficHashEncryptedLength);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        if(!HmacSha384.verify(HkdfSha384.expand(this.clientSecret, Utf8.decode("finished"),32),
                this.getTrafficHash(),
                Aes.decrypt(trafficHashEncrypted,this.handshakeKey.getClientKey()))){
            Log.error("Traffic hash (Client Finished) verification failed");
            this.closeClientSocket();
            return null;
        }

        return this.applicationKey;
    }

    private void closeClientSocket() {
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
