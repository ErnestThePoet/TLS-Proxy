package proxy.client;

import config.ConfigManager;
import proxy.HandshakeController;
import proxy.RequestHandler;
import utils.Log;
import utils.http.HostPortExtractor;

import java.io.IOException;
import java.net.Socket;

public class ClientRequestHandler extends RequestHandler implements Runnable{
    public ClientRequestHandler(Socket clientSocket){
        super(clientSocket);
    }

    private String getRequestHost(){
        while(true){
            String currentLine;

            try {
                currentLine=this.clientSocketReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                this.closeSocket(this.clientSocket);
                return null;
            }

            if(currentLine==null){
                return null;
            }

            if(currentLine.startsWith("Host:")){
                return currentLine
                        .replace("Host:","")
                        .replace(" ","");
            }
        }
    }

    @Override
    public void run() {
        String host=this.getRequestHost();

        if(host==null){
            Log.error("Cannot get host from request header");
            this.closeSocket(this.clientSocket);
            return;
        }

        if(!ConfigManager.isTargetHost(host)){
            return;
        }

        var hostPort= HostPortExtractor.extract(host);

        Socket hostSocket=this.connectToHost(hostPort.getHost(),hostPort.getPort());

        if(hostSocket==null){
            Log.error("Cannot connect to host");
            this.closeSocket(this.clientSocket);
            return;
        }

        HandshakeController handshakeController=
                new ClientHandshakeController(hostSocket);

        var applicationKey=handshakeController.negotiateApplicationKey();

        this.closeSocket(this.clientSocket);
        this.closeSocket(hostSocket);
    }
}
