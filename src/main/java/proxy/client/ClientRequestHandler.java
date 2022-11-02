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

    private record HostPathAndNewRequestData(String host, String newPath, byte[] newRequestData) {
    }

    protected HostPathAndNewRequestData getRequestHostAndReplacePath(byte[] requestData) {
        var allLines = Utf8.encode(requestData).split("\r\n");

        String host = null;

        for (var i : allLines) {
            if (i.startsWith("Host:")) {
                host = i
                        .replace("Host:", "")
                        .replace(" ", "");
                break;
            }
        }

        if (host == null) {
            return new HostPathAndNewRequestData(null, null, null);
        }

        var firstLineSplit = allLines[0].split(" ");
        if (firstLineSplit.length != 3) {
            return new HostPathAndNewRequestData(host, null, null);
        }

        var newPath = firstLineSplit[1]
                .replace("http://", "")
                .replace(host, "");

        byte[] newRequestData = new byte[firstLineSplit[0].length()
                + newPath.length() + 2
                + firstLineSplit[2].length()
                + requestData.length - allLines[0].length()];

        System.arraycopy(Utf8.decode(firstLineSplit[0]), 0,
                newRequestData, 0, firstLineSplit[0].length());

        System.arraycopy(Utf8.decode(" " + newPath + " "), 0,
                newRequestData, firstLineSplit[0].length(), newPath.length() + 2);

        System.arraycopy(Utf8.decode(firstLineSplit[2]), 0,
                newRequestData, firstLineSplit[0].length() + newPath.length() + 2,
                firstLineSplit[2].length());

        System.arraycopy(requestData, allLines[0].length(),
                newRequestData,
                firstLineSplit[0].length() + newPath.length() + 2 + firstLineSplit[2].length(),
                requestData.length - allLines[0].length());

        //Log.info(String.format("Replaced header path [%s] with [%s]",firstLineSplit[1],newPath));

        return new HostPathAndNewRequestData(host, newPath, newRequestData);
    }

    private void encryptAndSendToServer(byte[] data) throws IOException {
        this.serverSocket.getOutputStream().write(
                Aes.encrypt(data, this.applicationKey.getClientKey())
        );
        this.serverSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromServer(byte[] data) {
        return Aes.decrypt(data, this.applicationKey.getServerKey());
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
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        var replaceData = this.getRequestHostAndReplacePath(clientData);

        String host = replaceData.host;
        String path = replaceData.newPath;
        clientData = replaceData.newRequestData;

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

        var serverPort = HostPortExtractor.extract(host);

        this.connectToServer(serverPort.getHost(), serverPort.getPort());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host for " + host + path);
            this.closeClientSocket();
            return;
        }

        Log.info("Negotiating application key for " + host + path);

        HandshakeController handshakeController =
                new ClientHandshakeController(this.serverSocket);

        this.applicationKey = handshakeController.negotiateApplicationKey();

        Log.info("Successfully calculated application key for " + host + path
                + " Sending encrypted request data...");

        // Forward encrypted client data
        try {
            this.encryptAndSendToServer(clientData);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        Log.info("Receiving server data and sending back to client for " + host + path);
        // Send back decrypted response data or chunked data
        byte[] serverData = new byte[2 * 1024 * 1024];
        int serverDataLength;

        try {
            int i = 0;
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
