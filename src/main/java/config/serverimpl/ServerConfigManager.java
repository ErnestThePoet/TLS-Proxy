package config.serverimpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfigManager {
    private static ServerProxyConfig config;

    public static void load(String configFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String configJsonContent = Files.readString(
                Path.of(configFilePath), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent, ServerProxyConfig.class);

        String propertyMissingTemplate = "服务端配置文件缺少字段'%s'";

        if (config.getPort() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("port"));
        }

        if (config.getTimeout() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("timeout"));
        }

        if (config.getProxyPass() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("proxyPass"));
        }

        if (config.getCertPath() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("certPath"));
        }

        if (config.getPrivateKeyPath() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("privateKeyPath"));
        }
    }

    public static Integer getPort() {
        return config.getPort();
    }

    public static Integer getTimeout() {
        return config.getTimeout();
    }

    public static String getProxyPass() {
        return config.getProxyPass();
    }

    public static String getCertPath() {
        return config.getCertPath();
    }

    public static String getPrivateKeyPath() {
        return config.getPrivateKeyPath();
    }
}
