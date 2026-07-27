package org.luminal.openapi.sdk.model;

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
     * Card BIN list filters. The current API supports the {@code SHARED} card type.
     *
     * @param pageNo           one-based page number; the server default is {@code 1}
     * @param pageSize         number of records requested per page; the server default is {@code 10}
     * @param cardType         required card product type; this endpoint currently accepts {@link OpenApiEnums.CardType#SHARED}
     * @param cardOrganization optional card network or organization; see {@link OpenApiEnums.CardOrganization}
     * @param cardBin          optional card BIN digits
     * @param areaCode         optional card product area code
     */
    public record CardBinsRequest(
            Integer pageNo,
            Integer pageSize,
            String cardType,
            String cardOrganization,
            String cardBin,
            String areaCode) {
    }

    /**
     * Card BIN product information.
     *
     * @param cardBinId           card BIN product identifier
     * @param cardType            card product type; see {@link OpenApiEnums.CardType}
     * @param currencyCode        supported ISO 4217 currency code; see {@link OpenApiEnums.CurrencyCode}
     * @param areaCode            card product area code
     * @param cardBin             card BIN digits
     * @param cardOrganization    card network or organization; see {@link OpenApiEnums.CardOrganization}
     * @param applicableScenarios description of supported usage scenarios
     */
    public record CardBinResponse(
            Long cardBinId,
            String cardType,
            String currencyCode,
            String areaCode,
            String cardBin,
            String cardOrganization,
            String applicableScenarios) {
    }

    /**
     * Card issuance parameters. The canonical serialized JSON is signed before submission.
     *
     * @param applyCount            number of cards requested; maximum {@code 20}
     * @param cardBinId             selected card BIN product identifier
     * @param cardGroupId           required card-group identifier matching the selected card type
     * @param cardName              user-visible card name
     * @param cardType              required card product type; see {@link OpenApiEnums.CardType}
     * @param memberSharedAccountId required shared account funding the card
     * @param monthLimit            monthly spending limit used only for {@link OpenApiEnums.CardType#RECHARGE}
     * @param rechargeAmount        initial funding amount for {@code RECHARGE}, or total limit for {@code SHARED}
     */
    public record IssueCardRequest(
            Integer applyCount,
            Long cardBinId,
            Long cardGroupId,
            String cardName,
            String cardType,
            Long memberSharedAccountId,
            BigDecimal monthLimit,
            BigDecimal rechargeAmount) {
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
     * Card-limit update parameters.
     *
     * @param memberCardId target member-card identifier
     * @param totalLimit   new total card limit
     */
    public record CardLimitUpdateRequest(Long memberCardId, BigDecimal totalLimit) {
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
