package proxy.clientimpl;

import config.clientimpl.ClientConfigManager;
import proxy.TlsProxy;
import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientTlsProxy implements TlsProxy {
    @Override
    public void start(int port) {
        try(ServerSocket proxyServerSocket=new ServerSocket(port)) {
            Log.info(String.format(
                    "Successfully started TLS Proxy in CLIENT mode, port %d, timeout is %dms",
                    ClientConfigManager.getPort(),
                    ClientConfigManager.getTimeout()));

            while(true){
                Socket clientSocket;

                try {
                    clientSocket=proxyServerSocket.accept();
                } catch (IOException e) {
                    Log.error(e.getMessage());
                    continue;
                }

                clientSocket.setSoTimeout(ClientConfigManager.getTimeout());

                new Thread(new ClientRequestHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
