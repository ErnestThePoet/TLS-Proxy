package utils.http;

import crypto.encoding.Utf8;
import utils.ByteArrayUtil;
import utils.http.objs.ParsedHost;
import utils.http.objs.HttpRequestInfo;

import java.util.Arrays;

public class HttpUtil {
    public static ParsedHost parseHost(String host) {
        // IP:Port or url:port or localhost:port
        if (host.matches("\\S+:\\d{1,5}")) {
            var hostSplit = host.split(":");
            return new ParsedHost(hostSplit[0], Integer.valueOf(hostSplit[1]));
        }
        // Http url
        else {
            return new ParsedHost(host, 80);
        }
    }

    public static String shortenPath(String host, String path) {
        return path
                .replace("http://", "")
                .replace(host, "");
    }

    public static byte[] replaceRequestPath(String newPath, byte[] requestData) {
        var firstCrIndex = ByteArrayUtil.indexOf(requestData, (byte) '\r');

        var firstLine = Utf8.encode(Arrays.copyOf(requestData, firstCrIndex));

        var firstLineSplit = firstLine.split(" ");

        byte[] newRequestData = new byte[firstLineSplit[0].length()
                + newPath.length() + 2
                + firstLineSplit[2].length()
                + requestData.length - firstLine.length()];

        System.arraycopy(Utf8.decode(firstLineSplit[0]), 0,
                newRequestData, 0, firstLineSplit[0].length());

        System.arraycopy(Utf8.decode(" " + newPath + " "), 0,
                newRequestData, firstLineSplit[0].length(), newPath.length() + 2);

        System.arraycopy(Utf8.decode(firstLineSplit[2]), 0,
                newRequestData, firstLineSplit[0].length() + newPath.length() + 2,
                firstLineSplit[2].length());

        System.arraycopy(requestData, firstLine.length(),
                newRequestData,
                firstLineSplit[0].length() + newPath.length() + 2 + firstLineSplit[2].length(),
                requestData.length - firstLine.length());

        return newRequestData;
    }

    // Such awkward parameters are meant to reuse Utf8.decode() results
    public static byte[] replaceRequestHost(
            String newHost, byte[] requestData, String requestString) {
        int hostHIndex = requestString.indexOf("Host:");

        int hostCrIndex = requestString.indexOf('\r', hostHIndex);

        byte[] newRequestData = new byte[
                hostHIndex + newHost.length() + 6 + requestData.length - hostCrIndex];
        System.arraycopy(requestData, 0, newRequestData, 0, hostHIndex);
        System.arraycopy(Utf8.decode("Host: " + newHost), 0,
                newRequestData, hostHIndex, newHost.length() + 6);
        System.arraycopy(
                requestData,
                hostCrIndex,
                newRequestData,
                hostHIndex + newHost.length() + 6,
                requestData.length - hostCrIndex);

        return newRequestData;
    }
}
