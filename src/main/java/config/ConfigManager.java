package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigManager {
    private static TlsProxyConfig config;

    public static void init() throws IOException {
        ObjectMapper objectMapper=new ObjectMapper();

        String configJsonContent= Files.readString(
                Path.of("./configs/configs.json"), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent,TlsProxyConfig.class);
    }

    public static Integer getPort(){
        return config.getPort();
    }

    public static String getMode(){
        return config.getMode();
    }

    public static boolean isTargetHost(String host){
        return config.getTargetHostPatterns().stream().anyMatch(host::matches);
    }

    public static String getGetCertificateUrl(){
        return config.getGetCertificateUrl();
    }
}
