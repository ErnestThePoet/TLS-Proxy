package config;

import lombok.Data;

import java.util.List;

@Data
public class TlsProxyConfig {
    private String mode;
    private Integer port;
    private List<String> targetHostPatterns;
    private String getCertificateUrl;
}
