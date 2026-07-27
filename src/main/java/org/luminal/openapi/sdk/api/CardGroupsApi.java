package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupCreateRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupDeleteRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupResponse;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupUpdateRequest;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

import java.util.Objects;

/** Card-group operations exposed by the Luminal Open API. */
public final class CardGroupsApi {

    private static final String PATH = "/open-api/v1/cards/group";
    private final HttpTransport transport;

    /**
     * Creates the card-group API.
     *
     * @param transport shared HTTP transport
     */
    public CardGroupsApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Lists card groups belonging to the current member.
     *
     * @param request page and card-type filters
     * @return matching card groups and page metadata
     */
    public PageResultEx<CardGroupResponse, Object> list(CardGroupRequest request) {
        return transport.postAuthorized(PATH, Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResultEx.class, CardGroupResponse.class, Object.class));
    }

    /**
     * Creates a card group.
     *
     * @param request new group name and card type
     * @return created card-group data
     */
    public CardGroupResponse create(CardGroupCreateRequest request) {
        return transport.postAuthorized(PATH + "/create", Objects.requireNonNull(request, "request"),
                JsonSupport.type(CardGroupResponse.class));
    }

    /**
     * Updates a card-group name.
     *
     * @param request target group identifier and new name
     * @return {@code true} when the server reports success
     */
    public boolean update(CardGroupUpdateRequest request) {
        return transport.postAuthorizedBoolean(PATH + "/update", Objects.requireNonNull(request, "request"));
    }

    /**
     * Deletes a card group.
     *
     * @param request target group identifier
     * @return {@code true} when the server reports success
     */
    public boolean delete(CardGroupDeleteRequest request) {
        return transport.postAuthorizedBoolean(PATH + "/delete", Objects.requireNonNull(request, "request"));
    }
}
