package handshake.certificate.impl;

import crypto.sign.Rsa;
import handshake.certificate.objs.CertificateValidationResult;
import handshake.certificate.CertificateValidator;
import utils.ExceptionUtil;
import utils.Log;

import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.*;

public class ErnestCertificateValidator implements CertificateValidator {
    @Override
    public CertificateValidationResult validateCertificate(
            byte[] certificate, String host) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(
                    new ByteArrayInputStream(certificate)
            );

            try {
                cert.checkValidity();
            } catch (CertificateExpiredException expiredException) {
                return new CertificateValidationResult(
                        false, "证书已过期。" + expiredException.getMessage()
                );
            } catch (CertificateNotYetValidException notYetValidException) {
                return new CertificateValidationResult(
                        false, "证书无效。" + notYetValidException.getMessage()
                );
            }

            try {
                cert.verify(cert.getPublicKey());
            } catch (GeneralSecurityException e) {
                return new CertificateValidationResult(
                        false, "证书签名验证失败。" + e.getMessage()
                );
            }

            String domainName = host.replace("www.", "");
            String certDomainName = "";

            for (var entry : cert.getSubjectX500Principal().getName().split(",")) {
                entry = entry.replace(" ", "");
                if (entry.startsWith("CN=")) {
                    certDomainName = entry.split("=")[1];
                    break;
                }
            }

            if (!domainName.equals(certDomainName)) {
                return new CertificateValidationResult(
                        false,
                        String.format("正在访问的域名是：%s，而证书被签发给域名：%s。您可能并不是在与%s通信。",
                                domainName,
                                certDomainName,
                                domainName)
                );
            }

            return new CertificateValidationResult(true, "");
        } catch (CertificateException e) {
            return new CertificateValidationResult(
                    false, ExceptionUtil.getExceptionBrief(e));
        }
    }

    @Override
    public boolean validateTrafficSignature(
            byte[] certificate, byte[] traffic, byte[] trafficSignature) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(
                    new ByteArrayInputStream(certificate)
            );

            return Rsa.verify(cert.getPublicKey(), traffic, trafficSignature);
        } catch (CertificateException e) {
            Log.error(e);
            return false;
        }
    }
}
