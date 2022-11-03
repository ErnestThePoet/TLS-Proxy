import config.clientimpl.ClientConfigManager;
import config.serverimpl.ServerConfigManager;
import proxy.clientimpl.ClientTlsProxy;
import proxy.serverimpl.ServerTlsProxy;
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
                    ClientConfigManager.load();
                    new ClientTlsProxy().start(ClientConfigManager.getPort());
                }
                case "SERVER" -> {
                    ServerConfigManager.load();
                    new ServerTlsProxy().start(ServerConfigManager.getPort());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
