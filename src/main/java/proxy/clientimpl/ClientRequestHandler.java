package proxy.clientimpl;

import communication.SynchronizedTransceiver;
import config.clientimpl.ClientConfigManager;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import exceptions.TlsException;
import handshake.clientimpl.ClientHandshakeController;
import handshake.HandshakeController;
import proxy.RequestHandler;
import proxy.clientimpl.htmlresponse.HtmlResponseProvider;
import utils.Log;
import utils.http.HttpUtil;
import utils.http.objs.HttpRequestInfo;

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

    private void sendErrorPage(String message, String url) {
        try {
            this.clientSocket.getOutputStream().write(
                    HtmlResponseProvider.getErrorPageResponse(message));
        } catch (IOException e) {
            if (url != null) {
                Log.error(e.getClass().getName() + e.getMessage(), url);
            }
            e.printStackTrace();
        }
    }

    private void sendNotTargetHostPage(String host, String url) {
        try {
            this.clientSocket.getOutputStream().write(
                    HtmlResponseProvider.getNotTargetHostPageResponse(host));
        } catch (IOException e) {
            if (url != null) {
                Log.error(e.getClass().getName() + e.getMessage(), url);
            }
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] clientData = new byte[8 * 1024 * 1024];
        try {
            int clientDataLength = this.clientSocket.getInputStream().read(clientData);
            if (clientDataLength == -1) {
                throw new IOException("read() returned -1");
            }
            clientData = Arrays.copyOf(clientData, clientDataLength);
        } catch (IOException e) {
            // when browser is using cache, client timeout often occurs
            if (!(e instanceof SocketTimeoutException)) {
                e.printStackTrace();
            }

            e.printStackTrace();
            this.sendErrorPage(e.getClass().getName() + e.getMessage(), null);
            this.closeClientSocket();
            return;
        }

        var requestString = Utf8.encode(clientData);
        var requestInfo = new HttpRequestInfo(requestString);

        var newPath = HttpUtil.shortenPath(requestInfo.getHost(), requestInfo.getPath());

        clientData = HttpUtil.replaceRequestPath(newPath, clientData);

        requestInfo.setPath(newPath);

        if (requestInfo.getHost() == null) {
            Log.error("Cannot get host from request header");
            this.sendErrorPage("Cannot get host from request header", null);
            this.closeClientSocket();
            return;
        }

        if (requestInfo.getPath() == null) {
            Log.warn("Cannot get path from request header");
        }

        var url = requestInfo.getHost() + requestInfo.getPath();

        if (!ClientConfigManager.isTargetHost(requestInfo.getHost())) {
            this.sendNotTargetHostPage(requestInfo.getHost(), url);
            this.closeClientSocket();
            return;
        }

        this.connectToServer(requestInfo.getHostName(), requestInfo.getHostPort());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host", url);
            this.sendErrorPage("Cannot connect to host", url);
            this.closeClientSocket();
            return;
        }

        try {
            this.serverSocket.setSoTimeout(ClientConfigManager.getTimeout());
        } catch (SocketException e) {
            Log.error(e.getClass().getName() + e.getMessage(), url);
            e.printStackTrace();
            this.sendErrorPage(e.getClass().getName() + e.getMessage(), url);
            this.closeBothSockets();
            return;
        }

        Log.info("Negotiating application key", url);

        HandshakeController handshakeController =
                new ClientHandshakeController(this.serverSocket, requestInfo.getHost());

        try {
            this.applicationKey = handshakeController.negotiateApplicationKey();
        } catch (IOException | TlsException e) {
            Log.error(e.getClass().getName() + e.getMessage(), url);
            e.printStackTrace();
            this.sendErrorPage(e.getClass().getName() + e.getMessage(), url);
            this.closeBothSockets();
            return;
        }

        if (this.applicationKey == null) {
            Log.error("Application key negotiation failed", url);
            this.sendErrorPage("Application key negotiation failed", url);
            this.closeBothSockets();
            return;
        }

        Log.info("Sending encrypted request data", url);

        this.synchronizedTransceiver = new SynchronizedTransceiver(this.serverSocket);

        // Forward encrypted client data
        try {
            this.synchronizedTransceiver.sendData(this.encryptDataForServer(clientData));
        } catch (IOException e) {
            Log.error(e.getClass().getName() + e.getMessage(), url);
            e.printStackTrace();
            this.sendErrorPage(e.getClass().getName() + e.getMessage(), url);
            this.closeBothSockets();
            return;
        }

        Log.info("Receiving encrypted response data and delivering", url);
        // Send back decrypted response data

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
            Log.error(e.getClass().getName() + e.getMessage(), url);
            e.printStackTrace();
            this.sendErrorPage(e.getClass().getName() + e.getMessage(), url);
            this.closeBothSockets();
            return;
        }

        Log.success("All response data delivered", url);

        this.closeBothSockets();
    }
}
