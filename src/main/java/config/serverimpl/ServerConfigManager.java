package config.serverimpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfigManager {
    private static ServerProxyConfig config;

    public static void load() throws IOException {
        ObjectMapper objectMapper=new ObjectMapper();

        String configJsonContent= Files.readString(
                Path.of("./configs/configs_server.json"), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent, ServerProxyConfig.class);
    }

    public static Integer getPort(){
        return config.getPort();
    }

    public static Integer getTimeout(){
        return config.getTimeout();
    }

    public static String getProxyPass(){
        return config.getProxyPass();
    }
}
