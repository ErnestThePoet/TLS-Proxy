package config.server;

import config.server.objs.ServerProxyEntry;
import lombok.Data;

import java.util.List;

@Data
public class ServerProxyConfig {
    private List<ServerProxyEntry> proxies;
}
