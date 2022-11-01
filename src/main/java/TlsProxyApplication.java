import config.client.ClientConfigManager;
import config.server.ServerConfigManager;
import proxy.client.ClientTlsProxy;
import proxy.server.ServerTlsProxy;
import utils.Log;

import java.io.IOException;

public class TlsProxyApplication {
    public static void main(String[] args) {
        if (args.length != 1||(!args[0].equals("CLIENT")&&!args[0].equals("SERVER"))) {
            Log.error("Usage: <CLIENT|SERVER>");
            return;
        }

        try {
            switch (args[0]) {
                case "CLIENT" -> {
                    ClientConfigManager.init();
                    new ClientTlsProxy().start(ClientConfigManager.getPort());
                }
                case "SERVER" -> {
                    ServerConfigManager.init();
                    new ServerTlsProxy().start(ServerConfigManager.getPort());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
