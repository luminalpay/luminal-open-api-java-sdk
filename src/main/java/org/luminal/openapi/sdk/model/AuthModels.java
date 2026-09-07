package org.luminal.openapi.sdk.model;

/**
 * Models for obtaining and refreshing Luminal OAuth2 bearer tokens.
 */
public final class AuthModels {

    private AuthModels() {
    }

    /**
     * OAuth2 access and refresh token data.
     *
     * @param accessToken  bearer access token
     * @param tokenType    OAuth2 token type, usually {@code Bearer}
     * @param expiresTime  access-token expiration instant as Unix epoch milliseconds
     * @param refreshToken token used to obtain new access-token data
     * @param scope        space-delimited granted OAuth2 scopes; see {@link OpenApiEnums.OpenApiScope#code()}
     * @param jti          unique token identifier
     */
    public record OAuth2Token(
            String accessToken,
            String tokenType,
            Long expiresTime,
            String refreshToken,
            String scope,
            String jti) {

        @Override
        public String toString() {
            return "OAuth2Token[accessToken=<redacted>, tokenType=" + tokenType + ", expiresTime=" + expiresTime
                    + ", refreshToken=<redacted>, scope=" + scope + ", jti=" + jti + ']';
        }
    }

    /**
     * Request for a new access token using a refresh token.
     *
     * @param refreshToken refresh token issued by the authorization API
     */
    public record RefreshTokenRequest(String refreshToken) {

        @Override
        public String toString() {
            return "RefreshTokenRequest[refreshToken=<redacted>]";
        }
    }
}
