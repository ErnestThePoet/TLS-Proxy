package proxy.client;

import config.client.ClientConfigManager;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import proxy.HandshakeController;
import proxy.RequestHandler;
import utils.Log;
import utils.http.HostPortExtractor;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ClientRequestHandler extends RequestHandler implements Runnable {
    public ClientRequestHandler(Socket clientSocket) {
        super(clientSocket);
    }

    private String getRequestHost(byte[] requestData) {
        var allLines= Utf8.encode(requestData).split("\r\n");

        for(var i:allLines) {
            if (i.startsWith("Host:")) {
                return i
                        .replace("Host:", "")
                        .replace(" ", "");
            }
        }

        return null;
    }

    private void encryptAndSendToServer(byte[] data) throws IOException {
        this.serverSocket.getOutputStream().write(
                Aes.encrypt(data, this.applicationKey.getClientKey())
        );
        this.serverSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromServer(byte[] data){
        return Aes.decrypt(data,this.applicationKey.getServerKey());
    }

    @Override
    public void run() {
        byte[] clientData = new byte[8 * 1024 * 1024];
        try {
            int clientDataLength = this.clientSocket.getInputStream().read(clientData);
            clientData = Arrays.copyOf(clientData, clientDataLength);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        String host = this.getRequestHost(clientData);

        if (host == null) {
            Log.error("Cannot get host from request header");
            this.closeClientSocket();
            return;
        }

        if (!ClientConfigManager.isTargetHost(host)) {
            Log.info("Ignore request to " + host);
            this.closeClientSocket();
            return;
        }

        var serverPort = HostPortExtractor.extract(host);

        this.connectToServer(serverPort.getHost(), serverPort.getPort());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host");
            this.closeClientSocket();
            return;
        }

        Log.info("Negotiating application key with " + host);

        HandshakeController handshakeController =
                new ClientHandshakeController(this.serverSocket);

        this.applicationKey = handshakeController.negotiateApplicationKey();

        Log.info("Successfully calculated application key with " + host
                + ". Sending encrypted request data...");

        // Forward encrypted client data
        try {
            this.encryptAndSendToServer(clientData);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        Log.info("Receiving server data and sending back to client");
        // Send back decrypted response data or chunked data
        byte[] serverData=new byte[1024*1024];
        int serverDataLength;

        try{
            while(true) {
                serverDataLength = this.serverSocket.getInputStream().read(serverData);
                if(serverDataLength==-1){
                    break;
                }

                byte[] actualServerData =
                        this.decryptDataFromServer(Arrays.copyOf(serverData, serverDataLength));

                this.clientSocket.getOutputStream().write(actualServerData);
                this.clientSocket.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("All data sent back to client");

        this.closeBothSockets();
    }
}
