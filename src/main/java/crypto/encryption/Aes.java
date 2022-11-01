package crypto.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Aes {
    private static byte[] aesOperate(int opMode,byte[] data,byte[] key,byte[] iv){
        Cipher aesCipher;

        try {
            aesCipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        }

        SecretKeySpec secretKeySpec=new SecretKeySpec(key,"AES");
        IvParameterSpec ivParameterSpec=new IvParameterSpec(iv);

        try {
            aesCipher.init(opMode,secretKeySpec,ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return null;
        }

        try {
            return aesCipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] cipher,AesKey key){
        return aesOperate(
                Cipher.DECRYPT_MODE,
                cipher,
                key.getKey(),
                key.getIv()
        );
    }

    public static byte[] encrypt(byte[] plainText,AesKey key){
        return aesOperate(
                Cipher.ENCRYPT_MODE,
                plainText,
                key.getKey(),
                key.getIv()
        );
    }
}
