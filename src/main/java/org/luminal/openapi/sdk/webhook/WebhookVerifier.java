package org.luminal.openapi.sdk.webhook;

import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.CardStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.SharedAccountOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.TransactionWebhook;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.io.IOException;

/**
 * Verifies webhook signatures against the exact raw body and parses supported event payloads.
 */
public final class WebhookVerifier {

    private WebhookVerifier() {
    }

    /**
     * Verifies a webhook signature against exact request bytes without parsing the payload.
     *
     * @param rawBody   exact request bytes
     * @param signature Base64 {@code SHA256withRSA} signature
     * @param publicKey RSA public key
     * @return {@code true} when the signature matches the exact bytes
     */
    public static boolean verify(byte[] rawBody, String signature, PublicKey publicKey) {
        if (rawBody == null) {
            return false;
        }
        return RsaSignatures.verify(rawBody, signature, publicKey);
    }

    /**
     * Verifies a UTF-8 webhook body without parsing the payload.
     *
     * @param rawBody   exact request body text
     * @param signature Base64 {@code SHA256withRSA} signature
     * @param publicKey RSA public key
     * @return {@code true} when the signature matches the UTF-8 bytes
     */
    public static boolean verify(String rawBody, String signature, PublicKey publicKey) {
        return rawBody != null && verify(rawBody.getBytes(StandardCharsets.UTF_8), signature, publicKey);
    }

    /**
     * Verifies and parses exact webhook request bytes using an RSA public key.
     *
     * @param eventHeader value of the {@code event} header
     * @param eventId     value of the {@code event_id} header
     * @param rawBody     exact request bytes
     * @param signature   value of the {@code sign} header
     * @param publicKey   RSA public key
     * @return verified event and event-specific payload
     */
    public static WebhookEvent<?> parse(
            String eventHeader,
            String eventId,
            byte[] rawBody,
            String signature,
            PublicKey publicKey) {
        requireHeader(eventHeader, "event");
        requireHeader(eventId, "event_id");
        requireHeader(signature, "sign");
        if (rawBody == null) {
            throw new WebhookVerificationException("Webhook raw body must not be null");
        }
        if (!verify(rawBody, signature, publicKey)) {
            throw new WebhookVerificationException("Webhook signature is invalid");
        }

        final WebhookEventType type;
        try {
            type = WebhookEventType.valueOf(eventHeader);
        } catch (IllegalArgumentException exception) {
            throw new WebhookVerificationException("Unsupported webhook event: " + eventHeader, exception);
        }

        Object payload = switch (type) {
            case CARD_OPEN_STATUS -> read(rawBody, CardOpenStatusWebhook.class);
            case CARD_STATUS -> read(rawBody, CardStatusWebhook.class);
            case SHARED_ACCOUNT_OPEN_STATUS -> read(rawBody, SharedAccountOpenStatusWebhook.class);
            case CARD_TRANSACTIONS, SHARE_ACCOUNT_FUND_TRANSACTIONS -> read(rawBody, TransactionWebhook.class);
        };
        return new WebhookEvent<>(type, eventId, rawBody, signature, payload);
    }

    /**
     * Verifies and parses a UTF-8 webhook body using an RSA public key.
     *
     * @param eventHeader value of the {@code event} header
     * @param eventId     value of the {@code event_id} header
     * @param rawBody     exact request body text
     * @param signature   value of the {@code sign} header
     * @param publicKey   RSA public key
     * @return verified event and event-specific payload
     */
    public static WebhookEvent<?> parse(
            String eventHeader,
            String eventId,
            String rawBody,
            String signature,
            PublicKey publicKey) {
        if (rawBody == null) {
            throw new WebhookVerificationException("Webhook raw body must not be null");
        }
        return parse(eventHeader, eventId, rawBody.getBytes(StandardCharsets.UTF_8), signature, publicKey);
    }

    /**
     * Verifies and parses exact webhook request bytes using an X.509 RSA public-key PEM string.
     *
     * @param eventHeader  value of the {@code event} header
     * @param eventId      value of the {@code event_id} header
     * @param rawBody      exact request bytes
     * @param signature    value of the {@code sign} header
     * @param publicKeyPem X.509 RSA public-key PEM text
     * @return verified event and event-specific payload
     */
    public static WebhookEvent<?> parse(
            String eventHeader,
            String eventId,
            byte[] rawBody,
            String signature,
            String publicKeyPem) {
        return parse(eventHeader, eventId, rawBody, signature, RsaSignatures.readPublicKey(publicKeyPem));
    }

    /**
     * Verifies and parses a UTF-8 webhook body using an X.509 RSA public-key PEM string.
     *
     * @param eventHeader  value of the {@code event} header
     * @param eventId      value of the {@code event_id} header
     * @param rawBody      exact request body text
     * @param signature    value of the {@code sign} header
     * @param publicKeyPem X.509 RSA public-key PEM text
     * @return verified event and event-specific payload
     */
    public static WebhookEvent<?> parse(
            String eventHeader,
            String eventId,
            String rawBody,
            String signature,
            String publicKeyPem) {
        if (rawBody == null) {
            throw new WebhookVerificationException("Webhook raw body must not be null");
        }
        return parse(eventHeader, eventId, rawBody.getBytes(StandardCharsets.UTF_8), signature, publicKeyPem);
    }

    private static <T> T read(byte[] rawBody, Class<T> payloadType) {
        try {
            return JsonSupport.readValue(rawBody, payloadType);
        } catch (IOException exception) {
            throw new WebhookVerificationException("Webhook payload is invalid for " + payloadType.getSimpleName(), exception);
        }
    }

    private static void requireHeader(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new WebhookVerificationException("Webhook " + name + " header must not be blank");
        }
    }
}
