package config.clientimpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientConfigManager {
    private static ClientProxyConfig config;

    public static void load(String configFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        String configJsonContent = Files.readString(
                Path.of(configFilePath), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent, ClientProxyConfig.class);

        String propertyMissingTemplate = "客户端配置文件缺少字段'%s'";

        if (config.getPort() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("port"));
        }

        if (config.getTimeout() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("timeout"));
        }

        if (config.getTargetHostPatterns() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("targetHostPatterns"));
        }

        if (config.getRootCertPath() == null) {
            throw new RuntimeException(propertyMissingTemplate.formatted("rootCertPath"));
        }
    }

    public static Integer getPort() {
        return config.getPort();
    }

    public static Integer getTimeout() {
        return config.getTimeout();
    }

    public static boolean isTargetHost(String host) {
        return config.getTargetHostPatterns().stream().anyMatch(host::matches);
    }

    public static String getTargetHostPatternsHtmlText(){
        StringBuilder builder=new StringBuilder();

        for(var i:config.getTargetHostPatterns()){
            builder.append(i).append("<br/>");
        }

        return builder.toString();
    }

    public static String getRootCertPath(){
        return config.getRootCertPath();
    }
}
