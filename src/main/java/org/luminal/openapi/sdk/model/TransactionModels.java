package org.luminal.openapi.sdk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Models for querying wallet-ledger transactions, including card and shared-account fees.
 */
public final class TransactionModels {

    private TransactionModels() {
    }

    /**
     * Wallet-transaction list filters.
     *
     * @param pageNo       one-based page number; the server default is {@code 1}
     * @param pageSize     number of records requested per page; the server default is {@code 10}
     * @param type         optional numeric transaction type; see {@link OpenApiEnums.WalletTransactionType}
     * @param createTime   optional two-value ISO-8601 local date-time range: inclusive start then end
     * @param memberCardId optional member-card identifier
     */
    public record WalletTransactionRequest(
            Integer pageNo,
            Integer pageSize,
            Integer type,
            List<LocalDateTime> createTime,
            Long memberCardId) {
        public WalletTransactionRequest {
            createTime = createTime == null ? null : List.copyOf(createTime);
        }
    }

    /**
     * Wallet-transaction information.
     *
     * @param transactionNo wallet-transaction identifier
     * @param memberNo      member identifier assigned by Luminal
     * @param walletNo      wallet identifier assigned by Luminal
     * @param orderNo       related business-order identifier
     * @param type          numeric transaction type; see {@link OpenApiEnums.WalletTransactionType}
     * @param direction     numeric balance direction; see {@link OpenApiEnums.WalletTransactionDirection}
     * @param amount        transaction amount
     * @param fee           transaction fee
     * @param currency      ISO 4217 currency code; current values are defined by {@link OpenApiEnums.CurrencyCode}
     * @param beforeBalance wallet balance before the transaction
     * @param afterBalance  wallet balance after the transaction
     * @param status        numeric transaction status; see {@link OpenApiEnums.WalletTransactionStatus}
     * @param remark        transaction remark
     * @param createTime    transaction creation time
     * @param memberCardId  related member-card identifier
     * @param cardNumber    related masked or display card number returned by the server
     */
    public record WalletTransactionResponse(
            Long transactionNo,
            Long memberNo,
            Long walletNo,
            String orderNo,
            Integer type,
            Integer direction,
            BigDecimal amount,
            BigDecimal fee,
            String currency,
            BigDecimal beforeBalance,
            BigDecimal afterBalance,
            Integer status,
            String remark,
            LocalDateTime createTime,
            Long memberCardId,
            String cardNumber) {
    }
}
