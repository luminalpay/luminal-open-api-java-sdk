package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoRequest;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

import java.util.Objects;

/** Wallet-account operations exposed by the Luminal Open API. */
public final class AccountsApi {

    private static final String PATH = "/open-api/v1/accounts";
    private final HttpTransport transport;

    /**
     * Creates the wallet-account API.
     *
     * @param transport shared HTTP transport
     */
    public AccountsApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Lists wallet accounts belonging to the current member.
     *
     * @param request page and currency filters
     * @return matching wallet accounts and page metadata
     */
    public PageResultEx<WalletInfoResponse, Object> list(WalletInfoRequest request) {
        return transport.postAuthorized(PATH, Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResultEx.class, WalletInfoResponse.class, Object.class));
    }
}
