package certificate;

import crypto.hash.Sha384;

public class ErnestCertificateProvider implements CertificateProvider{
    @Override
    public byte[] getCertificate() {
        return new byte[0];
    }

    @Override
    public byte[] signTraffic(byte[] traffic) {
        // TODO This is not signature. Only for demo.
        return Sha384.hash(traffic);
    }
}
