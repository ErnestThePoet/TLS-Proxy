package proxy.client;

import crypto.encryption.AesKey;
import crypto.encryption.DualAesKey;
import proxy.HandshakeController;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandshakeController extends HandshakeController {
    private final Socket serverSocket;
    public ClientHandshakeController(Socket serverSocket){
        super();
        this.serverSocket=serverSocket;
    }

    @Override
    public DualAesKey negotiateApplicationKey() {
        this.generateX22519KeyPair();

        try {
            var selfRandomWithPublicKey=this.getRandomWithPublicKey();
            serverSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.addTransmittedBytes(selfRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeServerSocket();
            return null;
        }

        byte[] serverRandomWithPublicKey;

        try {
            serverRandomWithPublicKey=serverSocket.getInputStream().readAllBytes();
            this.addTransmittedBytes(serverRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeServerSocket();
            return null;
        }

        var aesKey=this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        serverRandomWithPublicKey,32,serverRandomWithPublicKey.length));

        return null;
    }

    private void closeServerSocket(){
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
