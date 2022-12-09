package handshake.certificate.impl;

import handshake.certificate.objs.CertificateValidationResult;
import handshake.certificate.CertificateValidator;

public class ErnestCertificateValidator implements CertificateValidator {
    @Override
    public CertificateValidationResult validateCertificate(
            byte[] certificate, String host) {
        return new CertificateValidationResult(true,"");
    }

    @Override
    public boolean validateTrafficSignature(
            byte[] certificate, byte[] traffic, byte[] trafficSignature) {
        return true;
    }
}
