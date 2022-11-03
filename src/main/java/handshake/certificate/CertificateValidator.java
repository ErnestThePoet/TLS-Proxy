package handshake.certificate;

import handshake.certificate.impl.ErnestCertificateValidator;

public interface CertificateValidator {
    /**
     * 验证证书合法性。
     *
     * @param   certificate
     *          UTF-8编码的证书字节序列。
     *
     * @return  返回{@code true}如果验证通过，否则返回{@code false}。
     */
    boolean validateCertificate(byte[] certificate);

    /**
     * 验证通信数据签名的合法性。
     *
     * @param   certificate
     *          UTF-8编码的证书字节序列。
     *
     * @param   traffic
     *          被签名的通信数据字节序列。
     *
     * @param   trafficSignature
     *          要被验证的签名的字节序列。
     *
     * @return  返回{@code true}如果验证通过，否则返回{@code false}。
     */
    boolean validateTrafficSignature(byte[] certificate,byte[] traffic, byte[] trafficSignature);

    static CertificateValidator getInstance(){
        return new ErnestCertificateValidator();
    }
}
