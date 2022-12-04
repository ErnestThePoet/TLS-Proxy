import config.clientimpl.ClientConfigManager;
import config.serverimpl.ServerConfigManager;
import proxy.clientimpl.ClientTlsProxy;
import proxy.serverimpl.ServerTlsProxy;
import utils.Log;

import java.io.IOException;

public class TlsProxyApplication {
    private static final String HELP_PROMPT =
            "Usage: java -jar tlsproxy.jar <CLIENT|SERVER> [-c <config-file-path>]";

    private static final String DEFAULT_CLIENT_CONFIG_FILE_PATH =
            "./configs/configs_client.json";

    private static final String DEFAULT_SERVER_CONFIG_FILE_PATH =
            "./configs/configs_server.json";

    public static void main(String[] args) {
        if (!((args.length == 1 || args.length == 3)
                && (args[0].equals("CLIENT") || args[0].equals("SERVER")))) {
            Log.error(HELP_PROMPT);
            return;
        }

        if (args.length == 3 && !args[1].equals("-c")) {
            Log.error(HELP_PROMPT);
            return;
        }

        try {
            switch (args[0]) {
                case "CLIENT" -> {
                    ClientConfigManager.load(
                            args.length == 1
                                    ? DEFAULT_CLIENT_CONFIG_FILE_PATH
                                    : args[2]);
                    new ClientTlsProxy().start(ClientConfigManager.getPort());
                }
                case "SERVER" -> {
                    ServerConfigManager.load(
                            args.length == 1
                                    ? DEFAULT_SERVER_CONFIG_FILE_PATH
                                    : args[2]);
                    new ServerTlsProxy().start(ServerConfigManager.getPort());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
