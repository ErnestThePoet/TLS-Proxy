package proxy.server;

import config.ConfigManager;
import proxy.HandshakeController;
import proxy.RequestHandler;
import proxy.client.ClientHandshakeController;
import utils.Log;
import utils.http.HostPortExtractor;

import java.net.Socket;

public class ServerRequestHandler extends RequestHandler implements Runnable{
    public ServerRequestHandler(Socket clientSocket){
        super(clientSocket);
    }

    @Override
    public void run() {
        HandshakeController handshakeController=new ServerHandshakeController(this.clientSocket);

        var applicationKey=handshakeController.negotiateApplicationKey();

        this.closeSocket(this.clientSocket);
    }
}
