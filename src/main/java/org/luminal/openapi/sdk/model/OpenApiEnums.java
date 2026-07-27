package org.luminal.openapi.sdk.model;

/**
 * Enumerated wire values used by Luminal Open API model fields.
 *
 * <p>Model record components intentionally remain {@link String} or {@link Integer} so a newer server value can still
 * be deserialized before this SDK adds the corresponding enum constant.</p>
 */
public final class OpenApiEnums {

    private OpenApiEnums() {
    }

    /**
     * Supported wallet and transaction currencies. The enum name is the API wire value.
     */
    public enum CurrencyCode {
        /**
         * United States dollar.
         */
        USD,
        /**
         * Hong Kong dollar.
         */
        HKD,
        /**
         * Euro.
         */
        EUR
    }

    /**
     * Wallet availability status. The enum name is the API wire value.
     */
    public enum WalletStatus {
        /**
         * Wallet is available.
         */
        ACTIVE,
        /**
         * Wallet is disabled.
         */
        DISABLED
    }

    /**
     * Card product type. The enum name is the API wire value.
     */
    public enum CardType {
        /**
         * Independently funded rechargeable card.
         */
        RECHARGE,
        /**
         * Card funded by a shared account.
         */
        SHARED
    }

    /**
     * Card lifecycle status. The enum name is the API wire value.
     *
     * <p>The server wire value is {@code UNACTIVE}, not {@code UN_ACTIVE}.</p>
     */
    public enum CardStatus {
        /**
         * Not activated.
         */
        UNACTIVE,
        /**
         * Activation is in progress.
         */
        APPLYING,
        /**
         * Activated and available.
         */
        ACTIVE,
        /**
         * Frozen.
         */
        FREEZE,
        /**
         * Freeze is in progress.
         */
        PRE_FREEZE,
        /**
         * Unfreeze is in progress.
         */
        PRE_UNFREEZE,
        /**
         * Frozen by risk control.
         */
        RISK_FREEZE,
        /**
         * Frozen by an administrator.
         */
        ADMIN_FREEZE,
        /**
         * Cancellation is in progress.
         */
        PRE_CANCEL,
        /**
         * Canceled.
         */
        CANCEL,
        /**
         * Canceled by risk control.
         */
        RISK_CANCEL,
        /**
         * Canceled by an administrator.
         */
        ADMIN_CANCEL,
        /**
         * Expired.
         */
        EXPIRED
    }

    /**
     * Card network or organization. The enum name is the API wire value.
     */
    public enum CardOrganization {
        /**
         * Visa.
         */
        VISA,
        /**
         * Mastercard.
         */
        MASTER_CARD,
        /**
         * Diners Club; the server wire value is {@code DINNERS}.
         */
        DINNERS,
        /**
         * American Express.
         */
        AMEX,
        /**
         * Japan Credit Bureau.
         */
        JCB,
        /**
         * Discover.
         */
        DISCOVER
    }

    /**
     * Shared-account lifecycle status. The enum name is the API wire value.
     */
    public enum SharedAccountStatus {
        /**
         * Activation is in progress.
         */
        APPLYING,
        /**
         * Activated and available.
         */
        ACTIVE,
        /**
         * Frozen.
         */
        FREEZE,
        /**
         * Freeze is in progress.
         */
        PRE_FREEZE,
        /**
         * Unfreeze is in progress.
         */
        PRE_UNFREEZE,
        /**
         * Frozen by risk control.
         */
        RISK_FREEZE,
        /**
         * Frozen by an administrator.
         */
        ADMIN_FREEZE,
        /**
         * Cancellation is in progress.
         */
        PRE_CANCEL,
        /**
         * Canceled.
         */
        CANCEL,
        /**
         * Canceled by risk control.
         */
        RISK_CANCEL,
        /**
         * Canceled by an administrator.
         */
        ADMIN_CANCEL
    }

    /**
     * Integer capability flag returned by shared-account models.
     */
    public enum AvailabilityFlag {
        /**
         * Operation is unavailable.
         */
        NO(0),
        /**
         * Operation is available.
         */
        YES(1);

        private final int value;

        AvailabilityFlag(int value) {
            this.value = value;
        }

        /**
         * Returns the API wire value.
         */
        public int value() {
            return value;
        }
    }

    /**
     * Shared-account transaction type. The enum name is the public API wire value; {@link #code()} is the internal
     * server code shown in API documentation.
     */
    public enum SharedAccountTransactionType {
        /**
         * Account inbound transfer.
         */
        DEPOSIT(101),
        /**
         * Account outbound transfer.
         */
        WITHDRAW(102),
        /**
         * Shared-account adjustment.
         */
        SHARED_ACCOUNT_ADJUST(103),
        /**
         * Card authorization transaction.
         */
        CARD_TRANSACTION(1);

        private final int code;

        SharedAccountTransactionType(int code) {
            this.code = code;
        }

        /**
         * Returns the corresponding internal server code.
         */
        public int code() {
            return code;
        }
    }

    /**
     * Card or shared-account transaction status. The enum name is the API wire value.
     */
    public enum TradeStatus {
        /**
         * Transaction succeeded.
         */
        SUCCESS,
        /**
         * Transaction failed.
         */
        FAIL,
        /**
         * Transaction is processing.
         */
        PROCESSING,
        /**
         * Transaction is pending processing.
         */
        PENDING,
        /**
         * Refund is pending.
         */
        REFUND_PENDING,
        /**
         * Transaction was refunded.
         */
        REFUND
    }

    /**
     * Card transaction classification. The enum name is the API wire value.
     */
    public enum MemberTradeType {
        /**
         * Authorization transaction.
         */
        AUTH,
        /**
         * Pre-authorization or card verification.
         */
        AUTH_VERIFY,
        /**
         * Authorization reversal.
         */
        AUTH_REVOKE,
        /**
         * Authorization refund.
         */
        AUTH_REFUND,
        /**
         * Corrective authorization.
         */
        AUTH_CORRECTIVE,
        /**
         * Refund reversal.
         */
        AUTH_REFUND_REVERSAL,
        /**
         * Dispute or chargeback.
         */
        DISPUTED_REFUSAL
    }

    /**
     * Direction code used by card and shared-account transactions.
     */
    public enum TransactionDirection {
        /**
         * Transfer in.
         */
        TRANSFER_IN(1),
        /**
         * Transfer out.
         */
        TRANSFER_OUT(2);

        private final int code;

        TransactionDirection(int code) {
            this.code = code;
        }

        /**
         * Returns the API wire value.
         */
        public int code() {
            return code;
        }
    }

    /**
     * Asynchronous transaction processing status. The enum name is the API wire value.
     */
    public enum ProcessStatus {
        /**
         * Waiting to be processed.
         */
        PENDING,
        /**
         * Processing is in progress.
         */
        PROCESSING,
        /**
         * Processing succeeded; final status.
         */
        SUCCESS,
        /**
         * Processing failed; final status.
         */
        FAIL
    }

    /**
     * Shared-account opening result status. The enum name is the webhook wire value.
     */
    public enum SharedAccountOpenStatus {
        /**
         * Opening succeeded.
         */
        SUCCESS,
        /**
         * Opening failed.
         */
        FAIL,
        /**
         * Opening is in progress.
         */
        PROCESSING
    }

    /**
     * Numeric wallet-transaction type returned by the wallet transaction API.
     */
    public enum WalletTransactionType {
        /**
         * Account deposit.
         */
        DEPOSIT(101),
        /**
         * Account withdrawal.
         */
        WITHDRAW(102),
        /**
         * Wallet adjustment.
         */
        WALLET_ADJUST(103),
        /**
         * Wallet freeze.
         */
        FREEZE(201),
        /**
         * Wallet unfreeze.
         */
        UNFREEZE(202),
        /**
         * Card opening fee.
         */
        APPLY_CARD_FEE(1),
        /**
         * Card inbound transfer.
         */
        CARD_RECHARGE(2),
        /**
         * Card recharge fee.
         */
        CARD_RECHARGE_FEE(3),
        /**
         * Card reversal fee.
         */
        CARD_REVOKE_FEE(4),
        /**
         * Small-amount fee.
         */
        CARD_MIN_AMOUNT_FEE(5),
        /**
         * Card authorization fee.
         */
        CARD_AUTH_FEE(6),
        /**
         * Cross-border transaction fee.
         */
        CARD_CROSS_BORDER_FEE(7),
        /**
         * Card outbound transfer.
         */
        CARD_OUT(8),
        /**
         * Failed transaction reversal.
         */
        REFUND(9),
        /**
         * Card outbound fee.
         */
        CARD_OUT_FEE(10),
        /**
         * Wallet allocation transfer in.
         */
        TRANSFER_IN(11),
        /**
         * Wallet allocation transfer out.
         */
        TRANSFER_OUT(12),
        /**
         * Shared-account opening fee.
         */
        APPLY_ACCOUNT_FEE(13),
        /**
         * Shared-account service fee.
         */
        ACCOUNT_SERVICE_FEE(14),
        /**
         * Shared-account inbound transfer.
         */
        SHARED_ACCOUNT_RECHARGE(15),
        /**
         * Shared-account outbound transfer.
         */
        SHARED_ACCOUNT_REDUCE(16),
        /**
         * Card refund fee.
         */
        CARD_REFUND_FEE(17);

        private final int code;

        WalletTransactionType(int code) {
            this.code = code;
        }

        /**
         * Returns the API wire value.
         */
        public int code() {
            return code;
        }
    }

    /**
     * Numeric wallet-transaction direction.
     */
    public enum WalletTransactionDirection {
        /**
         * Transfer into the wallet.
         */
        TRANSFER_INTO(1),
        /**
         * Transfer out of the wallet.
         */
        TRANSFER_OUT(2);

        private final int code;

        WalletTransactionDirection(int code) {
            this.code = code;
        }

        /**
         * Returns the API wire value.
         */
        public int code() {
            return code;
        }
    }

    /**
     * Numeric wallet-transaction status.
     */
    public enum WalletTransactionStatus {
        /**
         * Processing.
         */
        PROCESS(0),
        /**
         * Succeeded.
         */
        SUCCESS(1),
        /**
         * Failed.
         */
        FAIL(2);

        private final int code;

        WalletTransactionStatus(int code) {
            this.code = code;
        }

        /**
         * Returns the API wire value.
         */
        public int code() {
            return code;
        }
    }
}
