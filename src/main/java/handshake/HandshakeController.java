package handshake;

import communication.SynchronizedTransceiver;
import crypto.encoding.Utf8;
import crypto.encryption.objs.AesKey;
import crypto.encryption.objs.DualAesKey;
import crypto.hash.Sha384;
import crypto.kdf.HkdfSha384;
import crypto.keyex.Curve25519KeyEx;
import exceptions.TlsException;
import utils.ByteArrayUtil;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public abstract class HandshakeController {
    private final List<byte[]> traffic;
    protected final SynchronizedTransceiver synchronizedTransceiver;
    protected byte[] selfPublicKey;
    protected byte[] selfPrivateKey;

    protected byte[] handshakeSecret;
    protected byte[] clientSecret;
    protected byte[] serverSecret;
    protected DualAesKey handshakeKey;
    protected DualAesKey applicationKey;

    public HandshakeController(Socket remoteSocket){
        this.traffic=new ArrayList<>();
        this.synchronizedTransceiver=new SynchronizedTransceiver(remoteSocket);
    }

    protected void generateX22519KeyPair(){
        var keyPair= Curve25519KeyEx.generateKeyPair();
        this.selfPrivateKey=keyPair.getPrivateKey();
        this.selfPublicKey=keyPair.getPublicKey();
    }

    protected byte[] getRandomWithPublicKey(){
        byte[] randomBytes=new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return ByteArrayUtil.concat(randomBytes,this.selfPublicKey);
    }

    protected void addTraffic(byte[] bytes) {
        this.traffic.add(bytes);
    }

    protected byte[] getTrafficConcat(){
        return ByteArrayUtil.concat(this.traffic);
    }

    protected byte[] getTrafficHash(){
        return Sha384.hash(this.getTrafficConcat());
    }

    protected void calculateHandshakeKey(byte[] oppositePublicKey){
        var sharedSecret= Curve25519KeyEx.calculateAgreement(oppositePublicKey,this.selfPrivateKey);
        var trafficHash=this.getTrafficHash();

        var earlySecret= HkdfSha384.extract(new byte[48],new byte[48]);

        var derivedSecret=HkdfSha384.expand(earlySecret,Utf8.decode("derived"),48);
        var handshakeSecret=HkdfSha384.extract(derivedSecret,sharedSecret);
        this.handshakeSecret=handshakeSecret;

        var clientTrafficInfo=HkdfSha384.expand(trafficHash,Utf8.decode("c hs traffic"),48);
        var clientSecret=HkdfSha384.expand(handshakeSecret,clientTrafficInfo,48);
        this.clientSecret=clientSecret;

        var serverTrafficInfo=HkdfSha384.expand(trafficHash,Utf8.decode("s hs traffic"),48);
        var serverSecret=HkdfSha384.expand(handshakeSecret,serverTrafficInfo,48);
        this.serverSecret=serverSecret;

        var clientHandshakeKey=HkdfSha384.expand(clientSecret,Utf8.decode("key"),16);
        var serverHandshakeKey=HkdfSha384.expand(serverSecret,Utf8.decode("key"),16);

        var clientHandshakeIv=HkdfSha384.expand(clientSecret,Utf8.decode("iv"),16);
        var serverHandshakeIv=HkdfSha384.expand(serverSecret,Utf8.decode("iv"),16);

        this.handshakeKey= new DualAesKey(
                new AesKey(clientHandshakeKey,clientHandshakeIv),
                new AesKey(serverHandshakeKey,serverHandshakeIv)
        );
    }

    protected void calculateApplicationKey(){
        var trafficHash=this.getTrafficHash();

        var derivedSecret=HkdfSha384.expand(this.handshakeSecret,Utf8.decode("derived"),48);
        var masterSecret=HkdfSha384.extract(derivedSecret,new byte[48]);

        var clientTrafficInfo=HkdfSha384.expand(trafficHash,Utf8.decode("c ap traffic"),48);
        var clientSecret=HkdfSha384.expand(masterSecret,clientTrafficInfo,48);

        var serverTrafficInfo=HkdfSha384.expand(trafficHash,Utf8.decode("s ap traffic"),48);
        var serverSecret=HkdfSha384.expand(masterSecret,serverTrafficInfo,48);

        var clientApplicationKey=HkdfSha384.expand(clientSecret,Utf8.decode("key"),16);
        var serverApplicationKey=HkdfSha384.expand(serverSecret,Utf8.decode("key"),16);

        var clientApplicationIv=HkdfSha384.expand(clientSecret,Utf8.decode("iv"),16);
        var serverApplicationIv=HkdfSha384.expand(serverSecret,Utf8.decode("iv"),16);

        this.applicationKey= new DualAesKey(
                new AesKey(clientApplicationKey,clientApplicationIv),
                new AesKey(serverApplicationKey,serverApplicationIv)
        );
    }

    public abstract DualAesKey negotiateApplicationKey() throws IOException, TlsException;
}
