package config.server;

import config.TlsProxyConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerProxyConfig extends TlsProxyConfig {
    private List<ProxyPassItem> proxyPasses;
}
