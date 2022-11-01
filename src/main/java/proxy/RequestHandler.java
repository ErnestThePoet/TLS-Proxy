package proxy;

import java.io.*;
import java.net.Socket;

public abstract class RequestHandler {
    protected Socket clientSocket;

    protected RequestHandler(Socket clientSocket){
        this.clientSocket=clientSocket;
    }
    protected void closeSocket(Socket socket){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Socket connectToHost(String host,int port){
        try {
            return new Socket(host,17750);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
