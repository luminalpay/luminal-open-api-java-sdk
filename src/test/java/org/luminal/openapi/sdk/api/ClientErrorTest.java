package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.LuminalApiException;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientErrorTest extends ApiTestSupport {

    @Test
    void nonSuccessHttpStatusPreservesStatusAndBody() throws Exception {
        respond(503, "temporarily unavailable");

        LuminalApiException exception = assertThrows(LuminalApiException.class,
                () -> client.cards().freeze(new CardIdRequest(5L)));

        assertEquals(503, exception.httpStatus());
        assertEquals("temporarily unavailable", exception.responseBody());
        assertBearerPost("/open-api/v1/cards/freeze", "\"memberCardId\":5");
    }

    @Test
    void businessErrorPreservesApiCodeAndMessage() throws Exception {
        respond(200, "{\"code\":400100,\"msg\":\"Card is already frozen\",\"data\":null}");

        LuminalApiException exception = assertThrows(LuminalApiException.class,
                () -> client.cards().freeze(new CardIdRequest(5L)));

        assertEquals(200, exception.httpStatus());
        assertEquals(400100, exception.apiCode());
        assertEquals("Card is already frozen", exception.getMessage());
        assertBearerPost("/open-api/v1/cards/freeze", "\"memberCardId\":5");
    }

    @Test
    void oversizedResponseIsRejected() throws Exception {
        respond(200, "x".repeat((1 << 20) + 1));

        LuminalApiException exception = assertThrows(LuminalApiException.class,
                () -> client.cards().freeze(new CardIdRequest(5L)));

        assertEquals(200, exception.httpStatus());
        assertTrue(exception.getMessage().contains("exceeds 1048576 bytes"));
    }
}
