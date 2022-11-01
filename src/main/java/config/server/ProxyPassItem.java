package config.server;

import lombok.Data;

@Data
public class ProxyPassItem {
    private String location;
    private String pass;
}
