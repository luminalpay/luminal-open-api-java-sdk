package org.luminal.openapi.sdk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Models for shared-account opening, balances, capabilities, and transaction history.
 */
public final class SharedAccountModels {

    private SharedAccountModels() {
    }

    /**
     * Shared-account creation parameters.
     *
     * @param cardBinId      card BIN product identifier; at least one selector is required
     * @param cardPoolId     card-pool identifier; at least one selector is required
     * @param rechargeAmount initial account funding amount; minimum {@code 0.01}
     * @param accountName    user-visible shared-account name
     */
    public record CreateSharedAccountRequest(
            Long cardBinId,
            Long cardPoolId,
            BigDecimal rechargeAmount,
            String accountName) {
        public CreateSharedAccountRequest {
            if (cardBinId == null && cardPoolId == null) {
                throw new IllegalArgumentException("At least one of cardBinId or cardPoolId must be provided");
            }
        }

        /**
         * Backward-compatible constructor without a card-pool association.
         */
        public CreateSharedAccountRequest(Long cardBinId, BigDecimal rechargeAmount, String accountName) {
            this(cardBinId, null, rechargeAmount, accountName);
        }
    }

    /**
     * Newly created shared-account identifier.
     *
     * @param memberSharedAccountId member shared-account identifier
     */
    public record SharedAccountIdResponse(Long memberSharedAccountId) {
    }

    /**
     * Shared-account list filters.
     *
     * @param pageNo                one-based page number; the server default is {@code 1}
     * @param pageSize              number of records requested per page; the server default is {@code 10}
     * @param memberSharedAccountId optional shared-account identifier
     * @param accountName           optional shared-account name filter
     * @param cardPoolId            optional card-pool identifier
     */
    public record SharedAccountPageRequest(
            Integer pageNo,
            Integer pageSize,
            Long memberSharedAccountId,
            String accountName,
            Long cardPoolId) {
        /**
         * Backward-compatible constructor without a card-pool filter.
         */
        public SharedAccountPageRequest(Integer pageNo, Integer pageSize, Long memberSharedAccountId,
                String accountName) {
            this(pageNo, pageSize, memberSharedAccountId, accountName, null);
        }
    }

    /**
     * Shared-account information and supported operations.
     *
     * @param memberSharedAccountId   member shared-account identifier
     * @param accountName             user-visible shared-account name
     * @param status                  current shared-account status; see {@link OpenApiEnums.SharedAccountStatus}
     * @param createTime              shared-account creation time
     * @param cardBin                 card BIN digits
     * @param cardBinId               card BIN product identifier
     * @param cardPoolId              associated card-pool identifier
     * @param poolName                associated card-pool name
     * @param cardOrganization        card network or organization; see {@link OpenApiEnums.CardOrganization}
     * @param balance                 available shared-account balance
     * @param issuedCardCount         number of cards issued from this account
     * @param remainingApplyCardCount number of additional cards that may be requested
     * @param canRecharge             deposit capability; see {@link OpenApiEnums.AvailabilityFlag}
     * @param canApply                card-issuance capability; see {@link OpenApiEnums.AvailabilityFlag}
     * @param applyHandlingFee        handling fee charged for card issuance
     * @param canReduce               withdrawal capability; see {@link OpenApiEnums.AvailabilityFlag}
     * @param canFreeze               freeze capability; see {@link OpenApiEnums.AvailabilityFlag}
     * @param canUnfreeze             unfreeze capability; see {@link OpenApiEnums.AvailabilityFlag}
     * @param canCancel               cancellation capability; see {@link OpenApiEnums.AvailabilityFlag}
     */
    public record SharedAccountResponse(
            Long memberSharedAccountId,
            String accountName,
            String status,
            LocalDateTime createTime,
            String cardBin,
            Long cardBinId,
            Long cardPoolId,
            String poolName,
            String cardOrganization,
            BigDecimal balance,
            Long issuedCardCount,
            Long remainingApplyCardCount,
            Integer canRecharge,
            Integer canApply,
            BigDecimal applyHandlingFee,
            Integer canReduce,
            Integer canFreeze,
            Integer canUnfreeze,
            Integer canCancel) {
        /**
         * Backward-compatible constructor for the previous response shape.
         */
        public SharedAccountResponse(
                Long memberSharedAccountId,
                String accountName,
                String status,
                LocalDateTime createTime,
                String cardBin,
                Long cardBinId,
                String cardOrganization,
                BigDecimal balance,
                Long issuedCardCount,
                Long remainingApplyCardCount,
                Integer canRecharge,
                Integer canApply,
                BigDecimal applyHandlingFee,
                Integer canReduce,
                Integer canFreeze,
                Integer canUnfreeze,
                Integer canCancel) {
            this(memberSharedAccountId, accountName, status, createTime, cardBin, cardBinId,
                    null, null, cardOrganization, balance, issuedCardCount, remainingApplyCardCount,
                    canRecharge, canApply, applyHandlingFee, canReduce, canFreeze, canUnfreeze, canCancel);
        }
    }

    /**
     * Shared-account deposit or withdrawal parameters.
     *
     * @param memberSharedAccountId target shared-account identifier
     * @param amount                positive amount to deposit or withdraw; minimum {@code 0.01}
     */
    public record SharedAccountBalanceRequest(Long memberSharedAccountId, BigDecimal amount) {
    }

    /**
     * Shared-account cancellation parameters.
     *
     * @param memberSharedAccountId target shared-account identifier
     * @param remark                optional cancellation remark
     * @param verifyCode            email or one-time verification code required by the server
     */
    public record SharedAccountCancelRequest(Long memberSharedAccountId, String remark, String verifyCode) {
    }

    /**
     * Identifier of a submitted shared-account transaction.
     *
     * @param sharedAccountTransactionId shared-account transaction identifier
     */
    public record SharedAccountTransactionIdResponse(String sharedAccountTransactionId) {
    }

    /**
     * Shared-account detail lookup parameters.
     *
     * @param memberSharedAccountId target shared-account identifier
     */
    public record SharedAccountGetRequest(Long memberSharedAccountId) {
    }

    /**
     * Shared-account transaction list filters.
     *
     * @param pageNo                     one-based page number; the server default is {@code 1}
     * @param pageSize                   number of records requested per page; the server default is {@code 10}
     * @param sharedAccountTransactionId optional shared-account transaction identifier
     * @param memberSharedAccountId      optional shared-account identifier
     * @param memberCardId               optional member-card identifier
     * @param type                       optional public transaction type name; see {@link OpenApiEnums.SharedAccountTransactionType}
     * @param tradeTime                  optional two-value ISO-8601 local date-time range: inclusive start then end
     */
    public record SharedAccountTransactionsRequest(
            Integer pageNo,
            Integer pageSize,
            Long sharedAccountTransactionId,
            Long memberSharedAccountId,
            Long memberCardId,
            String type,
            List<LocalDateTime> tradeTime) {
        public SharedAccountTransactionsRequest {
            tradeTime = tradeTime == null ? null : List.copyOf(tradeTime);
        }
    }

    /**
     * Shared-account transaction information.
     *
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
    public record SharedAccountTransactionResponse(
            Long sharedAccountTransactionId,
            Long memberSharedAccountId,
            Long memberCardId,
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
         * Backward-compatible constructor for clients using the original transaction response shape.
         */
        public SharedAccountTransactionResponse(
                Long sharedAccountTransactionId,
                Long memberSharedAccountId,
                Long memberCardId,
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
            this(sharedAccountTransactionId, memberSharedAccountId, memberCardId, maskCardNo,
                    orderNo, originalOrderNo, accountBalance, balance, beforeBalance, beforeAccountBalance,
                    status, null, null, type, tradeType, description, tradeActualAmount, currencyCode,
                    tradeCurrencyCode, tradeAmount, tradeTime, merchantName, merchantId, merchantCountry,
                    cardBin, merchantCity, merchantMcc, processStatus);
        }
    }
}
