package config.client;

import config.TlsProxyConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClientProxyConfig extends TlsProxyConfig {
    private List<String> targetHostPatterns;
}
