package crypto.encoding;

import java.nio.charset.StandardCharsets;

public class Utf8 {
    public static byte[] decode(String s){
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String encode(byte[] bytes){
        return new String(bytes,StandardCharsets.UTF_8);
    }
}
