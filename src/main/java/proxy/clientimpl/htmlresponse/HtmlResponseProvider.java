package proxy.clientimpl.htmlresponse;

import config.clientimpl.ClientConfigManager;
import crypto.encoding.Utf8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HtmlResponseProvider {
    private static final String HTTP_RESPONSE_HEADER = "HTTP/1.1 200 OK\r\n\r\n";

    public static byte[] getErrorPageResponse(String message) {
        try {
            String htmlTemplate = Files.readString(Path.of("./res/error.html"));
            return Utf8.decode(HTTP_RESPONSE_HEADER
                    + htmlTemplate.replace("{DETAIL}", message));
        } catch (IOException e) {
            return Utf8.decode(HTTP_RESPONSE_HEADER
                    + "Could not read error template HTML.\n"
                    + "An error happened in client TLS Proxy while communicating with server.\n"
                    + "Details of the error are as follows:\n"
                    + message);
        }
    }

    public static byte[] getNotTargetHostPageResponse(String host) {
        try {
            String htmlTemplate = Files.readString(Path.of("./res/not_target_host.html"));
            return Utf8.decode(HTTP_RESPONSE_HEADER
                    + htmlTemplate.replace("{HOST}", host)
                    .replace("{TARGET_HOST_PATTERNS}",
                            ClientConfigManager.getTargetHostPatternsHtmlText()));
        } catch (IOException e) {
            return Utf8.decode(HTTP_RESPONSE_HEADER
                    + ("Could not read template HTML.\n"
                    + "The host you are accessing(%s) "
                    + "is not a proxy target of TLS Proxy.").formatted(host));
        }
    }
}
