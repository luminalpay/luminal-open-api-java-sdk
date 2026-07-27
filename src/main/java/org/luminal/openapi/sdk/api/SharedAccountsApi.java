package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;
import org.luminal.openapi.sdk.model.SharedAccountModels.CreateSharedAccountRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountBalanceRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountGetRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountIdResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountPageRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionIdResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionsRequest;

import java.util.Objects;

/** Shared-account operations exposed by the Luminal Open API. */
public final class SharedAccountsApi {

    private static final String PATH = "/open-api/v1/shared-account";
    private final HttpTransport transport;

    /**
     * Creates the shared-account API.
     *
     * @param transport shared HTTP transport
     */
    public SharedAccountsApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Creates and initially funds a shared account.
     *
     * @param request card BIN, initial funding amount, and account name
     * @return identifier of the created shared account
     */
    public SharedAccountIdResponse create(CreateSharedAccountRequest request) {
        return post("/create", request, SharedAccountIdResponse.class);
    }

    /**
     * Lists shared accounts belonging to the current member.
     *
     * @param request page, shared-account identifier, and account-name filters
     * @return matching shared accounts and page metadata
     */
    public PageResultEx<SharedAccountResponse, Object> list(SharedAccountPageRequest request) {
        return transport.postAuthorized(PATH + "/list", Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResultEx.class, SharedAccountResponse.class, Object.class));
    }

    /**
     * Deposits funds into a shared account.
     *
     * @param request target shared-account identifier and deposit amount
     * @return identifier of the submitted deposit transaction
     */
    public SharedAccountTransactionIdResponse increase(SharedAccountBalanceRequest request) {
        return post("/increase", request, SharedAccountTransactionIdResponse.class);
    }

    /**
     * Withdraws funds from a shared account.
     *
     * @param request target shared-account identifier and withdrawal amount
     * @return identifier of the submitted withdrawal transaction
     */
    public SharedAccountTransactionIdResponse decrease(SharedAccountBalanceRequest request) {
        return post("/decrease", request, SharedAccountTransactionIdResponse.class);
    }

    /**
     * Retrieves one shared account and its supported operations.
     *
     * @param request target shared-account identifier
     * @return shared-account details and operation flags
     */
    public SharedAccountResponse details(SharedAccountGetRequest request) {
        return post("/details", request, SharedAccountResponse.class);
    }

    /**
     * Lists fund transactions for shared accounts.
     *
     * @param request page, account, card, type, and time filters
     * @return matching shared-account transactions and page metadata
     */
    public PageResultEx<SharedAccountTransactionResponse, Object> transactions(SharedAccountTransactionsRequest request) {
        return transport.postAuthorized(PATH + "/transactions", Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResultEx.class, SharedAccountTransactionResponse.class, Object.class));
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        return transport.postAuthorized(PATH + path, Objects.requireNonNull(request, "request"),
                JsonSupport.type(responseType));
    }
}
