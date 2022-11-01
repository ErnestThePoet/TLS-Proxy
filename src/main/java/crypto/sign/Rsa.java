package crypto.sign;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Rsa {
    public static byte[] sign(byte[] privateKey,byte[] message){
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);

        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        PrivateKey key;
        try {
            key = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }

        Signature signature;
        try {
            signature = Signature.getInstance("SHA384withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        try {
            signature.initSign(key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }

        try {
            signature.update(message);
            return signature.sign();
        } catch (SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verify(byte[] publicKey,byte[] message,byte[] sign){
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(publicKey);

        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        PublicKey key;
        try {
            key = keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        }

        Signature signature;
        try {
            signature = Signature.getInstance("SHA384withRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }

        try {
            signature.initVerify(key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

        try {
            signature.update(message);
            return signature.verify(sign);
        } catch (SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }
}
