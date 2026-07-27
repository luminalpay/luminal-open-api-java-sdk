package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.CommonModels.PageResult;
import org.luminal.openapi.sdk.model.TransactionModels.WalletTransactionRequest;
import org.luminal.openapi.sdk.model.TransactionModels.WalletTransactionResponse;

import java.util.Objects;

/** Wallet-transaction operations exposed by the Luminal Open API. */
public final class TransactionsApi {

    private static final String PATH = "/open-api/v1/transactions";
    private final HttpTransport transport;

    /**
     * Creates the wallet-transaction API.
     *
     * @param transport shared HTTP transport
     */
    public TransactionsApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Lists wallet transactions belonging to the current member.
     *
     * @param request page, type, time, and card filters
     * @return matching wallet transactions
     */
    public PageResult<WalletTransactionResponse> list(WalletTransactionRequest request) {
        return transport.postAuthorized(PATH + "/list", Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResult.class, WalletTransactionResponse.class));
    }
}
