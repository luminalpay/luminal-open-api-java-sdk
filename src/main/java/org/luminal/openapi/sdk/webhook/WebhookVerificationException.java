package org.luminal.openapi.sdk.webhook;

/** Reports an invalid webhook signature or payload. */
public final class WebhookVerificationException extends RuntimeException {

    WebhookVerificationException(String message) {
        super(message);
    }

    WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
