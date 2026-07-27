package org.luminal.openapi.sdk.internal;

import org.luminal.openapi.sdk.api.AuthApi;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;

import java.util.Objects;

/**
 * Caches and refreshes OAuth2 access tokens.
 */
public final class TokenManager {

    private final AuthApi authApi;
    private final String appId;
    private final String appSecret;
    private volatile TokenState state;

    public TokenManager(AuthApi authApi, String appId, String appSecret) {
        this.authApi = Objects.requireNonNull(authApi, "authApi");
        this.appId = HttpTransport.requireNonBlank(appId, "appId");
        this.appSecret = HttpTransport.requireNonBlank(appSecret, "appSecret");
    }

    public String currentAccessToken() {
        TokenState current = state;
        if (current != null && !current.shouldRefresh()) {
            return current.token().accessToken();
        }
        synchronized (this) {
            current = state;
            if (current == null || current.shouldRefresh()) {
                state = TokenState.from(authApi.getToken(appId, appSecret));
            }
            return state.token().accessToken();
        }
    }

    public void invalidate() {
        state = null;
    }

    public synchronized String refresh() {
        state = TokenState.from(authApi.getToken(appId, appSecret));
        return state.token().accessToken();
    }

    private record TokenState(OAuth2Token token, long fetchedAtMillis, long expiresAtMillis) {
        private static TokenState from(OAuth2Token token) {
            long now = System.currentTimeMillis();
            long expiresAt = normalizeExpiry(token.expiresTime(), now);
            return new TokenState(token, now, expiresAt);
        }

        private boolean shouldRefresh() {
            long ttl = expiresAtMillis - fetchedAtMillis;
            if (ttl <= 0) {
                return true;
            }
            return System.currentTimeMillis() >= fetchedAtMillis + (ttl / 2);
        }

        private static long normalizeExpiry(Long expiresTime, long now) {
            if (expiresTime == null) {
                return now;
            }
            long value = expiresTime;
            return value < 1_000_000_000_000L ? value * 1000L : value;
        }
    }
}
