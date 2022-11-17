package config.client;

import lombok.Data;

import java.util.List;

@Data
public class ClientProxyConfig {
    private Integer port;
    private Integer timeout;
    private List<String> targetHostPatterns;
}
