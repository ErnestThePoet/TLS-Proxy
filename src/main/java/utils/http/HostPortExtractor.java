package utils.http;

public class HostPortExtractor {
    public static HostPort extract(String host){
        // IP:Port or url:port or localhost:port
        if(host.matches("\\S+:\\d{1,5}")){
            var hostSplit=host.split(":");
            return new HostPort(hostSplit[0],Integer.valueOf(hostSplit[1]));
        }
        // Http url
        else{
            return new HostPort(host,80);
        }
    }
}
