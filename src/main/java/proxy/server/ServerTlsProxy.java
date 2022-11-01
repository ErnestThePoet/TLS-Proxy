package proxy.server;

import config.ConfigManager;
import proxy.TlsProxy;
import proxy.client.ClientRequestHandler;
import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ServerTlsProxy implements TlsProxy {
    @Override
    public void start(int port) {
        try(ServerSocket proxyServerSocket=new ServerSocket(port)) {
            Log.info(String.format(
                    "Successfully started TLS Proxy in %s mode, port %d",
                    ConfigManager.getMode(),
                    ConfigManager.getPort()));

            while(true){
                Socket clientSocket;

                try {
                    clientSocket=proxyServerSocket.accept();
                } catch (IOException e) {
                    Log.error(e.getMessage());
                    continue;
                }

                new Thread(new ClientRequestHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
