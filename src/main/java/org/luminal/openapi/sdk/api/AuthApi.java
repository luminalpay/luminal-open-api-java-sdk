package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.internal.HttpTransport;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;
import org.luminal.openapi.sdk.model.AuthModels.RefreshTokenRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/** Authorization operations exposed by the Luminal Open API. */
public final class AuthApi {

    private static final String PATH = "/open-api/v1/auth";
    private final HttpTransport transport;

    /**
     * Creates the authorization API.
     *
     * @param transport shared HTTP transport
     */
    public AuthApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Obtains an OAuth2 token using HTTP Basic authorization.
     *
     * @param appId non-blank application identifier
     * @param appSecret non-blank application secret
     * @return issued access and refresh token data
     */
    public OAuth2Token getToken(String appId, String appSecret) {
        String credentials = HttpTransport.requireNonBlank(appId, "appId") + ':'
                + HttpTransport.requireNonBlank(appSecret, "appSecret");
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return transport.postPublic(PATH + "/token", null, Map.of("Authorization", authorization),
                JsonSupport.type(OAuth2Token.class));
    }

    /**
     * Exchanges a refresh token for new OAuth2 token data.
     *
     * @param refreshToken non-blank refresh token
     * @return refreshed access and refresh token data
     */
    public OAuth2Token refreshToken(String refreshToken) {
        return transport.postAuthorized(PATH + "/refresh-token",
                new RefreshTokenRequest(HttpTransport.requireNonBlank(refreshToken, "refreshToken")),
                JsonSupport.type(OAuth2Token.class));
    }

    /**
     * Invalidates the configured bearer token.
     *
     * @return {@code true} when the server reports success
     */
    public boolean logout() {
        return transport.postAuthorizedBoolean(PATH + "/logout", null);
    }
}
