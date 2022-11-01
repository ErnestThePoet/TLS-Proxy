package proxy;

import java.io.*;
import java.net.Socket;

public abstract class RequestHandler {
    protected Socket clientSocket;
    protected BufferedReader clientSocketReader;
    protected BufferedWriter clientSocketWriter;

    protected RequestHandler(Socket clientSocket){
        this.clientSocket=clientSocket;
        try {
            this.clientSocketReader=new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            this.clientSocketWriter=new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    protected void closeSocket(Socket socket){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Socket connectToHost(String host,int port){
        try(Socket hostSocket=new Socket(host,port)) {
            return hostSocket;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
