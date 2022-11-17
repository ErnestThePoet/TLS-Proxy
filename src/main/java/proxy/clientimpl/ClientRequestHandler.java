package proxy.clientimpl;

import config.client.ClientConfigManager;
import crypto.encryption.Aes;
import handshake.clientimpl.ClientHandshakeController;
import handshake.HandshakeController;
import proxy.RequestHandler;
import utils.Log;
import utils.http.HttpUtil;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class ClientRequestHandler extends RequestHandler implements Runnable {
    public ClientRequestHandler(Socket clientSocket,int timeout) {
        super(clientSocket,timeout);
    }

    private void encryptAndSendToServer(byte[] data) throws IOException {
        this.serverSocket.getOutputStream().write(
                Aes.encrypt(data, this.applicationKey.clientKey())
        );
        this.serverSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromServer(byte[] data) {
        return Aes.decrypt(data, this.applicationKey.serverKey());
    }

    @Override
    public void run() {
        byte[] clientData = new byte[8 * 1024 * 1024];
        try {
            int clientDataLength = this.clientSocket.getInputStream().read(clientData);
            if (clientDataLength == -1) {
                throw new IOException("Read got -1");
            }
            clientData = Arrays.copyOf(clientData, clientDataLength);
        } catch (IOException e) {
            // when browser is using cache, client timeout often occurs
            if(!(e instanceof SocketTimeoutException)){
                e.printStackTrace();
            }
            this.closeClientSocket();
            return;
        }

        var hostAndShortenPathResult =
                HttpUtil.getRequestHeaderHostAndShortenPath(clientData);

        String host = hostAndShortenPathResult.host();
        String path = hostAndShortenPathResult.newPath();
        clientData = hostAndShortenPathResult.newRequestData();

        if (host == null) {
            Log.error("Cannot get host from request header");
            this.closeClientSocket();
            return;
        }

        if (path == null) {
            Log.warn("Cannot get path from request header");
        }

        if (!ClientConfigManager.isTargetHost(host)) {
            //Log.info("Ignore request to " + host + url);
            this.closeClientSocket();
            return;
        }

        var serverPort = HttpUtil.extractHostPort(host);

        this.connectToServer(serverPort.host(), serverPort.port());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host for " + host + path);
            this.closeClientSocket();
            return;
        }

        try {
            this.serverSocket.setSoTimeout(this.timeout);
        } catch (SocketException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Negotiating application key for " + host + path);

        HandshakeController handshakeController =
                new ClientHandshakeController(this.serverSocket,host);

        this.applicationKey = handshakeController.negotiateApplicationKey();

        if(this.applicationKey==null){
            Log.error("Application key negotiation failed for "+host+path);
            this.closeBothSockets();
            return;
        }

        Log.success("Successfully calculated application key for " + host + path
                + " Sending encrypted request data...");

        // Forward encrypted client data
        try {
            this.encryptAndSendToServer(clientData);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Receiving server data and sending back to client for " + host + path);
        // Send back decrypted response data or chunked data
        byte[] serverData = new byte[2 * 1024 * 1024];
        int serverDataLength;

        try {
            while (true) {
                serverDataLength = this.serverSocket.getInputStream().read(serverData);

                if (serverDataLength == -1 || (serverDataLength == 1 && serverData[0] == 0)) {
                    break;
                }

                byte[] actualServerData =
                        this.decryptDataFromServer(Arrays.copyOf(serverData, serverDataLength));

                this.clientSocket.getOutputStream().write(actualServerData);
                this.clientSocket.getOutputStream().flush();

                // Synchronize
                this.serverSocket.getOutputStream().write(new byte[1]);
                this.serverSocket.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.success("All data sent back to client for " + host + path);

        this.closeBothSockets();
    }
}
