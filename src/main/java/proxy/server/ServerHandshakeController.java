package proxy.server;

import crypto.encryption.DualAesKey;
import proxy.HandshakeController;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ServerHandshakeController extends HandshakeController {
    private final Socket clientSocket;

    public ServerHandshakeController(Socket clientSocket){
        super();
        this.clientSocket=clientSocket;
    }

    @Override
    public DualAesKey negotiateApplicationKey() {
        this.generateX22519KeyPair();

        byte[] clientRandomWithPublicKey;

        try {
            clientRandomWithPublicKey=this.clientSocket.getInputStream().readAllBytes();
            this.addTransmittedBytes(clientRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        try {
            var selfRandomWithPublicKey=this.getRandomWithPublicKey();
            this.clientSocket.getOutputStream().write(selfRandomWithPublicKey);
            this.addTransmittedBytes(selfRandomWithPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return null;
        }

        var aesKey=this.calculateHandshakeKey(
                Arrays.copyOfRange(
                        clientRandomWithPublicKey,32,clientRandomWithPublicKey.length));

        return null;
    }

    private void closeClientSocket(){
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
