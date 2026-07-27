package org.luminal.openapi.sdk;

import org.luminal.openapi.sdk.internal.JsonSupport;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * SHA-256 with RSA signing, verification, canonical JSON, and PEM key helpers.
 */
public final class RsaSignatures {

    private static final String ALGORITHM = "SHA256withRSA";

    private RsaSignatures() {
    }

    /**
     * Signs exact bytes using {@code SHA256withRSA}.
     *
     * @param content    exact bytes to sign
     * @param privateKey RSA private key
     * @return Base64-encoded signature
     */
    public static String sign(byte[] content, PrivateKey privateKey) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(privateKey, "privateKey");
        try {
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(privateKey);
            signer.update(content);
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Content could not be signed with SHA256withRSA", exception);
        }
    }

    /**
     * Verifies a Base64 signature against exact bytes using {@code SHA256withRSA}.
     *
     * @param content   exact signed bytes
     * @param signature Base64-encoded signature
     * @param publicKey RSA public key
     * @return {@code true} when the signature is valid
     */
    public static boolean verify(byte[] content, String signature, PublicKey publicKey) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(publicKey, "publicKey");
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(content);
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (IllegalArgumentException exception) {
            return false;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Signature could not be verified with SHA256withRSA", exception);
        }
    }

    /**
     * Serializes a value with the SDK canonical JSON rules, then signs the resulting UTF-8 bytes.
     *
     * @param value      value to serialize and sign
     * @param privateKey RSA private key
     * @return Base64-encoded signature
     */
    public static String signCanonicalJson(Object value, PrivateKey privateKey) {
        return sign(JsonSupport.writeBytes(Objects.requireNonNull(value, "value")), privateKey);
    }

    /**
     * Reads an RSA public key from X.509 PEM text.
     *
     * @param pem PEM text containing an X.509 RSA public key
     * @return decoded RSA public key
     */
    public static PublicKey readPublicKey(String pem) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodePem(
                    pem, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----")));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid X.509 RSA public key PEM", exception);
        }
    }

    /**
     * Reads an RSA private key from PKCS#8 PEM text.
     *
     * @param pem PEM text containing a PKCS#8 RSA private key
     * @return decoded RSA private key
     */
    public static PrivateKey readPrivateKey(String pem) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodePem(
                    pem, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----")));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid PKCS#8 RSA private key PEM", exception);
        }
    }

    private static byte[] decodePem(String pem, String begin, String end) {
        if (pem == null || !pem.contains(begin) || !pem.contains(end)) {
            throw new IllegalArgumentException("PEM header or footer is missing");
        }
        String base64 = pem.replace(begin, "").replace(end, "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
