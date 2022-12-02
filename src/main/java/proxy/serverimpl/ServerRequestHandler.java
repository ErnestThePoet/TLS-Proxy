package proxy.serverimpl;

import communication.SynchronizedTransceiver;
import config.serverimpl.ServerConfigManager;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import exceptions.TlsException;
import handshake.HandshakeController;
import handshake.serverimpl.ServerHandshakeController;
import proxy.RequestHandler;
import utils.ByteArrayUtil;
import utils.Log;
import utils.http.HttpUtil;
import utils.http.objs.HttpRequestInfo;
import utils.http.objs.HttpResponseInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerRequestHandler extends RequestHandler implements Runnable {
    public ServerRequestHandler(Socket clientSocket) {
        super(clientSocket);
        this.synchronizedTransceiver = new SynchronizedTransceiver(clientSocket);
    }

    private byte[] encryptDataForClient(byte[] data) {
        return Aes.encrypt(data, this.applicationKey.serverKey());
    }

    private byte[] decryptDataFromClient(byte[] data) {
        return Aes.decrypt(data, this.applicationKey.clientKey());
    }

    private void logReceivingResponse(String transmissionType, String url) {
        Log.info(String.format(
                        "Receiving response data and sending back, type: %s",
                        transmissionType),
                url);
    }

    @Override
    public void run() {
        Log.info("Negotiating application key");

        HandshakeController handshakeController = new ServerHandshakeController(this.clientSocket);

        try {
            this.applicationKey = handshakeController.negotiateApplicationKey();
        } catch (IOException | TlsException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        if (this.applicationKey == null) {
            Log.error("Application key negotiation failed");
            this.closeClientSocket();
            return;
        }

        Log.info("Receiving encrypted request data");
        // Receive encrypted client request data
        byte[] clientData;
        try {
            clientData = this.synchronizedTransceiver.receiveData().data();
            clientData = this.decryptDataFromClient(clientData);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        // Replace host field in request header
        String newHost = ServerConfigManager.getProxyPass();

        String originalRequestString = Utf8.encode(clientData);

        var originalRequestInfo = new HttpRequestInfo(originalRequestString);

        var url = originalRequestInfo.getHost() + originalRequestInfo.getPath();

        clientData = HttpUtil.replaceRequestHost(
                newHost, clientData, originalRequestString);

        var parsedNewHost = HttpUtil.parseHost(newHost);

        this.connectToServer(parsedNewHost.name(), parsedNewHost.port());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host", url);
            this.closeClientSocket();
            return;
        }

        Log.info("Forwarding request data to local server", url);
        // Forward request data to server
        try {
            this.serverSocket.setSoTimeout(ServerConfigManager.getTimeout());
            this.serverSocket.getOutputStream().write(clientData);
            this.serverSocket.getOutputStream().flush();
        } catch (IOException e) {
            Log.error(e.getClass().getName() + e.getMessage(), url);
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        // Receive response data and send encrypted data to client
        List<byte[]> headerBytes = new ArrayList<>();
        byte[] responseData = new byte[128 * 1024];
        int responseDataLength;
        byte[] actualResponseData;

        try {
            while (!Utf8.encode(ByteArrayUtil.concat(headerBytes)).contains("\r\n\r\n")) {
                responseDataLength = this.serverSocket.getInputStream().read(responseData);
                actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                headerBytes.add(actualResponseData);
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        actualResponseData = ByteArrayUtil.concat(headerBytes);
        responseDataLength = actualResponseData.length;
        // Determine response data transmission type
        String actualResponseString = Utf8.encode(actualResponseData);

        HttpResponseInfo responseInfo = new HttpResponseInfo(actualResponseString);

        try {
            // For status code 304(Not Modified), send the sole header directly
            if (responseInfo.getStatus() == 304) {
                this.logReceivingResponse("304 Not Modified", url);

                this.synchronizedTransceiver.sendData(
                        this.encryptDataForClient(actualResponseData));
            }
            // For HTTP 1.0, read until connection closes
            else if (responseInfo.getHttpVersion().equals("1.0")) {
                this.logReceivingResponse("HTTP 1.0", url);

                while (responseDataLength != -1) {
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                    this.synchronizedTransceiver.sendData(
                            this.encryptDataForClient(actualResponseData));
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                }
            }
            // The following two branches are higher versions of HTTP using long connections
            // Response transmission type: Chunked
            // Receive data in loop until encounter "\r\n0\r\n"
            else if (responseInfo.getTransferEncoding() != null
                    && responseInfo.getTransferEncoding().equals("chunked")) {
                this.logReceivingResponse("Chunked", url);

                this.synchronizedTransceiver.sendData(
                        this.encryptDataForClient(actualResponseData));

                while (!Utf8.encode(actualResponseData).contains("\r\n0\r\n")) {
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                    this.synchronizedTransceiver.sendData(
                            this.encryptDataForClient(actualResponseData));
                }
            }
            // Response transmission type: With Content-Length
            // Receive data of size contentLength
            else if (responseInfo.getContentLength() != null) {
                this.logReceivingResponse("Content-Length", url);

                int bodyStartIndex = actualResponseString.indexOf("\r\n\r\n") + 4;

                if (bodyStartIndex == 3) {
                    throw new IOException("Response header terminator not found");
                }

                int receivedDataLength = responseDataLength - bodyStartIndex;

                this.synchronizedTransceiver.sendData(
                        this.encryptDataForClient(actualResponseData));

                while (receivedDataLength < responseInfo.getContentLength()) {
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);

                    receivedDataLength += responseDataLength;

                    this.synchronizedTransceiver.sendData(
                            this.encryptDataForClient(actualResponseData));
                }
            } else {
                throw new IOException("Response transmission type not supported");
            }

            // Send finishing signal
            this.synchronizedTransceiver.sendData(new byte[]{0});
        } catch (IOException e) {
            Log.error(e.getClass().getName() + e.getMessage(), url);
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.success("All response data sent back", url);

        this.closeBothSockets();
    }
}
