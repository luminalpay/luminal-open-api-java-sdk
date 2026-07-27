package org.luminal.openapi.sdk.webhook;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * A verified webhook event. Exact raw bytes are retained for auditing and replay diagnostics.
 *
 * @param type      verified event type
 * @param eventId   value of the {@code event_id} header
 * @param rawBody   exact bytes used during signature verification
 * @param signature value of the {@code sign} header
 * @param payload   event-specific payload
 */
public record WebhookEvent<T>(
        WebhookEventType type,
        String eventId,
        byte[] rawBody,
        String signature,
        T payload) {

    /**
     * Creates a verified event while defensively copying the raw request bytes.
     */
    public WebhookEvent {
        Objects.requireNonNull(type, "type");
        rawBody = Arrays.copyOf(Objects.requireNonNull(rawBody, "rawBody"), rawBody.length);
    }

    /**
     * @return a defensive copy of the exact verified request bytes
     */
    @Override
    public byte[] rawBody() {
        return Arrays.copyOf(rawBody, rawBody.length);
    }

    /**
     * @return the verified request bytes decoded as UTF-8 text
     */
    public String rawBodyUtf8() {
        return new String(rawBody, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "WebhookEvent[type=" + type + ", eventId=" + eventId
                + ", rawBody=<redacted>, signature=<redacted>, payload=<redacted>]";
    }
}
