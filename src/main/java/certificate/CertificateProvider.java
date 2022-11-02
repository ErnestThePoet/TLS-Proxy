package certificate;

public interface CertificateProvider {
    /**
     * 提供证书字节序列。
     *
     * @return  UTF-8编码的证书字节序列。
     */
    byte[] getCertificate();

    /**
     * 使用证书私钥对通信字节序列进行签名。
     *
     * @param   traffic
     *          要被签名的通信数据字节序列。
     *
     * @return  签名结果的字节序列。
     */
    byte[] signTraffic(byte[] traffic);

    static CertificateProvider getInstance(){
        return new ErnestCertificateProvider();
    }
}
