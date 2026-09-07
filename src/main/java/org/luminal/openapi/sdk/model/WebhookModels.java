package org.luminal.openapi.sdk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload models emitted by Luminal webhook events after signature verification.
 */
public final class WebhookModels {

    private WebhookModels() {
    }

    /**
     * One card result inside a card-opening webhook.
     *
     * @param memberCardId processed member-card identifier
     * @param cardStatus   resulting card status; see {@link OpenApiEnums.CardStatus}
     * @param message      failure reason when card issuance does not succeed
     */
    public record CardOpenResult(Long memberCardId, String cardStatus, String message) {
    }

    /**
     * Payload of the {@code CARD_OPEN_STATUS} webhook.
     *
     * @param cardApplyTaskId card issuance task identifier
     * @param memberId        member identifier
     * @param list            individual card processing results
     */
    public record CardOpenStatusWebhook(Long cardApplyTaskId, Long memberId, List<CardOpenResult> list) {
        public CardOpenStatusWebhook {
            list = list == null ? null : List.copyOf(list);
        }
    }

    /**
     * Payload of the {@code CARD_STATUS} webhook.
     *
     * @param memberCardId member-card identifier
     * @param cardStatus   new card status; see {@link OpenApiEnums.CardStatus}
     * @param memberNo     member identifier assigned by Luminal
     * @param updateTime   status update time reported by the webhook
     */
    public record CardStatusWebhook(String memberCardId, String cardStatus, String memberNo, String updateTime) {
    }

    /**
     * Payload of the {@code SHARED_ACCOUNT_OPEN_STATUS} webhook.
     *
     * @param sharedAccountOperationRecordId shared-account opening operation identifier
     * @param memberNo                       member identifier assigned by Luminal
     * @param memberSharedAccountId          created shared-account identifier
     * @param status                         shared-account opening result; see {@link OpenApiEnums.SharedAccountOpenStatus}
     */
    public record SharedAccountOpenStatusWebhook(
            Long sharedAccountOperationRecordId,
            Long memberNo,
            Long memberSharedAccountId,
            String status) {
    }

    /**
     * Payload of card operation-result webhooks, including recharge, withdrawal and limit modification.
     *
     * @param memberCardOperationRecordId operation-record identifier
     * @param memberCardId                member-card identifier
     * @param cardType                    card product type; see {@link OpenApiEnums.CardType}
     * @param operationType               operation type; see {@link OpenApiEnums.RechargeCardOperationType}
     * @param amount                      operation amount, when applicable
     * @param currencyCode                operation currency; see {@link OpenApiEnums.CurrencyCode}
     * @param balance                     card balance after the operation
     * @param status                      operation result; see {@link OpenApiEnums.RechargeCardOperationStatus}
     * @param message                     operation result message
     * @param updateTime                  last-update time reported by the webhook
     */
    public record RechargeCardTransferStatusWebhook(
            Long memberCardOperationRecordId,
            Long memberCardId,
            String cardType,
            String operationType,
            BigDecimal amount,
            String currencyCode,
            BigDecimal balance,
            String status,
            String message,
            LocalDateTime updateTime) {
    }

    /**
     * Payload shared by the {@code CARD_TRANSACTIONS} and {@code SHARE_ACCOUNT_FUND_TRANSACTIONS} webhooks. Both
     * events use the same server message schema; fields unrelated to a specific transaction may be absent.
     *
     * @param memberCardTransactionId    recharge-card transaction identifier
     * @param cardType                   card product type
     * @param sharedAccountTransactionId shared-account transaction identifier
     * @param memberSharedAccountId      related shared-account identifier
     * @param memberCardId               related member-card identifier
     * @param maskCardNo                 masked card number
     * @param orderNo                    business-order identifier
     * @param originalOrderNo            original business-order identifier for related operations
     * @param accountBalance             shared-account balance reported for the transaction
     * @param balance                    card balance reported for the transaction
     * @param beforeBalance              card balance before the transaction
     * @param beforeAccountBalance       shared-account balance before the transaction
     * @param status                     transaction status; see {@link OpenApiEnums.TradeStatus}
     * @param settleStatus               local settlement status; see {@link OpenApiEnums.SettleStatus}
     * @param settleTime                 local settlement time; absent when the transaction is not settled
     * @param type                       public transaction type name; see {@link OpenApiEnums.SharedAccountTransactionType}
     * @param tradeType                  card transaction classification when applicable; see {@link OpenApiEnums.MemberTradeType}
     * @param direction                  string-encoded balance direction code; see {@link OpenApiEnums.TransactionDirection}
     * @param description                transaction description
     * @param tradeActualAmount          settled amount
     * @param currencyCode               ISO 4217 card settlement currency code
     * @param tradeCurrencyCode          ISO 4217 original transaction currency code
     * @param tradeAmount                original trade amount
     * @param tradeTime                  transaction time
     * @param merchantName               merchant name
     * @param merchantId                 merchant identifier
     * @param merchantCountry            merchant country code
     * @param cardBin                    card BIN digits
     * @param merchantCity               merchant city
     * @param merchantMcc                merchant category code
     * @param processStatus              asynchronous processing status; see {@link OpenApiEnums.ProcessStatus}. Final values are
     *                                   {@code SUCCESS} and {@code FAIL}
     */
    public record TransactionWebhook(
            String memberCardTransactionId,
            String cardType,
            String sharedAccountTransactionId,
            String memberSharedAccountId,
            String memberCardId,
            String maskCardNo,
            String orderNo,
            String originalOrderNo,
            BigDecimal accountBalance,
            BigDecimal balance,
            BigDecimal beforeBalance,
            BigDecimal beforeAccountBalance,
            String status,
            String settleStatus,
            LocalDateTime settleTime,
            String type,
            String tradeType,
            String direction,
            String description,
            BigDecimal tradeActualAmount,
            String currencyCode,
            String tradeCurrencyCode,
            BigDecimal tradeAmount,
            LocalDateTime tradeTime,
            String merchantName,
            String merchantId,
            String merchantCountry,
            String cardBin,
            String merchantCity,
            String merchantMcc,
            String processStatus) {
        /**
         * Backward-compatible constructor for the original shared-card webhook shape.
         */
        public TransactionWebhook(
                String sharedAccountTransactionId,
                String memberSharedAccountId,
                String memberCardId,
                String maskCardNo,
                String orderNo,
                String originalOrderNo,
                BigDecimal accountBalance,
                BigDecimal balance,
                BigDecimal beforeBalance,
                BigDecimal beforeAccountBalance,
                String status,
                String type,
                String tradeType,
                String direction,
                String description,
                BigDecimal tradeActualAmount,
                String currencyCode,
                String tradeCurrencyCode,
                BigDecimal tradeAmount,
                LocalDateTime tradeTime,
                String merchantName,
                String merchantId,
                String merchantCountry,
                String cardBin,
                String merchantCity,
                String merchantMcc,
                String processStatus) {
            this(null, null, sharedAccountTransactionId, memberSharedAccountId, memberCardId,
                    maskCardNo, orderNo, originalOrderNo, accountBalance, balance,
                    beforeBalance, beforeAccountBalance, status, null, null, type, tradeType, direction, description,
                    tradeActualAmount, currencyCode, tradeCurrencyCode, tradeAmount, tradeTime,
                    merchantName, merchantId, merchantCountry, cardBin, merchantCity, merchantMcc, processStatus);
        }
    }
}
