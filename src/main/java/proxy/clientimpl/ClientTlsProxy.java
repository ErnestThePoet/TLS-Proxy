package proxy.clientimpl;

import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientTlsProxy {
    public void start(int port,int timeout) {
        try(ServerSocket proxyServerSocket=new ServerSocket(port)) {
            Log.info(String.format(
                    "Successfully started TLS Proxy in CLIENT mode, port %d, timeout is %dms",
                    port,
                    timeout));

            while(true){
                Socket clientSocket;

                try {
                    clientSocket=proxyServerSocket.accept();
                } catch (IOException e) {
                    Log.error(e.getMessage());
                    continue;
                }

                clientSocket.setSoTimeout(timeout);

                new Thread(new ClientRequestHandler(clientSocket,timeout)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
