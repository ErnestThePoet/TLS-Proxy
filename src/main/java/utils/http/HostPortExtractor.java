package utils.http;

public class HostPortExtractor {
    public static HostPort extract(String host){
        // IP:Port
        if(host.matches("\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}:\\d{1,5}")){
            var hostSplit=host.split(":");
            return new HostPort(hostSplit[0],Integer.valueOf(hostSplit[1]));
        }
        // Http url
        else{
            return new HostPort(host,80);
        }
    }
}
