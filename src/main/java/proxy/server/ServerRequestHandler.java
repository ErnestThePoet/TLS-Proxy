package proxy.server;

import config.server.ServerConfigManager;
import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import proxy.HandshakeController;
import proxy.RequestHandler;
import utils.ByteArrayUtil;
import utils.Log;
import utils.http.HostPortExtractor;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerRequestHandler extends RequestHandler implements Runnable {
    public ServerRequestHandler(Socket clientSocket) {
        super(clientSocket);
    }

    private record ReplaceHostResult(byte[] newRequestData, String newHost) {
    }

    private ReplaceHostResult replaceRequestHeaderHost(byte[] requestData) {
        String requestString = Utf8.encode(requestData);
        int hostHIndex = requestString.indexOf("Host:");
        if (hostHIndex == -1) {
            Log.error("Host not found in request header");
            return new ReplaceHostResult(requestData, null);
        }

        int hostCrIndex = requestString.indexOf('\r', hostHIndex);
        String originalHost = requestString.substring(hostHIndex + 5, hostCrIndex)
                .replace(" ", "");
        String proxyPass = ServerConfigManager.getProxyPass(originalHost);

        if (proxyPass == null) {
            Log.error("No matching proxy pass found for:"+originalHost);
            return new ReplaceHostResult(requestData, null);
        }

        byte[] newRequestData = new byte[hostHIndex + proxyPass.length() + 6 + requestData.length - hostCrIndex];
        System.arraycopy(requestData, 0, newRequestData, 0, hostHIndex);
        System.arraycopy(Utf8.decode("Host: " + proxyPass), 0,
                newRequestData, hostHIndex, proxyPass.length() + 6);
        System.arraycopy(requestData, hostCrIndex,
                newRequestData, hostHIndex + proxyPass.length() + 6, requestData.length - hostCrIndex);

        Log.info(String.format("Replaced request header Host [%s] with [%s]", originalHost, proxyPass));

        return new ReplaceHostResult(newRequestData, proxyPass);
    }

    private int getContentLength(byte[] responseData) {
        var allLines = Utf8.encode(responseData).split("\r\n");

        for (var i : allLines) {
            if (i.startsWith("Content-Length:")) {
                return Integer.parseInt(i
                        .replace("Content-Length:", "")
                        .replace(" ", ""));
            }
        }

        return -1;
    }

    private void encryptAndSendToClient(byte[] data) throws IOException {
        this.clientSocket.getOutputStream().write(
                Aes.encrypt(data, this.applicationKey.getServerKey()));
        this.clientSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromClient(byte[] data) {
        return Aes.decrypt(data, this.applicationKey.getClientKey());
    }

    @Override
    public void run() {
        Log.info("Negotiating application key with client");

        HandshakeController handshakeController = new ServerHandshakeController(this.clientSocket);

        this.applicationKey = handshakeController.negotiateApplicationKey();

        Log.info("Successfully calculated application key with client");

        // Receive encrypted client request data
        byte[] clientData = new byte[8 * 1024 * 1024];
        try {
            int clientDataLength = this.clientSocket.getInputStream().read(clientData);
            clientData = Arrays.copyOf(clientData, clientDataLength);
            clientData = this.decryptDataFromClient(clientData);
        } catch (IOException e) {
            e.printStackTrace();
            this.closeClientSocket();
            return;
        }

        // Replace host field in request header
        var replaceHostResult = this.replaceRequestHeaderHost(clientData);
        if (replaceHostResult.newHost == null) {
            this.closeClientSocket();
            return;
        }

        var serverPort = HostPortExtractor.extract(replaceHostResult.newHost);

        this.connectToServer(serverPort.getHost(), serverPort.getPort());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host");
            this.closeClientSocket();
            return;
        }

        // Forward request data to server
        try {
            this.serverSocket.getOutputStream().write(replaceHostResult.newRequestData);
            this.serverSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("Sent request data to local server");

        // Receive response data and send encrypted data to client
        List<byte[]> headerBytes=new ArrayList<>();
        byte[] responseData = new byte[64 * 1024];
        int responseDataLength;
        byte[] actualResponseData;

        try {
            while(!Utf8.encode(ByteArrayUtil.concat(headerBytes)).contains("\r\n\r\n")){
                responseDataLength = this.serverSocket.getInputStream().read(responseData);
                actualResponseData=Arrays.copyOf(responseData, responseDataLength);
                headerBytes.add(actualResponseData);
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        actualResponseData=ByteArrayUtil.concat(headerBytes);
        responseDataLength = actualResponseData.length;
        // Determine response data transmission type
        String actualResponseString = Utf8.encode(actualResponseData);

        try {
            int contentLength = this.getContentLength(actualResponseData);

            // Response transmission type: Chunked
            // Receive data in loop until encounter "\r\n\0\r\n"
            if (contentLength == -1) {
                Log.info("Response transmission type: Chunked");

                this.encryptAndSendToClient(actualResponseData);

                int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                if (syncLength != 1) {
                    throw new IOException("Client sync data not of length 1");
                }

                while (!Utf8.encode(actualResponseData).contains("\r\n\0\r\n")) {
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                    this.encryptAndSendToClient(actualResponseData);

                    syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                    if (syncLength != 1) {
                        throw new IOException("Client sync data not of length 1");
                    }
                }
            }
            // Response transmission type: With Content-Length
            // Receive data of size contentLength
            else {
                Log.info("Response transmission type: With Content-Length");

                int bodyStartIndex = actualResponseString.indexOf("\r\n\r\n") + 4;

                if(bodyStartIndex==3){
                    throw new IOException("Response header terminator not found");
                }

                int receivedDataLength = responseDataLength - bodyStartIndex;

                this.encryptAndSendToClient(actualResponseData);

                int syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                if (syncLength != 1) {
                    throw new IOException("Client sync data not of length 1");
                }

                while (receivedDataLength < contentLength) {
                    responseDataLength = this.serverSocket.getInputStream().read(responseData);
                    actualResponseData = Arrays.copyOf(responseData, responseDataLength);
                    this.encryptAndSendToClient(actualResponseData);
                    receivedDataLength += responseDataLength;

                    syncLength=this.clientSocket.getInputStream().read(new byte[2]);
                    if (syncLength != 1) {
                        throw new IOException("Client sync data not of length 1");
                    }
                }
            }

            // Send finishing signal
            this.clientSocket.getOutputStream().write(new byte[1]);
            this.clientSocket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            this.closeBothSockets();
            return;
        }

        Log.info("All data transmitted to client");

        this.closeBothSockets();
    }
}
