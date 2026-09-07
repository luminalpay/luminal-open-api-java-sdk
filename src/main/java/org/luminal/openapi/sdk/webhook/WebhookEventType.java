package org.luminal.openapi.sdk.webhook;

/**
 * Supported values of the webhook {@code event} header.
 */
public enum WebhookEventType {
    /**
     * Card transaction notification.
     */
    CARD_TRANSACTIONS,
    /**
     * Card settlement status change notification.
     */
    CARD_SETTLE_STATUS,
    /**
     * Card status change notification.
     */
    CARD_STATUS,
    /**
     * Card application status notification.
     */
    CARD_OPEN_STATUS,
    /**
     * Recharge card funding result notification.
     */
    CARD_RECHARGE_STATUS,
    /**
     * Recharge card withdrawal result notification.
     */
    CARD_WITHDRAW_STATUS,
    /**
     * Card limit modification result notification.
     */
    CARD_LIMIT_STATUS,
    /**
     * Shared account application status notification.
     */
    SHARED_ACCOUNT_OPEN_STATUS,
    /**
     * Shared account fund transaction notification.
     */
    SHARE_ACCOUNT_FUND_TRANSACTIONS
}
