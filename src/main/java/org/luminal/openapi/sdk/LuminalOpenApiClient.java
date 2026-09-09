package org.luminal.openapi.sdk;

import org.luminal.openapi.sdk.api.AccountsApi;
import org.luminal.openapi.sdk.api.AuthApi;
import org.luminal.openapi.sdk.api.CardGroupsApi;
import org.luminal.openapi.sdk.api.CardsApi;
import org.luminal.openapi.sdk.api.CardHoldersApi;
import org.luminal.openapi.sdk.api.CardPoolsApi;
import org.luminal.openapi.sdk.api.SharedAccountsApi;
import org.luminal.openapi.sdk.api.TransactionsApi;
import org.luminal.openapi.sdk.internal.TokenManager;
import org.luminal.openapi.sdk.internal.HttpTransport;
import org.slf4j.Logger;
import org.luminal.openapi.sdk.webhook.WebhookEvent;
import org.luminal.openapi.sdk.webhook.WebhookVerifier;

import java.security.PublicKey;

import java.net.CookieManager;
import java.net.http.HttpClient;
import java.util.function.Consumer;

/**
 * Standalone Java 17 facade for the Luminal Open API.
 *
 * <p>The {@code baseUrl} constructor argument configures the URL prefix for every request. It may include a gateway
 * context path, for example {@code https://gateway.example.com/luminal}. Endpoint paths are appended to that prefix.
 * The client has no Spring or Luminal project-module dependency.</p>
 */
public final class LuminalOpenApiClient {

    private final HttpTransport publicTransport;
    private final HttpTransport transport;
    private final AuthApi auth;
    private final AccountsApi accounts;
    private final TransactionsApi transactions;
    private final SharedAccountsApi sharedAccounts;
    private final CardsApi cards;
    private final CardHoldersApi cardHolders;
    private final CardPoolsApi cardPools;
    private final CardGroupsApi cardGroups;
    private final TokenManager tokenManager;
    /**
     * Creates a client without a bearer token.
     *
     * @param baseUrl absolute HTTP or HTTPS request URL prefix; query strings and fragments are not allowed
     */
    public LuminalOpenApiClient(String baseUrl) {
        this(sharedTransport(baseUrl, null, null, null, "en", 0, null),
                sharedTransport(baseUrl, null, null, null, "en", 0, null), null);
    }

    /**
     * Creates a client with a fixed bearer token.
     *
     * @param baseUrl absolute HTTP or HTTPS request URL prefix; query strings and fragments are not allowed
     * @param bearerToken OAuth2 bearer token used by protected endpoints
     */
    public LuminalOpenApiClient(String baseUrl, String bearerToken) {
        this(sharedTransport(baseUrl, null, null, null, "en", 0, null),
                sharedTransport(baseUrl, bearerToken, null, null, "en", 0, null), null);
    }

    /**
     * Creates a client with an optional raw-response observer.
     *
     * @param baseUrl absolute HTTP or HTTPS request URL prefix
     * @param bearerToken optional OAuth2 bearer token
     * @param responseObserver receives each raw response body without formatting
     */
    public LuminalOpenApiClient(String baseUrl, String bearerToken, Consumer<String> responseObserver) {
        this(sharedTransport(baseUrl, null, responseObserver, null, "en", 0, null),
                sharedTransport(baseUrl, bearerToken, responseObserver, null, "en", 0, null), null);
    }

    public LuminalOpenApiClient(String baseUrl, String bearerToken, Consumer<String> responseObserver, boolean httpLoggingEnabled) {
        this(sharedTransport(baseUrl, null, responseObserver, null, "en", 0, null, httpLoggingEnabled),
                sharedTransport(baseUrl, bearerToken, responseObserver, null, "en", 0, null, httpLoggingEnabled),
                null);
    }

    /**
     * Creates a client with an injectable SLF4J logger.
     */
    public LuminalOpenApiClient(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            boolean httpLoggingEnabled, Logger logger) {
        this(sharedTransport(baseUrl, null, responseObserver, null, "en", 0, null, httpLoggingEnabled, logger),
                sharedTransport(baseUrl, bearerToken, responseObserver, null, "en", 0, null, httpLoggingEnabled, logger),
                null);
    }

    public LuminalOpenApiClient(String baseUrl, String appId, String appSecret, Consumer<String> responseObserver,
            int unauthorizedRetryCount) {
        this(baseUrl, appId, appSecret, responseObserver, unauthorizedRetryCount, "en", true);
    }

    public LuminalOpenApiClient(String baseUrl, String appId, String appSecret, Consumer<String> responseObserver,
            int unauthorizedRetryCount, String locale) {
        this(baseUrl, appId, appSecret, responseObserver, unauthorizedRetryCount, locale, true);
    }

    public LuminalOpenApiClient(String baseUrl, String appId, String appSecret, Consumer<String> responseObserver,
            int unauthorizedRetryCount, String locale, boolean httpLoggingEnabled) {
        CookieManager cookieManager = new CookieManager();
        HttpClient httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).connectTimeout(java.time.Duration.ofSeconds(10)).build();
        HttpTransport publicTransport = HttpTransport.create(baseUrl, (String) null,
                responseObserver, null, locale, 0, null, httpClient, httpLoggingEnabled);
        TokenManager tokenManager = new TokenManager(new AuthApi(publicTransport), appId, appSecret);
        this.publicTransport = publicTransport;
        this.transport = HttpTransport.create(baseUrl, tokenManager::currentAccessToken,
                responseObserver, null,
                locale,
                unauthorizedRetryCount, tokenManager::refresh, httpClient, httpLoggingEnabled);
        this.auth = new AuthApi(transport);
        this.accounts = new AccountsApi(transport);
        this.transactions = new TransactionsApi(transport);
        this.sharedAccounts = new SharedAccountsApi(transport);
        this.cards = new CardsApi(transport);
        this.cardHolders = new CardHoldersApi(transport);
        this.cardPools = new CardPoolsApi(transport);
        this.cardGroups = new CardGroupsApi(transport);
        this.tokenManager = tokenManager;
    }

    /**
     * Creates an auto-token client with an injectable SLF4J logger.
     */
    public LuminalOpenApiClient(String baseUrl, String appId, String appSecret, Consumer<String> responseObserver,
            int unauthorizedRetryCount, String locale, boolean httpLoggingEnabled, Logger logger) {
        CookieManager cookieManager = new CookieManager();
        HttpClient httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).connectTimeout(java.time.Duration.ofSeconds(10)).build();
        HttpTransport publicTransport = HttpTransport.create(baseUrl, (String) null,
                responseObserver, null, locale, 0, null, httpClient, httpLoggingEnabled, logger);
        TokenManager tokenManager = new TokenManager(new AuthApi(publicTransport), appId, appSecret);
        this.publicTransport = publicTransport;
        this.transport = HttpTransport.create(baseUrl, tokenManager::currentAccessToken,
                responseObserver, null, locale, unauthorizedRetryCount, tokenManager::refresh, httpClient,
                httpLoggingEnabled, logger);
        this.auth = new AuthApi(transport);
        this.accounts = new AccountsApi(transport);
        this.transactions = new TransactionsApi(transport);
        this.sharedAccounts = new SharedAccountsApi(transport);
        this.cards = new CardsApi(transport);
        this.cardHolders = new CardHoldersApi(transport);
        this.cardPools = new CardPoolsApi(transport);
        this.cardGroups = new CardGroupsApi(transport);
        this.tokenManager = tokenManager;
    }

    private LuminalOpenApiClient(HttpTransport publicTransport, HttpTransport transport, TokenManager tokenManager) {
        this.publicTransport = publicTransport;
        this.transport = transport;
        this.auth = new AuthApi(transport);
        this.accounts = new AccountsApi(transport);
        this.transactions = new TransactionsApi(transport);
        this.sharedAccounts = new SharedAccountsApi(transport);
        this.cards = new CardsApi(transport);
        this.cardHolders = new CardHoldersApi(transport);
        this.cardPools = new CardPoolsApi(transport);
        this.cardGroups = new CardGroupsApi(transport);
        this.tokenManager = tokenManager;
    }

    private static HttpTransport sharedTransport(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction) {
        return sharedTransport(baseUrl, bearerToken, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, true);
    }

    private static HttpTransport sharedTransport(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction,
            boolean httpLoggingEnabled) {
        CookieManager cookieManager = new CookieManager();
        HttpClient httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).connectTimeout(java.time.Duration.ofSeconds(10)).build();
        return HttpTransport.create(baseUrl, bearerToken, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, httpClient, httpLoggingEnabled);
    }

    private static HttpTransport sharedTransport(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction,
            boolean httpLoggingEnabled, Logger logger) {
        CookieManager cookieManager = new CookieManager();
        HttpClient httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).connectTimeout(java.time.Duration.ofSeconds(10)).build();
        return HttpTransport.create(baseUrl, bearerToken, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, httpClient, httpLoggingEnabled, logger);
    }

    /**
     * Returns a client sharing the HTTP resources and using a different bearer token.
     *
     * @param token non-blank OAuth2 bearer token
     * @return immutable client configured with {@code token}
     */
    public LuminalOpenApiClient withBearerToken(String token) {
        return new LuminalOpenApiClient(publicTransport, transport.withBearerToken(token), null);
    }

    public String accessToken() {
        if (tokenManager == null) {
            throw new IllegalStateException("This client was not created with appId/appSecret token management");
        }
        return tokenManager.currentAccessToken();
    }

    /** @return authorization endpoints */
    public AuthApi auth() {
        return auth;
    }

    /** @return wallet-account endpoints */
    public AccountsApi accounts() {
        return accounts;
    }

    /** @return wallet-transaction endpoints */
    public TransactionsApi transactions() {
        return transactions;
    }

    /** @return shared-account endpoints */
    public SharedAccountsApi sharedAccounts() {
        return sharedAccounts;
    }

    /** @return card endpoints */
    public CardsApi cards() {
        return cards;
    }

    /** @return cardholder endpoints */
    public CardHoldersApi cardHolders() {
        return cardHolders;
    }

    /** @return card-pool endpoints */
    public CardPoolsApi cardPools() {
        return cardPools;
    }

    /** @return card-group endpoints */
    public CardGroupsApi cardGroups() {
        return cardGroups;
    }

    public WebhookEvent<?> parseWebhook(String eventHeader, String eventId, byte[] rawBody, String signature, PublicKey publicKey) {
        WebhookEvent<?> event = WebhookVerifier.parse(eventHeader, eventId, rawBody, signature, publicKey);
        transport.logWebhook(eventHeader, eventId, rawBody);
        return event;
    }


}
