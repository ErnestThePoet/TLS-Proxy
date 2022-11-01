package proxy.server;

import crypto.encryption.DualAesKey;
import proxy.HandshakeController;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

public class ServerHandshakeController extends HandshakeController {
    private final Socket clientSocket;

    public ServerHandshakeController(Socket clientSocket) {
        super();
        this.clientSocket = clientSocket;
    }

    @Override
    public DualAesKey negotiateApplicationKey() {
        this.generateX22519KeyPair();

        byte[] clientRandomWithPublicKey = new byte[64];

        try {
            int readLength = this.clientSocket.getInputStream().read(clientRandomWithPublicKey);
            if (readLength != 64) {
                throw new IOException("Client random data not of length 64");
            }

            this.addTransmittedBytes(clientRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        try {
            var selfRandomWithPublicKey = this.getRandomWithPublicKey();
            this.clientSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.clientSocket.getOutputStream().flush();
            this.addTransmittedBytes(selfRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        clientRandomWithPublicKey, 32, 64));

        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getServerKey().getKey()));
        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getServerKey().getIv()));
        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getClientKey().getKey()));
        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getClientKey().getIv()));

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
