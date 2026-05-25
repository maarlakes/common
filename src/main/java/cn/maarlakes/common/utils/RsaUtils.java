package cn.maarlakes.common.utils;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;

/**
 * @author linjpxc
 */
public final class RsaUtils {
    private RsaUtils() {
    }

    public static PrivateKey loadPrivateKey(File privateKeyFile, String keyPwd) throws Exception {
        try (InputStream input = Files.newInputStream(privateKeyFile.toPath())) {
            return loadPrivateKey(input, keyPwd);
        }
    }

    public static PrivateKey loadPrivateKey(InputStream privateKeyData, String keyPwd) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        final char[] pwdChars = keyPwd == null ? new char[0] : keyPwd.toCharArray();
        keyStore.load(privateKeyData, pwdChars);
        String keyAlias = null;
        final Enumeration<String> aliases = keyStore.aliases();
        if (aliases.hasMoreElements()) {
            keyAlias = aliases.nextElement();
        }
        if (keyAlias == null) {
            throw new IllegalArgumentException("No alias found in keystore");
        }

        final Key key = keyStore.getKey(keyAlias, pwdChars);

        final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(key.getEncoded());
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    public static PublicKey loadPublicKey(final File publicKeyFile) throws Exception {
        try (InputStream input = Files.newInputStream(publicKeyFile.toPath())) {
            return loadPublicKey(input);
        }
    }

    public static PublicKey loadPublicKey(InputStream publicKeyData) throws Exception {
        final byte[] buffer = StreamUtils.readAllBytes(publicKeyData);
        final BufferedReader br = new BufferedReader(new StringReader(new String(buffer, StandardCharsets.UTF_8)));
        final StringBuilder builder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("-")) {
                builder.append(line);
            }
        }

        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        return certificateFactory.generateCertificate(
                new ByteArrayInputStream(Base64.getDecoder().decode(builder.toString()))
        ).getPublicKey();
    }
}
