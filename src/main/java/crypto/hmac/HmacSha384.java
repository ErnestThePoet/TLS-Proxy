package crypto.hmac;

import utils.ByteArrayUtil;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class HmacSha384 {
    public static byte[] mac(byte[] key,byte[] message){
        SecretKey secretKey=new SecretKeySpec(key,"HmacSha384");

        try {
            Mac mac=Mac.getInstance("HmacSha384");
            mac.init(secretKey);
            return mac.doFinal(message);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verify(byte[] key,byte[] message,byte[] tag){
        return ByteArrayUtil.equals(tag,mac(key,message));
    }
}
