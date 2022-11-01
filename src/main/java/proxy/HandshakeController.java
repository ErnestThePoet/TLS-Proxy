package proxy;

import at.favre.lib.crypto.HKDF;
import at.favre.lib.crypto.HkdfMacFactory;
import crypto.encoding.Utf8;
import crypto.encryption.AesKey;
import crypto.encryption.DualAesKey;
import crypto.hash.Sha384;
import org.whispersystems.curve25519.Curve25519;
import utils.ByteArrayUtil;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public abstract class HandshakeController {
    private final List<byte[]> traffic;
    private final Curve25519 curve25519;
    protected byte[] selfPublicKey;
    protected byte[] selfPrivateKey;

    protected DualAesKey handshakeKey;

    protected HandshakeController(){
        this.curve25519=Curve25519.getInstance(Curve25519.BEST);
        this.traffic=new ArrayList<>();
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

    protected void addTraffic(byte[] bytes){
        this.traffic.add(bytes);
    }
    protected byte[] getTrafficConcat(){
        return ByteArrayUtil.concat(this.traffic);
    }

    protected byte[] getTrafficHash(){
        return Sha384.hash(this.getTrafficConcat());
    }

    protected void calculateHandshakeKey(byte[] oppositePublicKey){
        var sharedSecret= this.curve25519.calculateAgreement(oppositePublicKey,this.selfPrivateKey);
        var trafficHash=this.getTrafficHash();

        HKDF hkdf=HKDF.from(new HkdfMacFactory.Default("HmacSha384",null));

        var earlySecret=hkdf.extract(new byte[48],new byte[48]);

        var derivedSecret=hkdf.expand(earlySecret,Utf8.decode("derived"),48);
        var handshakeSecret=hkdf.extract(derivedSecret,sharedSecret);

        var clientTrafficInfo=hkdf.expand(trafficHash,Utf8.decode("c hs traffic"),48);
        var clientSecret=hkdf.expand(handshakeSecret,clientTrafficInfo,48);

        var serverTrafficInfo=hkdf.expand(trafficHash,Utf8.decode("s hs traffic"),48);
        var serverSecret=hkdf.expand(handshakeSecret,serverTrafficInfo,48);

        var clientHandshakeKey=hkdf.expand(clientSecret,Utf8.decode("key"),16);
        var serverHandshakeKey=hkdf.expand(serverSecret,Utf8.decode("key"),16);

        var clientHandshakeIv=hkdf.expand(clientSecret,Utf8.decode("iv"),16);
        var serverHandshakeIv=hkdf.expand(serverSecret,Utf8.decode("iv"),16);

        this.handshakeKey= new DualAesKey(
                new AesKey(clientHandshakeKey,clientHandshakeIv),
                new AesKey(serverHandshakeKey,serverHandshakeIv)
        );
    }
    public abstract DualAesKey negotiateApplicationKey();
}
