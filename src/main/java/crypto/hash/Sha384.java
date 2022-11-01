package crypto.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha384 {
    public static byte[] hash(byte[] message){
        try {
            var messageDigest= MessageDigest.getInstance("SHA-384");
            return messageDigest.digest(message);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
