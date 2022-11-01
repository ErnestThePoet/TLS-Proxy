package certificate;

public interface CertificateValidator {
    boolean validateCertificate(byte[] certificate);
    boolean validateTrafficSignature(byte[] certificate,byte[] traffic, byte[] trafficSignature);

    static CertificateValidator getInstance(){
        return new ErnestCertificateValidator();
    }
}
