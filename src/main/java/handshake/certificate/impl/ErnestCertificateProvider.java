package handshake.certificate.impl;

import handshake.certificate.CertificateProvider;

// TODO: Demo code only
public class ErnestCertificateProvider implements CertificateProvider {
    @Override
    public byte[] getCertificate() {
        return new byte[10];
    }

    @Override
    public byte[] signTraffic(byte[] traffic) {
        return new byte[10];
    }
}
