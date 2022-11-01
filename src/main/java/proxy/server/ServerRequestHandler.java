package proxy.server;

import proxy.HandshakeController;
import proxy.RequestHandler;

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
