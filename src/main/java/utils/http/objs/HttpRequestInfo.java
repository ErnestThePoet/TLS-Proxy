package utils.http.objs;

import lombok.Data;
import utils.http.HttpUtil;

@Data
public class HttpRequestInfo {
    private String path;
    private String host;
    private String hostName;
    private Integer hostPort;

    public HttpRequestInfo(String header) {
        var lines = header.split("\r\n");

        this.path = lines[0].split(" ")[1];

        for (var line : lines) {
            if (line.startsWith("Host:")) {
                this.host = line.replace("Host:", "")
                        .replace(" ", "");
                break;
            }
        }

        var parsedHost= HttpUtil.parseHost(this.host);

        this.hostName=parsedHost.name();
        this.hostPort=parsedHost.port();
    }
}
