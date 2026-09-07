package org.luminal.openapi.sdk;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientConfigurationTest extends ApiTestSupport {

    @Test
    void baseUrlPreservesGatewayContextPath() throws Exception {
        respondData("true");
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/gateway";
        LuminalOpenApiClient gatewayClient = new LuminalOpenApiClient(baseUrl, "test-token");

        gatewayClient.cards().freeze(new CardIdRequest(5L));

        assertEquals("/gateway/open-api/v1/cards/freeze", request().path());
    }

    @Test
    void baseUrlRejectsQueryString() {
        assertThrows(IllegalArgumentException.class,
                () -> new LuminalOpenApiClient("https://api.example.com?tenant=1"));
    }

    @Test
    void baseUrlRejectsFragment() {
        assertThrows(IllegalArgumentException.class,
                () -> new LuminalOpenApiClient("https://api.example.com#open-api"));
    }

    @Test
    void exposesCardholderApi() {
        assertNotNull(client.cardHolders());
    }
}
