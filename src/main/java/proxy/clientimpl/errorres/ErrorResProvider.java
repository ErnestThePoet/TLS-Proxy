package proxy.clientimpl.errorres;

import crypto.encoding.Utf8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ErrorResProvider {
    private static final String HTTP_RESPONSE_HEADER="HTTP/1.1 200 OK\r\n\r\n";
    public static byte[] getErrorPageResponse(String message){
        try{
            String htmlTemplate= Files.readString(Path.of("./res/error.html"));
            return Utf8.decode(HTTP_RESPONSE_HEADER
                    +htmlTemplate.formatted(message));
        } catch (IOException e) {
            return Utf8.decode(HTTP_RESPONSE_HEADER
                    +"Could not read error template HTML. The error message is:\n"
                    +message);
        }
    }
}
