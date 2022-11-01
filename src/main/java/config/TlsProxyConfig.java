package config;

import lombok.Data;

@Data
public abstract class TlsProxyConfig {
    private Integer port;
}
