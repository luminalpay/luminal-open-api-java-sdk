package org.luminal.openapi.sdk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsaSignaturesTest {

    private static KeyPair keyPair;

    @BeforeAll
    static void createKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void signsAndVerifiesExactBytes() {
        byte[] content = "exact content".getBytes(StandardCharsets.UTF_8);
        String signature = RsaSignatures.sign(content, keyPair.getPrivate());

        assertTrue(RsaSignatures.verify(content, signature, keyPair.getPublic()));
        assertFalse(RsaSignatures.verify("changed".getBytes(StandardCharsets.UTF_8), signature, keyPair.getPublic()));
    }

    @Test
    void readsPkcs8PrivateAndX509PublicPem() {
        String privatePem = pem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        String publicPem = pem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        assertEquals(keyPair.getPrivate(), RsaSignatures.readPrivateKey(privatePem));
        assertEquals(keyPair.getPublic(), RsaSignatures.readPublicKey(publicPem));
    }

    @Test
    void signsCanonicalJson() {
        Map<String, Object> value = Map.of("z", 2, "a", 1);
        String signature = RsaSignatures.signCanonicalJson(value, keyPair.getPrivate());

        assertTrue(RsaSignatures.verify("{\"a\":1,\"z\":2}".getBytes(StandardCharsets.UTF_8),
                signature, keyPair.getPublic()));
    }

    private static String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encoded)
                + "\n-----END " + type + "-----";
    }
}
