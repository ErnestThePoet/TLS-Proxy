package config.serverimpl;

import config.TlsProxyConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServerProxyConfig extends TlsProxyConfig {
    private String proxyPass;
}
