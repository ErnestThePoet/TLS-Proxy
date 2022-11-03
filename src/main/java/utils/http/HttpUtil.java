package utils.http;

import crypto.encoding.Utf8;
import utils.http.objs.HostAndShortenPathResult;
import utils.http.objs.HostPort;
import utils.http.objs.ReplaceHostResult;

public class HttpUtil {
    public static HostPort extractHostPort(String host){
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

    public static HostAndShortenPathResult getRequestHeaderHostAndShortenPath(byte[] requestData) {
        var allLines = Utf8.encode(requestData).split("\r\n");

        String host = null;

        for (var i : allLines) {
            if (i.startsWith("Host:")) {
                host = i
                        .replace("Host:", "")
                        .replace(" ", "");
                break;
            }
        }

        if (host == null) {
            return new HostAndShortenPathResult(null, null, null);
        }

        var firstLineSplit = allLines[0].split(" ");
        if (firstLineSplit.length != 3) {
            return new HostAndShortenPathResult(host, null, null);
        }

        var newPath = firstLineSplit[1]
                .replace("http://", "")
                .replace(host, "");

        byte[] newRequestData = new byte[firstLineSplit[0].length()
                + newPath.length() + 2
                + firstLineSplit[2].length()
                + requestData.length - allLines[0].length()];

        System.arraycopy(Utf8.decode(firstLineSplit[0]), 0,
                newRequestData, 0, firstLineSplit[0].length());

        System.arraycopy(Utf8.decode(" " + newPath + " "), 0,
                newRequestData, firstLineSplit[0].length(), newPath.length() + 2);

        System.arraycopy(Utf8.decode(firstLineSplit[2]), 0,
                newRequestData, firstLineSplit[0].length() + newPath.length() + 2,
                firstLineSplit[2].length());

        System.arraycopy(requestData, allLines[0].length(),
                newRequestData,
                firstLineSplit[0].length() + newPath.length() + 2 + firstLineSplit[2].length(),
                requestData.length - allLines[0].length());

        return new HostAndShortenPathResult(host, newPath, newRequestData);
    }

    public static ReplaceHostResult replaceRequestHeaderHost(String newHost, byte[] requestData) {
        String requestString = Utf8.encode(requestData);
        int hostHIndex = requestString.indexOf("Host:");
        if (hostHIndex == -1) {
            return new ReplaceHostResult(null, requestData);
        }

        int hostCrIndex = requestString.indexOf('\r', hostHIndex);
        String originalHost = requestString.substring(hostHIndex + 5, hostCrIndex)
                .replace(" ", "");

        byte[] newRequestData = new byte[hostHIndex + newHost.length() + 6 + requestData.length - hostCrIndex];
        System.arraycopy(requestData, 0, newRequestData, 0, hostHIndex);
        System.arraycopy(Utf8.decode("Host: " + newHost), 0,
                newRequestData, hostHIndex, newHost.length() + 6);
        System.arraycopy(requestData, hostCrIndex,
                newRequestData, hostHIndex + newHost.length() + 6, requestData.length - hostCrIndex);

        return new ReplaceHostResult(originalHost, newRequestData);
    }

    public static int getContentLength(byte[] responseData) {
        var allLines = Utf8.encode(responseData).split("\r\n");

        for (var i : allLines) {
            if (i.startsWith("Content-Length:")) {
                return Integer.parseInt(i
                        .replace("Content-Length:", "")
                        .replace(" ", ""));
            }
        }

        return -1;
    }
}
