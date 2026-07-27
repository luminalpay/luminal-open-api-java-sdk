package org.luminal.openapi.sdk.webhook;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.webhook.WebhookEvent;
import org.luminal.openapi.sdk.webhook.WebhookEventType;
import org.luminal.openapi.sdk.webhook.WebhookVerificationException;
import org.luminal.openapi.sdk.webhook.WebhookVerifier;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.CardStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.SharedAccountOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.TransactionWebhook;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookVerifierTest {

    private static KeyPair keyPair;

    @BeforeAll
    static void createKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    void verifiesExactRawBodyAndRejectsTampering() {
        byte[] body = bytes("{\"memberCardId\":\"5\",\"cardStatus\":\"ACTIVE\"}");
        String signature = sign(body);

        assertTrue(WebhookVerifier.verify(body, signature, keyPair.getPublic()));
        assertFalse(WebhookVerifier.verify(bytes(new String(body, StandardCharsets.UTF_8) + " "),
                signature, keyPair.getPublic()));
    }

    @Test
    void verifiesUtf8StringBody() {
        String body = "{\"memberCardId\":\"5\",\"cardStatus\":\"ACTIVE\"}";

        assertTrue(WebhookVerifier.verify(body, sign(body), keyPair.getPublic()));
        assertFalse(WebhookVerifier.verify(body + " ", sign(body), keyPair.getPublic()));
    }

    @Test
    void parsesCardTransactions() {
        String body = "{\"sharedAccountTransactionId\":\"21\",\"memberCardId\":\"5\",\"orderNo\":\"O1\",\"tradeTime\":\"2026-07-01T10:15:30\"}";

        WebhookEvent<?> event = parse(WebhookEventType.CARD_TRANSACTIONS, body);

        TransactionWebhook payload = assertInstanceOf(TransactionWebhook.class, event.payload());
        assertEquals("O1", payload.orderNo());
        assertEquals("event-1", event.eventId());
    }

    @Test
    void parsesCardStatus() {
        String body = "{\"memberCardId\":\"5\",\"cardStatus\":\"FREEZE\",\"memberNo\":\"9\",\"updateTime\":\"2026-07-01T10:15:30\"}";

        WebhookEvent<?> event = parse(WebhookEventType.CARD_STATUS, body);

        CardStatusWebhook payload = assertInstanceOf(CardStatusWebhook.class, event.payload());
        assertEquals("FREEZE", payload.cardStatus());
    }

    @Test
    void parsesCardOpenStatus() {
        String body = "{\"cardApplyTaskId\":501,\"memberId\":9,\"list\":[{\"memberCardId\":5,\"cardStatus\":\"ACTIVE\",\"message\":\"created\"}]}";

        WebhookEvent<?> event = parse(WebhookEventType.CARD_OPEN_STATUS, body);

        CardOpenStatusWebhook payload = assertInstanceOf(CardOpenStatusWebhook.class, event.payload());
        assertEquals(501L, payload.cardApplyTaskId());
        assertEquals("created", payload.list().get(0).message());
    }

    @Test
    void parsesSharedAccountOpenStatus() {
        String body = "{\"sharedAccountOperationRecordId\":31,\"memberNo\":9,\"memberSharedAccountId\":11,\"status\":\"SUCCESS\"}";

        WebhookEvent<?> event = parse(WebhookEventType.SHARED_ACCOUNT_OPEN_STATUS, body);

        SharedAccountOpenStatusWebhook payload =
                assertInstanceOf(SharedAccountOpenStatusWebhook.class, event.payload());
        assertEquals(11L, payload.memberSharedAccountId());
    }

    @Test
    void parsesSharedAccountFundTransactions() {
        String body = "{\"sharedAccountTransactionId\":\"21\",\"memberSharedAccountId\":\"11\",\"type\":\"DEPOSIT\",\"tradeAmount\":10}";

        WebhookEvent<?> event = parse(WebhookEventType.SHARE_ACCOUNT_FUND_TRANSACTIONS, body);

        TransactionWebhook payload = assertInstanceOf(TransactionWebhook.class, event.payload());
        assertEquals("DEPOSIT", payload.type());
    }

    @Test
    void parsesWithX509PemPublicKey() {
        String body = "{\"memberCardId\":\"5\",\"cardStatus\":\"ACTIVE\"}";
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        WebhookEvent<?> event = WebhookVerifier.parse(
                "CARD_STATUS", "event-1", body, sign(body), pem);

        assertEquals(WebhookEventType.CARD_STATUS, event.type());
    }

    @Test
    void parsesAndRetainsExactRequestBytes() {
        byte[] body = bytes("{\"memberCardId\":\"5\",\"cardStatus\":\"ACTIVE\"}");

        WebhookEvent<?> event = WebhookVerifier.parse(
                "CARD_STATUS", "event-1", body, sign(body), keyPair.getPublic());

        assertEquals(new String(body, StandardCharsets.UTF_8), event.rawBodyUtf8());
        byte[] retained = event.rawBody();
        retained[0] = 'X';
        assertTrue(event.rawBodyUtf8().startsWith("{"));
        body[0] = 'X';
        assertTrue(event.rawBodyUtf8().startsWith("{"));
    }

    @Test
    void rejectsInvalidSignature() {
        String body = "{\"memberCardId\":\"5\"}";

        assertThrows(WebhookVerificationException.class,
                () -> WebhookVerifier.parse("CARD_STATUS", "event-1", body, sign("different"), keyPair.getPublic()));
    }

    @Test
    void rejectsUnknownEvent() {
        String body = "{}";

        WebhookVerificationException exception = assertThrows(WebhookVerificationException.class,
                () -> WebhookVerifier.parse("UNKNOWN", "event-1", body, sign(body), keyPair.getPublic()));

        assertTrue(exception.getMessage().contains("Unsupported webhook event"));
    }

    private static WebhookEvent<?> parse(WebhookEventType type, String body) {
        return WebhookVerifier.parse(type.name(), "event-1", body, sign(body), keyPair.getPublic());
    }

    private static String sign(String body) {
        return sign(bytes(body));
    }

    private static String sign(byte[] body) {
        return RsaSignatures.sign(body, keyPair.getPrivate());
    }

    private static byte[] bytes(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }
}
