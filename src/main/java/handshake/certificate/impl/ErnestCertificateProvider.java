package handshake.certificate.impl;

import crypto.hash.Sha384;
import handshake.certificate.CertificateProvider;

// TODO Demo code only
public class ErnestCertificateProvider implements CertificateProvider {
    @Override
    public byte[] getCertificate() {
        return new byte[10];
    }

    @Override
    public byte[] signTraffic(byte[] traffic) {
        // TODO This is not signature. Only for demo.
        return Sha384.hash(traffic);
    }
}
