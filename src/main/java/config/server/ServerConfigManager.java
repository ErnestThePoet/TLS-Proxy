package config.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.server.objs.ServerProxyEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ServerConfigManager {
    private static ServerProxyConfig config;

    public static void load() throws IOException {
        ObjectMapper objectMapper=new ObjectMapper();

        String configJsonContent= Files.readString(
                Path.of("./configs/configs_server.json"), StandardCharsets.UTF_8);

        config = objectMapper.readValue(configJsonContent, ServerProxyConfig.class);

        String propertyMissingTemplate="property '%s' missing from server config file";
        String proxiesPropertyMissingTemplate=
                "property '%s' missing from proxies[%d] in server config file";

        if(config.getProxies()==null){
            throw new RuntimeException(propertyMissingTemplate.formatted("proxies"));
        }

        for(var i:config.getProxies()){
            if(i.getPort()==null){
                throw new RuntimeException(proxiesPropertyMissingTemplate.formatted("port"));
            }

            if(i.getTimeout()==null){
                throw new RuntimeException(proxiesPropertyMissingTemplate.formatted("timeout"));
            }

            if(i.getProxyPass()==null){
                throw new RuntimeException(proxiesPropertyMissingTemplate.formatted("proxyPass"));
            }
        }
    }

    public static List<ServerProxyEntry> getProxies(){
        return config.getProxies();
    }
}
