package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.ApiTestSupport.CapturedRequest;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthApiTest extends ApiTestSupport {

    @Test
    void getTokenUsesBasicAuthorization() throws Exception {
        respondData("{\"accessToken\":\"access\",\"tokenType\":\"Bearer\",\"expiresTime\":10,\"refreshToken\":\"refresh\"}");

        OAuth2Token token = client.auth().getToken("app", "secret");

        assertEquals("access", token.accessToken());
        CapturedRequest request = request();
        assertEquals("POST", request.method());
        assertEquals("/open-api/v1/auth/token", request.path());
        assertEquals("Basic YXBwOnNlY3JldA==", request.header("Authorization"));
        assertEquals("", request.body());
    }

    @Test
    void refreshTokenSendsBearerTokenAndBody() throws Exception {
        respondData("{\"accessToken\":\"new-access\",\"refreshToken\":\"new-refresh\"}");

        OAuth2Token token = client.auth().refreshToken("refresh");

        assertEquals("new-access", token.accessToken());
        assertBearerPost("/open-api/v1/auth/refresh-token", "\"refreshToken\":\"refresh\"");
    }

    @Test
    void logoutInvalidatesConfiguredToken() throws Exception {
        respondData("true");

        assertTrue(client.auth().logout());
        assertBearerPost("/open-api/v1/auth/logout", null);
        assertEquals("", request().body());
    }
}
