package proxy;

import crypto.encryption.AesKey;
import crypto.encryption.DualAesKey;
import crypto.hash.Sha384;
import org.whispersystems.curve25519.Curve25519;
import utils.ByteArrayUtil;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public abstract class HandshakeController {
    private final List<byte[]> transmittedBytes;
    private final Curve25519 curve25519;
    protected byte[] selfPublicKey;
    protected byte[] selfPrivateKey;

    protected DualAesKey handshakeKey;

    protected HandshakeController(){
        this.curve25519=Curve25519.getInstance(Curve25519.BEST);
        this.transmittedBytes=new ArrayList<>();
    }

    protected void generateX22519KeyPair(){
        var keyPair= this.curve25519.generateKeyPair();
        this.selfPrivateKey=keyPair.getPrivateKey();
        this.selfPublicKey=keyPair.getPublicKey();
    }

    protected byte[] getRandomWithPublicKey(){
        byte[] randomBytes=new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return ByteArrayUtil.concat(randomBytes,this.selfPublicKey);
    }

    protected void addTransmittedBytes(byte[] bytes){
        this.transmittedBytes.add(bytes);
    }

    protected byte[] getTransmittedBytesHash(){
        return Sha384.hash(ByteArrayUtil.concat(this.transmittedBytes));
    }

    protected AesKey calculateHandshakeKey(byte[] oppositePublicKey){
        var sharedKey= this.curve25519.calculateAgreement(oppositePublicKey,this.selfPrivateKey);
        var transmittedBytesHash=this.getTransmittedBytesHash();

        System.out.println(Base64.getEncoder().encodeToString(sharedKey));
        System.out.println(Base64.getEncoder().encodeToString(transmittedBytesHash));

        return null;
    }
    public abstract DualAesKey negotiateApplicationKey();
}
