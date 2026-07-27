package org.luminal.openapi.sdk.webhook;

/** Supported values of the webhook {@code event} header. */
public enum WebhookEventType {
    CARD_TRANSACTIONS,
    CARD_STATUS,
    CARD_OPEN_STATUS,
    SHARED_ACCOUNT_OPEN_STATUS,
    SHARE_ACCOUNT_FUND_TRANSACTIONS
}
