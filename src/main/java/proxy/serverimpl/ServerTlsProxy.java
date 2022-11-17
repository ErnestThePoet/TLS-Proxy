package proxy.serverimpl;

import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTlsProxy {
    public void start(int port,int timeout,String proxyPass) {
        try(ServerSocket proxyServerSocket=new ServerSocket(port)) {
            Log.info(String.format(
                    "Successfully started TLS Proxy in SERVER mode, port %d, timeout is %dms",
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

                new Thread(new ServerRequestHandler(clientSocket,timeout,proxyPass)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
