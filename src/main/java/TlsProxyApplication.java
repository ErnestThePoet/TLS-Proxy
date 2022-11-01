import config.ConfigManager;
import proxy.client.ClientTlsProxy;
import proxy.server.ServerTlsProxy;
import proxy.TlsProxy;
import utils.Log;

import java.io.IOException;

public class TlsProxyApplication {
    public static void main(String[] args){
        try{
            ConfigManager.init();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        TlsProxy tlsProxy;
        switch (ConfigManager.getMode()) {
            case "CLIENT" -> tlsProxy = new ClientTlsProxy();
            case "SERVER" -> tlsProxy = new ServerTlsProxy();
            default -> {
                Log.error("Config file 'mode' property invalid");
                return;
            }
        }

        tlsProxy.start(ConfigManager.getPort());
    }
}
