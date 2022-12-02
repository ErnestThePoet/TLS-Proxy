package utils.http.objs;

import lombok.Data;

@Data
public class HttpResponseInfo {
    private String httpVersion;
    private Integer status;
    private Integer contentLength;

    private String transferEncoding;

    public HttpResponseInfo(String header) {
        var lines = header.split("\r\n");
        var firstLineParts = lines[0].split(" ");
        this.httpVersion = firstLineParts[0].replace("HTTP/", "");
        this.status = Integer.valueOf(firstLineParts[1]);

        for (var line : lines) {
            if (line.startsWith("Content-Length:")) {
                this.contentLength = Integer.parseInt(line
                        .replace("Content-Length:", "")
                        .replace(" ", ""));

                if (this.transferEncoding != null) {
                    break;
                }
            }

            if (line.startsWith("Transfer-Encoding:")) {
                this.transferEncoding = line.replace("Transfer-Encoding:", "")
                        .replace(" ", "");

                if (this.contentLength != null) {
                    break;
                }
            }
        }
    }
}
