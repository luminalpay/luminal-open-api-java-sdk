package org.luminal.openapi.sdk.model;

import java.math.BigDecimal;

/**
 * Models for listing Luminal wallet accounts and their available or frozen balances.
 */
public final class AccountModels {

    private AccountModels() {
    }

    /**
     * Wallet-account list filters.
     *
     * @param pageNo   one-based page number; the server default is {@code 1}
     * @param pageSize number of records requested per page; the server default is {@code 10}
     * @param currency optional ISO 4217 currency code; current values are defined by
     *                 {@link OpenApiEnums.CurrencyCode}
     */
    public record WalletInfoRequest(Integer pageNo, Integer pageSize, String currency) {
    }

    /**
     * Wallet-account information.
     *
     * @param balance       available wallet balance
     * @param currency      ISO 4217 currency code; current values are defined by {@link OpenApiEnums.CurrencyCode}
     * @param frozenBalance wallet balance unavailable for spending
     * @param memberNo      member identifier assigned by Luminal
     * @param walletNo      wallet identifier assigned by Luminal
     * @param walletStatus  current wallet status; see {@link OpenApiEnums.WalletStatus}
     */
    public record WalletInfoResponse(
            BigDecimal balance,
            String currency,
            BigDecimal frozenBalance,
            String memberNo,
            String walletNo,
            String walletStatus) {
    }
}
