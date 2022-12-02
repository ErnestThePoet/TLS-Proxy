package handshake.certificate.impl;

import crypto.sign.Rsa;
import handshake.certificate.CertificateProvider;
import utils.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ErnestCertificateProvider implements CertificateProvider {
    @Override
    public byte[] getCertificate() {
        try {
            return Files.readAllBytes(Path.of("./cert/cert.crt"));
        } catch (IOException e) {
            Log.error(e);
            return null;
        }
    }

    @Override
    public byte[] signTraffic(byte[] traffic) {
        try {
            var privateKeyBase64 = Files.readAllBytes(Path.of("./cert/private.key"));
            return Rsa.sign(Base64.getDecoder().decode(privateKeyBase64), traffic);
        } catch (IOException e) {
            Log.error(e);
            return null;
        }
    }
}
