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
                        "正在接收响应数据并加密发回客户端，响应数据传输类型: %s",
                        transmissionType),
                url);
    }

    @Override
    public void run() {
        Log.info("正在协商应用数据通信秘钥");

        HandshakeController handshakeController = new ServerHandshakeController(this.clientSocket);

        try {
            this.applicationKey = handshakeController.negotiateApplicationKey();
        } catch (IOException | TlsException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        if (this.applicationKey == null) {
            Log.error("应用数据通信秘钥协商失败");
            this.closeClientSocket();
            return;
        }

        Log.info("正在接收加密的请求数据");
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

        var url = newHost + originalRequestInfo.getPath();

        clientData = HttpUtil.replaceRequestHost(
                newHost, clientData, originalRequestString);

        var parsedNewHost = HttpUtil.parseHost(newHost);

        this.connectToServer(parsedNewHost.name(), parsedNewHost.port());

        if (this.serverSocket == null) {
            Log.error("无法连接到主机", url);
            this.closeClientSocket();
            return;
        }

        try {
            Log.info("正在转发请求数据到本地服务器", url);
            // Forward request data to server

            this.serverSocket.setSoTimeout(ServerConfigManager.getTimeout());
            this.serverSocket.getOutputStream().write(clientData);
            this.serverSocket.getOutputStream().flush();

            // Receive response data and send encrypted data to client
            List<byte[]> headerBytes = new ArrayList<>();
            byte[] responseData = new byte[128 * 1024];
            int responseDataLength;
            byte[] actualResponseData;

            while (!Utf8.encode(ByteArrayUtil.concat(headerBytes)).contains("\r\n\r\n")) {
                responseDataLength = this.serverSocket.getInputStream().read(responseData);
                actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                headerBytes.add(actualResponseData);
            }

            actualResponseData = ByteArrayUtil.concat(headerBytes);
            responseDataLength = actualResponseData.length;
            // Determine response data transmission type
            String actualResponseString = Utf8.encode(actualResponseData);

            HttpResponseInfo responseInfo = new HttpResponseInfo(actualResponseString);

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
            // Receive data in loop until encounter "0\r\n\r\n"
            // Note: do not use "\r\n0\r\n" as terminator. In practice sometimes we cannot
            // get a full "\r\n0\r\n" in one read.
            else if (responseInfo.getTransferEncoding() != null
                    && responseInfo.getTransferEncoding().equals("chunked")) {
                this.logReceivingResponse("Chunked", url);

                this.synchronizedTransceiver.sendData(
                        this.encryptDataForClient(actualResponseData));

                while (!Utf8.encode(actualResponseData).contains("0\r\n\r\n")) {
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
                throw new IOException("响应数据传输类型不被TLS Proxy支持");
            }

            // Send finishing signal
            this.synchronizedTransceiver.sendData(new byte[]{0});
        } catch (IOException e) {
            Log.error(e, url);
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.success("全部响应数据已被加密发回客户端", url);

        this.closeBothSockets();
    }
}
