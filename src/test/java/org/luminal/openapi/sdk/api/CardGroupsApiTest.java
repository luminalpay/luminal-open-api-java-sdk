package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupCreateRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupDeleteRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupResponse;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupUpdateRequest;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardGroupsApiTest extends ApiTestSupport {

    @Test
    void listCallsGroupListEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"cardGroupId\":3,\"cardGroupName\":\"Travel\"}],\"extra\":null}");

        PageResultEx<CardGroupResponse, Object> result =
                client.cardGroups().list(new CardGroupRequest(1, 10, "SHARED"));

        assertEquals("Travel", result.list().get(0).cardGroupName());
        assertBearerPost("/open-api/v1/cards/group", "\"pageNo\":1");
        assertBearerPost("/open-api/v1/cards/group", "\"pageSize\":10");
        assertBearerPost("/open-api/v1/cards/group", "\"cardType\":\"SHARED\"");
    }

    @Test
    void createCallsGroupCreateEndpoint() throws Exception {
        respondData("{\"cardGroupId\":3,\"cardGroupName\":\"Travel\",\"cardType\":\"SHARED\"}");

        CardGroupResponse result = client.cardGroups().create(new CardGroupCreateRequest("Travel", "SHARED"));

        assertEquals(3L, result.cardGroupId());
        assertBearerPost("/open-api/v1/cards/group/create", "\"cardGroupName\":\"Travel\"");
    }

    @Test
    void updateCallsGroupUpdateEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.cardGroups().update(new CardGroupUpdateRequest(3L, "Trips")));
        assertBearerPost("/open-api/v1/cards/group/update", "\"cardGroupName\":\"Trips\"");
    }

    @Test
    void deleteCallsGroupDeleteEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.cardGroups().delete(new CardGroupDeleteRequest(3L)));
        assertBearerPost("/open-api/v1/cards/group/delete", "\"cardGroupId\":3");
    }
}
