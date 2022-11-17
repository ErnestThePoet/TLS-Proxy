package proxy.serverimpl;

import crypto.encoding.Utf8;
import crypto.encryption.Aes;
import handshake.HandshakeController;
import handshake.serverimpl.ServerHandshakeController;
import proxy.RequestHandler;
import utils.ByteArrayUtil;
import utils.Log;
import utils.http.HttpUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerRequestHandler extends RequestHandler implements Runnable {
    private String proxyPass;
    public ServerRequestHandler(Socket clientSocket,int timeout,String proxyPass) {
        super(clientSocket,timeout);
        this.proxyPass=proxyPass;
    }


    private void encryptAndSendToClient(byte[] data) throws IOException {
        this.clientSocket.getOutputStream().write(
                Aes.encrypt(data, this.applicationKey.serverKey()));
        this.clientSocket.getOutputStream().flush();
    }

    private byte[] decryptDataFromClient(byte[] data) {
        return Aes.decrypt(data, this.applicationKey.clientKey());
    }

    @Override
    public void run() {
        Log.info("Negotiating application key with client");

        HandshakeController handshakeController = new ServerHandshakeController(this.clientSocket);

        this.applicationKey = handshakeController.negotiateApplicationKey();

        if(this.applicationKey==null){
            Log.error("Application key negotiation failed");
            this.closeClientSocket();
            return;
        }

        Log.success("Successfully calculated application key with client");

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
        var replaceHostResult =
                HttpUtil.replaceRequestHeaderHost(this.proxyPass,clientData);
        if (replaceHostResult.originalHost() == null) {
            Log.error("Host not found in request header");
            this.closeClientSocket();
            return;
        }

        Log.info(String.format("Replaced request header Host [%s] with [%s]",
                replaceHostResult.originalHost(), this.proxyPass));

        var serverPort = HttpUtil.extractHostPort(this.proxyPass);

        this.connectToServer(serverPort.host(), serverPort.port());

        if (this.serverSocket == null) {
            Log.error("Cannot connect to host");
            this.closeClientSocket();
            return;
        }

        // Forward request data to server
        try {
            this.serverSocket.setSoTimeout(this.timeout);
            this.serverSocket.getOutputStream().write(replaceHostResult.newRequestData());
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
            int contentLength = HttpUtil.getContentLength(actualResponseData);

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

        Log.success("All response data transmitted to client");

        this.closeBothSockets();
    }
}
