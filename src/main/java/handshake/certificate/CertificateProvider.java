package handshake.certificate;

import handshake.certificate.impl.ErnestCertificateProvider;

// TODO: 实现您自己的CertificateProvider类，并修改下面的getInstance方法使其返回一个您实现的类的对象
public interface CertificateProvider {
    /**
     * 提供证书字节序列。
     *
     * @return  UTF-8编码的证书字节序列，或者{@code null}如果无法获取证书。
     */
    byte[] getCertificate();

    /**
     * 使用证书私钥对通信字节序列进行签名。
     *
     * @param   traffic
     *          要被签名的通信数据字节序列。
     *
     * @return  签名结果的字节序列，或者{@code null}如果签名失败。
     */
    byte[] signTraffic(byte[] traffic);

    static CertificateProvider getInstance(){
        return new ErnestCertificateProvider();
    }
}
