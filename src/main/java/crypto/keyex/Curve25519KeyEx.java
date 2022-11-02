package crypto.keyex;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

public class Curve25519KeyEx {
    private static final Curve25519 curve25519=Curve25519.getInstance(Curve25519.BEST);

    public static Curve25519KeyPair generateKeyPair(){
        return curve25519.generateKeyPair();
    }

    public static byte[] calculateAgreement(byte[] publicKey, byte[] privateKey){
        return curve25519.calculateAgreement(publicKey, privateKey);
    }
}
