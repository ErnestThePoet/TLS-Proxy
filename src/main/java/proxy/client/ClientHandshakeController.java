package proxy.client;

import crypto.encryption.DualAesKey;
import proxy.HandshakeController;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

public class ClientHandshakeController extends HandshakeController {
    private final Socket hostSocket;


    public ClientHandshakeController(Socket hostSocket) {
        super();
        this.hostSocket = hostSocket;
    }

    @Override
    public DualAesKey negotiateApplicationKey() {
        this.generateX22519KeyPair();

        try {
            var selfRandomWithPublicKey = this.getRandomWithPublicKey();
            this.hostSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.hostSocket.getOutputStream().flush();
            this.addTransmittedBytes(selfRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        byte[] serverRandomWithPublicKey = new byte[64];

        try {
            int readLength = this.hostSocket.getInputStream().read(serverRandomWithPublicKey);
            if (readLength != 64) {
                throw new IOException("Server random data not of length 64");
            }

            this.addTransmittedBytes(serverRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeHostSocket();
            return null;
        }

        this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        serverRandomWithPublicKey, 32, 64));

        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getServerKey().getKey()));
        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getServerKey().getIv()));
        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getClientKey().getKey()));
        System.out.println(Base64.getEncoder().encodeToString(this.handshakeKey.getClientKey().getIv()));

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
