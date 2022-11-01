package certificate;

public interface CertificateProvider {
    byte[] getCertificate();
    byte[] signTraffic(byte[] traffic);

    static CertificateProvider getInstance(){
        return new ErnestCertificateProvider();
    }
}
