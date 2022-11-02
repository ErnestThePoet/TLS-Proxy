package crypto.kdf;

import at.favre.lib.crypto.HKDF;
import at.favre.lib.crypto.HkdfMacFactory;

public class HkdfSha384 {
    private static final HKDF hkdf=
            HKDF.from(new HkdfMacFactory.Default("HmacSha384",null));

    public static byte[] extract(byte[] salt, byte[] inputKeyingMaterial){
        return hkdf.extract(salt,inputKeyingMaterial);
    }

    public static byte[] expand(byte[] pseudoRandomKey, byte[] info, int outLengthBytes){
        return hkdf.expand(pseudoRandomKey,info,outLengthBytes);
    }
}
