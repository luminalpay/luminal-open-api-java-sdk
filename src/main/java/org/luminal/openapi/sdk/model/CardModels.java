package org.luminal.openapi.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Models for card products, issuance, lifecycle actions, limits, and transactions.
 */
public final class CardModels {

    private CardModels() {
    }

    /**
     * Card BIN list filters. The current API supports the {@code SHARED} and {@code RECHARGE} card types.
     *
     * @param pageNo           one-based page number; the server default is {@code 1}
     * @param pageSize         number of records requested per page; the server default is {@code 10}
     * @param cardPoolId       optional card-pool identifier
     * @param cardType         required card product type; this endpoint currently accepts {@link OpenApiEnums.CardType#SHARED}
     * @param cardOrganization optional card network or organization; see {@link OpenApiEnums.CardOrganization}
     * @param cardBin          optional card BIN digits
     * @param areaCode         optional card product area code
     */
    public record CardBinsRequest(
            Integer pageNo,
            Integer pageSize,
            Long cardPoolId,
            String cardType,
            String cardOrganization,
            String cardBin,
            String areaCode) {
        /**
         * Backward-compatible constructor without a card-pool filter.
         */
        public CardBinsRequest(Integer pageNo, Integer pageSize, String cardType, String cardOrganization,
                String cardBin, String areaCode) {
            this(pageNo, pageSize, null, cardType, cardOrganization, cardBin, areaCode);
        }
    }

    /**
     * Card BIN product information.
     *
     * @param cardBinId           card BIN product identifier
     * @param cardPoolId          associated card-pool identifier
     * @param poolName            associated card-pool name
     * @param cardType            card product type; see {@link OpenApiEnums.CardType}
     * @param currencyCode        supported ISO 4217 currency code; see {@link OpenApiEnums.CurrencyCode}
     * @param areaCode            card product area code
     * @param cardBin             card BIN digits
     * @param cardOrganization    card network or organization; see {@link OpenApiEnums.CardOrganization}
     * @param applicableScenarios description of supported usage scenarios
     * @param customCardholder    whether the BIN supports an explicit cardholder on issuance; {@code 1} means yes
     * @param canLimit            whether the BIN supports card-limit configuration; {@code 1} means yes
     */
    public record CardBinResponse(
            Long cardBinId,
            Long cardPoolId,
            String poolName,
            String cardType,
            String currencyCode,
            String areaCode,
            String cardBin,
            String cardOrganization,
            String applicableScenarios,
            Integer customCardholder,
            Integer canLimit) {
        /**
         * Backward-compatible constructor for the previous response shape.
         */
        public CardBinResponse(Long cardBinId, String cardType, String currencyCode, String areaCode,
                String cardBin, String cardOrganization, String applicableScenarios,
                Integer customCardholder, Integer canLimit) {
            this(cardBinId, null, null, cardType, currencyCode, areaCode, cardBin, cardOrganization,
                    applicableScenarios, customCardholder, canLimit);
        }

        @SuppressWarnings("unused")
        public CardBinResponse(Long cardBinId, String cardType, String currencyCode, String areaCode,
                String cardBin, String cardOrganization, String applicableScenarios) {
            this(cardBinId, null, null, cardType, currencyCode, areaCode, cardBin, cardOrganization,
                    applicableScenarios, null, null);
        }
    }

    /**
     * Card issuance parameters. The canonical serialized JSON is signed before submission.
     *
     * @param applyCount            number of cards requested; maximum {@code 20}
     * @param cardBinId             selected card BIN product identifier
     * @param cardGroupId           required card-group identifier matching the selected card type
     * @param cardName              user-visible card name
     * @param cardType              required card product type; see {@link OpenApiEnums.CardType}
     * @param memberSharedAccountId shared account funding a {@code SHARED} card; omitted for {@code RECHARGE}
     * @param dailyLimit            daily spending limit used only for {@link OpenApiEnums.CardType#RECHARGE}
     * @param monthLimit            monthly spending limit used only for {@link OpenApiEnums.CardType#RECHARGE}
     * @param rechargeAmount        initial funding amount for {@code RECHARGE}, or total limit for {@code SHARED}
     * @param cardHolderId          optional cardholder identifier; pass it only when the selected BIN supports it
     */
    public record IssueCardRequest(
            Integer applyCount,
            Long cardBinId,
            Long cardGroupId,
            String cardName,
            String cardType,
            Long memberSharedAccountId,
            BigDecimal dailyLimit,
            BigDecimal monthLimit,
            BigDecimal rechargeAmount,
            Long cardHolderId) {
        /**
         * Backward-compatible constructor for SDK clients created before daily recharge-card limits were exposed.
         */
        public IssueCardRequest(
                Integer applyCount,
                Long cardBinId,
                Long cardGroupId,
                String cardName,
                String cardType,
                Long memberSharedAccountId,
                BigDecimal monthLimit,
                BigDecimal rechargeAmount) {
            this(applyCount, cardBinId, cardGroupId, cardName, cardType, memberSharedAccountId,
                    null, monthLimit, rechargeAmount, null);
        }

        public IssueCardRequest(Integer applyCount, Long cardBinId, Long cardGroupId, String cardName,
                String cardType, Long memberSharedAccountId, BigDecimal dailyLimit,
                BigDecimal monthLimit, BigDecimal rechargeAmount) {
            this(applyCount, cardBinId, cardGroupId, cardName, cardType, memberSharedAccountId,
                    dailyLimit, monthLimit, rechargeAmount, null);
        }
    }

    /**
     * Recharge-card funding parameters.
     */
    public record MemberCardRechargeRequest(Long memberCardId, BigDecimal amount, String remark) {
    }

    /**
     * Recharge-card withdrawal parameters.
     */
    public record MemberCardWithdrawRequest(Long memberCardId, BigDecimal amount, String remark) {
    }

    /**
     * Recharge-card operation-record lookup parameters.
     */
    public record RechargeCardOperationRecordRequest(
            Integer pageNo,
            Integer pageSize,
            Long memberCardOperationRecordId,
            Long memberCardId) {
        public RechargeCardOperationRecordRequest {
            if (memberCardOperationRecordId == null && memberCardId == null) {
                throw new IllegalArgumentException(
                        "memberCardOperationRecordId and memberCardId cannot both be null");
            }
        }

        public RechargeCardOperationRecordRequest(Long memberCardOperationRecordId) {
            this(1, 10, memberCardOperationRecordId, null);
        }
    }

    /**
     * Result of a recharge-card funding, withdrawal, or limit-modification operation.
     *
     * <p>{@code createTime} and {@code updateTime} are Unix epoch timestamps in milliseconds.</p>
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
     * @param createTime                  creation time as a Unix epoch timestamp in milliseconds
     * @param updateTime                  last-update time as a Unix epoch timestamp in milliseconds
     */
    public record RechargeCardOperationRecordResponse(
            Long memberCardOperationRecordId,
            Long memberCardId,
            String cardType,
            String operationType,
            BigDecimal amount,
            String currencyCode,
            BigDecimal balance,
            String status,
            String message,
            Long createTime,
            Long updateTime) {
    }

    /**
     * Issued-card list filters.
     *
     * @param pageNo       one-based page number; the server default is {@code 1}
     * @param pageSize     number of records requested per page; the server default is {@code 10}
     * @param memberCardId optional member-card identifier
     * @param status       optional card status; see {@link OpenApiEnums.CardStatus}. Use {@code UNACTIVE}, not
     *                     {@code UN_ACTIVE}
     * @param cardBin      optional card BIN digits
     * @param cardType     required card product type; see {@link OpenApiEnums.CardType}
     * @param cardKeyWords optional full card number, last four digits, or masked card number
     * @param cardGroups   optional card-group identifiers
     */
    public record MemberCardPageRequest(
            Integer pageNo,
            Integer pageSize,
            Long memberCardId,
            String status,
            String cardBin,
            String cardType,
            String cardKeyWords,
            List<Long> cardGroups) {
        public MemberCardPageRequest {
            cardGroups = cardGroups == null ? null : List.copyOf(cardGroups);
        }
    }

    /**
     * Issued-card information.
     *
     * @param memberCardId          member-card identifier
     * @param memberSharedAccountId funding shared-account identifier
     * @param cardBinId             card BIN product identifier
     * @param cardBin               card BIN digits
     * @param cardType              card product type; see {@link OpenApiEnums.CardType}
     * @param cardNo                card number returned by the server
     * @param cardTailNo            final card-number digits
     * @param currencyCode          ISO 4217 card currency code; see {@link OpenApiEnums.CurrencyCode}
     * @param balance               available card balance
     * @param totalLimit            configured total card limit
     * @param status                current card status; see {@link OpenApiEnums.CardStatus}
     * @param cardholder            cardholder name
     * @param freezeTime            most recent card freeze time
     * @param cancelTime            card cancellation time
     * @param remark                card remark
     * @param cardGroupId           assigned card-group identifier
     * @param cardGroupName         assigned card-group name
     * @param createTime            card creation time represented as Unix epoch milliseconds
     */
    public record MemberCardResponse(
            Long memberCardId,
            Long memberSharedAccountId,
            Long cardBinId,
            String cardBin,
            String cardType,
            String cardNo,
            String cardTailNo,
            String currencyCode,
            BigDecimal balance,
            BigDecimal totalLimit,
            BigDecimal dailyLimit,
            BigDecimal monthLimit,
            Integer canLimit,
            String status,
            String cardholder,
            LocalDateTime freezeTime,
            LocalDateTime cancelTime,
            String remark,
            Long cardGroupId,
            String cardGroupName,
            Long createTime) {
    }

    /**
     * Card identifier request used by CVV, limit, freeze, unfreeze, and cancel operations.
     *
     * @param memberCardId target member-card identifier
     */
    public record CardIdRequest(Long memberCardId) {
    }

    /**
     * Sensitive card details returned by the CVV endpoint.
     *
     * @param memberCardId member-card identifier
     * @param cardNo       full card number
     * @param cvv          card verification value
     * @param expiryDate   card expiration date
     */
    public record CardCvvResponse(Long memberCardId, String cardNo, String cvv, String expiryDate) {

        @Override
        public String toString() {
            return "CardCvvResponse[memberCardId=" + memberCardId
                    + ", cardNo=<redacted>, cvv=<redacted>, expiryDate=<redacted>]";
        }
    }

    /**
     * Card-transaction list filters.
     *
     * @param pageNo       one-based page number; the server default is {@code 1}
     * @param pageSize     number of records requested per page; the server default is {@code 10}
     * @param cardType     required card product type; see {@link OpenApiEnums.CardType}
     * @param memberCardId optional member-card identifier
     * @param tradeTime    optional two-value Unix epoch millisecond range: inclusive start then end
     */
    public record CardTransactionsRequest(
            Integer pageNo,
            Integer pageSize,
            String cardType,
            Long memberCardId,
            List<Long> tradeTime) {
        public CardTransactionsRequest {
            tradeTime = tradeTime == null ? null : List.copyOf(tradeTime);
        }
    }

    /**
     * Card-transaction information.
     *
     * @param memberCardTransactionId    recharge-card transaction identifier
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
     * @param tradeType                  card transaction classification; see {@link OpenApiEnums.MemberTradeType}
     * @param direction                  balance direction code; see {@link OpenApiEnums.TransactionDirection}
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
    public record CardTransactionResponse(
            Long memberCardTransactionId,
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
            Integer direction,
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
         * Backward-compatible constructor for the original shared-card response shape.
         */
        public CardTransactionResponse(
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
                Integer direction,
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
            this(null, sharedAccountTransactionId, memberSharedAccountId, memberCardId, maskCardNo,
                    orderNo, originalOrderNo, accountBalance, balance, beforeBalance, beforeAccountBalance,
                    status, null, null, type, tradeType, direction, description, tradeActualAmount, currencyCode,
                    tradeCurrencyCode, tradeAmount, tradeTime, merchantName, merchantId, merchantCountry,
                    cardBin, merchantCity, merchantMcc, processStatus);
        }
    }

    /**
     * Current card limit information.
     *
     * @param memberCardId member-card identifier
     * @param totalLimit   configured total card limit
     * @param balance      available card balance
     */
    public record CardLimitResponse(Long memberCardId, BigDecimal totalLimit, BigDecimal balance) {
    }

    /**
     * Card-limit update parameters. The server infers the card type from the member-card identifier;
     * {@code cardType} is retained only for source compatibility with earlier SDK snapshots and is never sent.
     *
     * @param memberCardId target member-card identifier
     * @param cardType     legacy card-type argument, ignored on the wire
     * @param dailyLimit   new daily limit for recharge cards
     * @param monthLimit   new monthly limit for recharge cards
     * @param totalLimit   new total limit for shared cards
     */
    public record CardLimitUpdateRequest(
            Long memberCardId,
            @JsonIgnore String cardType,
            BigDecimal dailyLimit,
            BigDecimal monthLimit,
            BigDecimal totalLimit) {
        /**
         * Creates a request using the current server contract without a card-type argument.
         */
        public CardLimitUpdateRequest(Long memberCardId, BigDecimal dailyLimit,
                BigDecimal monthLimit, BigDecimal totalLimit) {
            this(memberCardId, null, dailyLimit, monthLimit, totalLimit);
        }

        public CardLimitUpdateRequest(Long memberCardId, BigDecimal totalLimit) {
            this(memberCardId, null, null, null, totalLimit);
        }
    }

    /**
     * Card issuance task lookup parameters.
     *
     * @param taskId card issuance task identifier
     */
    public record IssueCardDetailsRequest(Long taskId) {
    }

    /**
     * Result for one card in an issuance task.
     *
     * @param memberCardId issued member-card identifier
     * @param cardStatus   resulting card status; see {@link OpenApiEnums.CardStatus}
     * @param message      failure reason when card issuance does not succeed
     */
    public record IssueCardDetailsResponse(Long memberCardId, String cardStatus, String message) {
    }
}
