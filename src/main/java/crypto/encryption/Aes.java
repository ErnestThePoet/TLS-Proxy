package crypto.encryption;

import crypto.encryption.objs.AesKey;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes {
    private static byte[] aesOperate(int opMode, byte[] data, byte[] key, byte[] iv) {
        try {
            Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            aesCipher.init(opMode, secretKeySpec, ivParameterSpec);

            return aesCipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] cipher, AesKey key) {
        return aesOperate(
                Cipher.DECRYPT_MODE,
                cipher,
                key.key(),
                key.iv()
        );
    }

    public static byte[] encrypt(byte[] plainText, AesKey key) {
        return aesOperate(
                Cipher.ENCRYPT_MODE,
                plainText,
                key.key(),
                key.iv()
        );
    }
}
