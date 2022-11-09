package handshake.certificate.impl;

import crypto.hash.Sha384;
import handshake.certificate.CertificateValidator;

import java.util.Base64;

// TODO Demo code only
public class ErnestCertificateValidator implements CertificateValidator {
    @Override
    public boolean validateCertificate(byte[] certificate,String host) {
        return true;
    }

    @Override
    public boolean validateTrafficSignature(byte[] certificate, byte[] traffic, byte[] trafficSignature) {
        // TODO This is NOT signature validation. Only for demo.
        return Base64.getEncoder().encodeToString(Sha384.hash(traffic))
                .equals(Base64.getEncoder().encodeToString(trafficSignature));
    }
}