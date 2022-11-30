package proxy.clientimpl;

import communication.SynchronizedTransceiver;
import config.clientimpl.ClientConfigManager;
import crypto.encryption.Aes;
import exceptions.TlsException;
import handshake.clientimpl.ClientHandshakeController;
import handshake.HandshakeController;
import proxy.RequestHandler;
import proxy.clientimpl.htmlresponse.HtmlResponseProvider;
import utils.Log;
import utils.http.HttpUtil;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class ClientRequestHandler extends RequestHandler implements Runnable {
    public ClientRequestHandler(Socket clientSocket) {
        super(clientSocket);
    }

    private byte[] encryptDataForServer(byte[] data) {
        return Aes.encrypt(data, this.applicationKey.clientKey());
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
            if (!(e instanceof SocketTimeoutException)) {
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
            try {
                this.clientSocket.getOutputStream().write(
                        HtmlResponseProvider.getNotTargetHostPageResponse(host));
            } catch (IOException e) {
                e.printStackTrace();
            }

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
            this.serverSocket.setSoTimeout(ClientConfigManager.getTimeout());
        } catch (SocketException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Negotiating application key for " + host + path);

        HandshakeController handshakeController =
                new ClientHandshakeController(this.serverSocket, host);

        try {
            this.applicationKey = handshakeController.negotiateApplicationKey();
        } catch (IOException | TlsException e) {
            e.printStackTrace();
            try {
                this.clientSocket.getOutputStream().write(
                        HtmlResponseProvider.getErrorPageResponse(
                                e.getClass().getName() + e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            this.closeBothSockets();
            return;
        }

        if (this.applicationKey == null) {
            Log.error("Application key negotiation failed for " + host + path);
            this.closeBothSockets();
            return;
        }

        Log.success("Successfully calculated application key for " + host + path
                + " Sending encrypted request data...");

        this.synchronizedTransceiver = new SynchronizedTransceiver(this.serverSocket);

        // Forward encrypted client data
        try {
            this.synchronizedTransceiver.sendData(this.encryptDataForServer(clientData));
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Receiving server data and sending back to client for " + host + path);
        // Send back decrypted response data or chunked data

        try {
            while (true) {
                var serverData = this.synchronizedTransceiver.receiveData().data();

                // finish signal
                if ((serverData.length == 1 && serverData[0] == 0)) {
                    break;
                }

                byte[] actualServerData = this.decryptDataFromServer(serverData);

                this.clientSocket.getOutputStream().write(actualServerData);
                this.clientSocket.getOutputStream().flush();
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
