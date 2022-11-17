package config.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientConfigManager {
    private static ClientProxyConfig config;

    public static void load() throws IOException {
        ObjectMapper objectMapper=new ObjectMapper();

        String configJsonContent= Files.readString(
                Path.of("./configs/configs_client.json"), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent, ClientProxyConfig.class);

        String propertyMissingTemplate="property '%s' missing from client config file";

        if(config.getPort()==null){
            throw new RuntimeException(propertyMissingTemplate.formatted("port"));
        }

        if(config.getTimeout()==null){
            throw new RuntimeException(propertyMissingTemplate.formatted("timeout"));
        }

        if(config.getTargetHostPatterns()==null){
            throw new RuntimeException(propertyMissingTemplate.formatted("targetHostPatterns"));
        }
    }

    public static Integer getPort(){
        return config.getPort();
    }

    public static Integer getTimeout(){
        return config.getTimeout();
    }

    public static boolean isTargetHost(String host){
        return config.getTargetHostPatterns().stream().anyMatch(host::matches);
    }
}
