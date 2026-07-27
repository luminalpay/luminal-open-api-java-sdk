package org.luminal.openapi.sdk.model;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;
import org.luminal.openapi.sdk.model.AuthModels.RefreshTokenRequest;
import org.luminal.openapi.sdk.model.CardModels.CardCvvResponse;
import org.luminal.openapi.sdk.webhook.WebhookEvent;
import org.luminal.openapi.sdk.webhook.WebhookEventType;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveModelsTest {

    @Test
    void toStringRedactsSecretsAndWebhookData() {
        OAuth2Token token = new OAuth2Token("access-secret", "Bearer", 1L,
                "refresh-secret", "scope", "jti");
        RefreshTokenRequest refresh = new RefreshTokenRequest("refresh-secret");
        CardCvvResponse cvv = new CardCvvResponse(5L, "4111111111111111", "123", "2030-01");
        WebhookEvent<String> event = new WebhookEvent<>(WebhookEventType.CARD_STATUS, "event-1",
                "raw-secret".getBytes(StandardCharsets.UTF_8), "signature-secret", "payload-secret");

        String rendered = token + " " + refresh + " " + cvv + " " + event;

        assertFalse(rendered.contains("access-secret"));
        assertFalse(rendered.contains("refresh-secret"));
        assertFalse(rendered.contains("4111111111111111"));
        assertFalse(rendered.contains("123"));
        assertFalse(rendered.contains("2030-01"));
        assertFalse(rendered.contains("raw-secret"));
        assertFalse(rendered.contains("signature-secret"));
        assertFalse(rendered.contains("payload-secret"));
    }
}
