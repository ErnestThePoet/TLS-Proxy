package crypto.encryption.objs;

public record DualAesKey(AesKey clientKey,AesKey serverKey){}
