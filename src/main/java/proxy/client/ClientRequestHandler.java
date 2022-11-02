package proxy.client;

import config.client.ClientConfigManager;
import proxy.HandshakeController;
import proxy.RequestHandler;
import utils.Log;
import utils.http.HostPortExtractor;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class ClientRequestHandler extends RequestHandler implements Runnable {
    private BufferedReader clientSocketReader;


    public ClientRequestHandler(Socket clientSocket) {
        super(clientSocket);

        try {
            this.clientSocketReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getRequestHost() {
        while (true) {
            String currentLine;

            try {
                currentLine = this.clientSocketReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                this.closeSocket(this.clientSocket);
                return null;
            }

            if (currentLine == null) {
                return null;
            }

            if (currentLine.startsWith("Host:")) {
                return currentLine
                        .replace("Host:", "")
                        .replace(" ", "");
            }
        }
    }

    @Override
    public void run() {
        String host = this.getRequestHost();

        if (host == null) {
            Log.error("Cannot get host from request header");
            this.closeSocket(this.clientSocket);
            return;
        }

        if (!ClientConfigManager.isTargetHost(host)) {
            Log.info("Ignore request to " + host);
            return;
        }

        var hostPort = HostPortExtractor.extract(host);

        Socket hostSocket = this.connectToHost(hostPort.getHost(), hostPort.getPort());

        if (hostSocket == null) {
            Log.error("Cannot connect to host");
            this.closeSocket(this.clientSocket);
            return;
        }

        Log.info("Negotiating application key with " + host);

        HandshakeController handshakeController =
                new ClientHandshakeController(hostSocket);

        var applicationKey = handshakeController.negotiateApplicationKey();

        // Forward encrypted client data
        byte[] clientData = new byte[1024 * 1024];
        int clientDataLength;

        System.out.println(Base64.getEncoder().encodeToString(applicationKey.getClientKey().getKey()));
        System.out.println(Base64.getEncoder().encodeToString(applicationKey.getClientKey().getIv()));
        System.out.println(Base64.getEncoder().encodeToString(applicationKey.getServerKey().getKey()));
        System.out.println(Base64.getEncoder().encodeToString(applicationKey.getServerKey().getIv()));

        this.closeSocket(hostSocket);

        try {
            while (this.clientSocket.isConnected()) {
                clientDataLength=this.clientSocket.getInputStream().read(clientData);
                System.out.println(clientDataLength);
                if(clientDataLength==-1){
                    break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            this.closeSocket(this.clientSocket);
            return;
        }

        System.out.println("EXIT");
        this.closeSocket(this.clientSocket);
        this.closeSocket(hostSocket);
    }
}
