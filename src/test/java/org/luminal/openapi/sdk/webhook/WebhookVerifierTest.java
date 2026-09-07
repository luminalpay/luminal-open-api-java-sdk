package org.luminal.openapi.sdk.webhook;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.CardStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.SharedAccountOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.TransactionWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.RechargeCardTransferStatusWebhook;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void parsesRechargeCardTransactions() {
        String body = "{\"memberCardTransactionId\":\"31\",\"memberCardId\":\"5\","
                + "\"cardType\":\"RECHARGE\",\"status\":\"SUCCESS\","
                + "\"settleStatus\":\"SETTLED\",\"settleTime\":\"2026-08-31T11:15:30\","
                + "\"currencyCode\":\"USD\",\"tradeCurrencyCode\":\"EUR\","
                + "\"tradeTime\":\"2026-08-31T10:15:30\"}";

        TransactionWebhook payload = assertInstanceOf(TransactionWebhook.class,
                parse(WebhookEventType.CARD_TRANSACTIONS, body).payload());

        assertEquals("31", payload.memberCardTransactionId());
        assertEquals("RECHARGE", payload.cardType());
        assertEquals("SUCCESS", payload.status());
        assertEquals("SETTLED", payload.settleStatus());
        assertEquals(LocalDateTime.of(2026, 8, 31, 11, 15, 30), payload.settleTime());
        assertEquals("USD", payload.currencyCode());
        assertEquals("EUR", payload.tradeCurrencyCode());
    }

    @Test
    void parsesSharedCardSettlementStatus() {
        String body = "{\"sharedAccountTransactionId\":\"21\",\"cardType\":\"SHARED\","
                + "\"memberCardId\":\"5\",\"status\":\"SUCCESS\","
                + "\"settleStatus\":\"SETTLED\",\"settleTime\":\"2026-08-31T11:15:30\","
                + "\"type\":\"CARD_TRANSACTION\"}";

        TransactionWebhook payload = assertInstanceOf(TransactionWebhook.class,
                parse(WebhookEventType.CARD_SETTLE_STATUS, body).payload());

        assertEquals("21", payload.sharedAccountTransactionId());
        assertEquals("SHARED", payload.cardType());
        assertEquals("SETTLED", payload.settleStatus());
        assertEquals(LocalDateTime.of(2026, 8, 31, 11, 15, 30), payload.settleTime());
    }

    @Test
    void parsesRechargeCardSettlementStatus() {
        String body = "{\"memberCardTransactionId\":\"31\",\"memberCardId\":\"5\","
                + "\"cardType\":\"RECHARGE\",\"status\":\"SUCCESS\","
                + "\"settleStatus\":\"SETTLED\",\"settleTime\":\"2026-08-31T11:15:30\"}";

        TransactionWebhook payload = assertInstanceOf(TransactionWebhook.class,
                parse(WebhookEventType.CARD_SETTLE_STATUS, body).payload());

        assertEquals("31", payload.memberCardTransactionId());
        assertEquals("RECHARGE", payload.cardType());
        assertEquals("SETTLED", payload.settleStatus());
        assertEquals(LocalDateTime.of(2026, 8, 31, 11, 15, 30), payload.settleTime());
        assertNull(payload.sharedAccountTransactionId());
    }

    @Test
    void parsesRechargeCardFundingStatus() {
        String body = "{\"memberCardOperationRecordId\":601,\"memberCardId\":5,"
                + "\"cardType\":\"RECHARGE\",\"operationType\":\"RECHARGE\","
                + "\"amount\":25,\"status\":\"SUCCESS\","
                + "\"updateTime\":\"2026-08-31T10:15:30\"}";

        RechargeCardTransferStatusWebhook payload = assertInstanceOf(RechargeCardTransferStatusWebhook.class,
                parse(WebhookEventType.CARD_RECHARGE_STATUS, body).payload());

        assertEquals(601L, payload.memberCardOperationRecordId());
        assertEquals("RECHARGE", payload.operationType());
    }

    @Test
    void parsesRechargeCardWithdrawalStatus() {
        String body = "{\"memberCardOperationRecordId\":602,\"memberCardId\":5,"
                + "\"cardType\":\"RECHARGE\",\"operationType\":\"WITHDRAW\","
                + "\"amount\":10,\"status\":\"FAIL\",\"message\":\"declined\","
                + "\"updateTime\":\"2026-08-31T10:15:30\"}";

        RechargeCardTransferStatusWebhook payload = assertInstanceOf(RechargeCardTransferStatusWebhook.class,
                parse(WebhookEventType.CARD_WITHDRAW_STATUS, body).payload());

        assertEquals("WITHDRAW", payload.operationType());
        assertEquals("declined", payload.message());
    }

    @Test
    void parsesCardLimitStatus() {
        String body = "{\"memberCardOperationRecordId\":603,\"memberCardId\":5,"
                + "\"cardType\":\"RECHARGE\",\"operationType\":\"MODIFY_LIMITS\","
                + "\"status\":\"SUCCESS\",\"message\":\"limit updated\","
                + "\"updateTime\":\"2026-09-01T10:15:30\"}";

        RechargeCardTransferStatusWebhook payload = assertInstanceOf(RechargeCardTransferStatusWebhook.class,
                parse(WebhookEventType.CARD_LIMIT_STATUS, body).payload());

        assertEquals(603L, payload.memberCardOperationRecordId());
        assertEquals("MODIFY_LIMITS", payload.operationType());
        assertEquals("SUCCESS", payload.status());
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
