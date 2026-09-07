package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCardPageRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCardResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCountryResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCreateRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderDetailResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderModifyRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderPageRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderPageResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cardholder management operations exposed by the Luminal Open API.
 */
public final class CardHoldersApi {
    private static final String PATH = "/open-api/v1/card-holders";
    private final HttpTransport transport;

    /**
     * Creates the cardholder API.
     */
    public CardHoldersApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Creates a cardholder and returns its identifier.
     */
    public Long add(CardHolderCreateRequest request) {
        return post("/add", request, Long.class);
    }

    /**
     * Lists active countries and regions available for cardholder information.
     */
    public List<CardHolderCountryResponse> countries() {
        return transport.getAuthorized(PATH + "/countries",
                JsonSupport.parametricType(List.class, CardHolderCountryResponse.class));
    }

    /**
     * Updates all editable fields of a cardholder.
     */
    public void modify(CardHolderModifyRequest request) {
        post("/modify", request, Void.class);
    }

    /**
     * Retrieves cardholder details by identifier.
     */
    public CardHolderDetailResponse detail(Long cardHolderId) {
        return post("/info/" + Objects.requireNonNull(cardHolderId, "cardHolderId"),
                Map.of(), CardHolderDetailResponse.class);
    }

    /**
     * Lists cardholders owned by the current member.
     */
    public PageResult<CardHolderPageResponse> page(CardHolderPageRequest request) {
        return transport.postAuthorized(PATH + "/page", Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResult.class, CardHolderPageResponse.class));
    }

    /**
     * Lists cards associated with a cardholder.
     */
    public PageResult<CardHolderCardResponse> associatedCards(CardHolderCardPageRequest request) {
        return transport.postAuthorized(PATH + "/card/page", Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResult.class, CardHolderCardResponse.class));
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        return transport.postAuthorized(PATH + path, Objects.requireNonNull(request, "request"),
                JsonSupport.type(responseType));
    }
}
