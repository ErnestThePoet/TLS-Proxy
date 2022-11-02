package crypto.sign;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class Rsa {
    public static byte[] sign(byte[] privateKey, byte[] message) {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PrivateKey key = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance("SHA384withRSA");

            signature.initSign(key);
            signature.update(message);

            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verify(byte[] publicKey, byte[] message, byte[] sign) {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(publicKey);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PublicKey key = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA384withRSA");

            signature.initVerify(key);
            signature.update(message);

            return signature.verify(sign);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
