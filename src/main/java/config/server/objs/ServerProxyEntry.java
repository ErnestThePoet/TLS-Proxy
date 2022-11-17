package config.server.objs;

import lombok.Data;

@Data
public class ServerProxyEntry {
    private Integer port;
    private Integer timeout;
    private String proxyPass;
}
