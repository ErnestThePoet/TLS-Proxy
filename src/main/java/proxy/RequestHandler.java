package proxy;

import crypto.encoding.Utf8;
import crypto.encryption.DualAesKey;

import java.io.*;
import java.net.Socket;

public abstract class RequestHandler {
    protected Socket clientSocket;
    protected Socket serverSocket;

    protected DualAesKey applicationKey;

    protected RequestHandler(Socket clientSocket){
        this.clientSocket=clientSocket;
    }

    private void closeSocket(Socket socket){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void closeClientSocket(){
        this.closeSocket(this.clientSocket);
    }

    protected void closeServerSocket(){
        this.closeSocket(this.serverSocket);
    }

    protected void closeBothSockets(){
        this.closeClientSocket();
        this.closeServerSocket();
    }

    protected void connectToServer(String host,int port){
        // TODO use actual port for server side
        try {
            this.serverSocket= new Socket(host,17750);
        } catch (IOException e) {
            e.printStackTrace();
            this.serverSocket=null;
        }
    }
}
