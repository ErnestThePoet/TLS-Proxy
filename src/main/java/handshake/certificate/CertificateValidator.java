package handshake.certificate;

import handshake.certificate.impl.ErnestCertificateValidator;
import handshake.certificate.objs.CertificateValidationResult;

public interface CertificateValidator {
    /**
     * 验证证书合法性。
     *
     * @param   certificate
     *          UTF-8编码的证书字节序列。
     *
     * @param   host
     *          UTF-8编码的当前客户端正在访问的主机名，例如192.168.3.254:8080。
     *
     * @return  返回一个{@code CertificateValidationResult}对象，包含验证是否通过以及如果失败的错误信息。
     */
    CertificateValidationResult validateCertificate(byte[] certificate, String host);

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
