package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.CardPoolModels.CardPoolRequest;
import org.luminal.openapi.sdk.model.CardPoolModels.CardPoolResponse;

import java.util.List;
import java.util.Objects;

/** Card-pool operations exposed by the Luminal Open API. */
public final class CardPoolsApi {

    private static final String PATH = "/open-api/v1/cards/pools";
    private final HttpTransport transport;

    /**
     * Creates the card-pool API.
     *
     * @param transport shared HTTP transport
     */
    public CardPoolsApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Lists card pools available to the current member.
     *
     * @param request card-pool filters; the returned list can be retained and reused by the caller for a pool-based
     *                shared-account flow
     * @return available card pools
     */
    public List<CardPoolResponse> list(CardPoolRequest request) {
        return transport.postAuthorized(PATH, Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(List.class, CardPoolResponse.class));
    }
}
