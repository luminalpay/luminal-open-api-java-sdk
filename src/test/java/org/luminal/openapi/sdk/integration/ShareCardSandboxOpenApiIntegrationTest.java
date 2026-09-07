package org.luminal.openapi.sdk.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.luminal.openapi.sdk.LuminalApiException;
import org.luminal.openapi.sdk.LuminalOpenApiClient;
import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoRequest;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupCreateRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupDeleteRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupRequest;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupUpdateRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCardPageRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCountryResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCreateRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderDetailResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderPageRequest;
import org.luminal.openapi.sdk.model.CardModels.CardBinResponse;
import org.luminal.openapi.sdk.model.CardModels.CardBinsRequest;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;
import org.luminal.openapi.sdk.model.CardModels.CardLimitUpdateRequest;
import org.luminal.openapi.sdk.model.CardModels.CardTransactionsRequest;
import org.luminal.openapi.sdk.model.CardModels.IssueCardDetailsRequest;
import org.luminal.openapi.sdk.model.CardModels.IssueCardDetailsResponse;
import org.luminal.openapi.sdk.model.CardModels.IssueCardRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardPageRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardResponse;
import org.luminal.openapi.sdk.model.CardModels.RechargeCardOperationRecordRequest;
import org.luminal.openapi.sdk.model.CardModels.RechargeCardOperationRecordResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResult;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;
import org.luminal.openapi.sdk.model.SharedAccountModels.CreateSharedAccountRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountBalanceRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountGetRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountPageRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionsRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionResponse;
import org.luminal.openapi.sdk.model.TransactionModels.WalletTransactionRequest;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenResult;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.CardStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.RechargeCardTransferStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.SharedAccountOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.TransactionWebhook;
import org.luminal.openapi.sdk.webhook.WebhookEvent;
import org.luminal.openapi.sdk.webhook.WebhookEventType;
import org.luminal.openapi.sdk.webhook.WebhookVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(OrderAnnotation.class)
class ShareCardSandboxOpenApiIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareCardSandboxOpenApiIntegrationTest.class);
    private static final String DEFAULT_BASE_URL = "https://sandbox-openapi.luminalads.com";
    private static final String DEFAULT_APP_ID = "lpsha6pj5mwsb7tz";
    private static final String DEFAULT_APP_SECRET = "P11g59PXY33JjqL4CRJ2Oz3nfsjsWRKe";
    private static final String TEST_CARD_BIN = "22346703";
    private static final String DEFAULT_WEBHOOK_HOST = "0.0.0.0";
    private static final int DEFAULT_WEBHOOK_PORT = 18081;
    private static final String DEFAULT_WEBHOOK_PATH = "/luminal-open-api-webhook";
    private static final int WEBHOOK_QUERY_TIMEOUT_SECONDS = 30;
    private static final int WEBHOOK_WAIT_LOG_INTERVAL_SECONDS = 10;
    private static final int MAX_WEBHOOK_BODY_BYTES = 1024 * 1024;
    private static final LuminalOpenApiClient PUBLIC_CLIENT = new LuminalOpenApiClient(configured(
            "LUMINAL_OPEN_API_BASE_URL", "luminal.openApi.baseUrl", DEFAULT_BASE_URL));
    private static final LuminalOpenApiClient MANAGED_CLIENT = new LuminalOpenApiClient(
            configured("LUMINAL_OPEN_API_BASE_URL", "luminal.openApi.baseUrl", DEFAULT_BASE_URL),
            configured("LUMINAL_OPEN_API_APP_ID", "luminal.openApi.appId", DEFAULT_APP_ID),
            configured("LUMINAL_OPEN_API_APP_SECRET", "luminal.openApi.appSecret", DEFAULT_APP_SECRET),
            null, 1, configured("LUMINAL_OPEN_API_LOCALE", "luminal.openApi.locale", "en"), true);
    private static final ConcurrentMap<Long, CompletableFuture<CardOpenStatusWebhook>> CARD_OPEN_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, CompletableFuture<CardStatusWebhook>> CARD_STATUS_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, CompletableFuture<RechargeCardTransferStatusWebhook>> LIMIT_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, CompletableFuture<SharedAccountOpenStatusWebhook>> SHARED_ACCOUNT_OPEN_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CompletableFuture<TransactionWebhook>> FUND_TRANSACTION_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, TransactionWebhook> SETTLEMENT_WEBHOOKS = new ConcurrentHashMap<>();
    private static final AtomicReference<Throwable> WEBHOOK_ERROR = new AtomicReference<>();
    private static volatile OAuth2Token CACHED_TOKEN;
    private static volatile IllegalStateException CARD_OPEN_FAILURE;
    private static volatile boolean TOKEN_FLOW_READY;
    private static HttpServer WEBHOOK_SERVER;
    private static CardBinResponse CACHED_CARD_BIN;
    private static CardHolderDetailResponse CACHED_CARD_HOLDER_TEMPLATE;
    private static List<CardHolderCountryResponse> CACHED_CARD_HOLDER_COUNTRIES;
    private static Long CACHED_CARD_HOLDER_ID;
    private static Long CACHED_SHARED_ACCOUNT_ID;
    private static Long CACHED_CARD_GROUP_ID;
    private static Long CACHED_DELETE_CARD_GROUP_ID;
    private static Long CACHED_CARD_TASK_ID;
    private static Long CACHED_CARD_ID;
    private static Long CACHED_LIMIT_OPERATION_RECORD_ID;
    private static String CACHED_INCREASE_TRANSACTION_ID;
    private static String CACHED_DECREASE_TRANSACTION_ID;
    private static String CACHED_CARD_NAME;
    private static boolean SHARED_ACCOUNT_CREATION_ATTEMPTED;
    private static boolean CARD_GROUP_CREATION_ATTEMPTED;
    private static boolean DELETE_CARD_GROUP_CREATION_ATTEMPTED;
    private static boolean CARD_CREATION_ATTEMPTED;

    @BeforeAll
    static void startWebhookServer() {
        int port = configuredLong(
                "LUMINAL_OPEN_API_WEBHOOK_PORT", "luminal.openApi.webhookPort", (long) DEFAULT_WEBHOOK_PORT).intValue();
        String host = configured("LUMINAL_OPEN_API_WEBHOOK_HOST", "luminal.openApi.webhookHost", DEFAULT_WEBHOOK_HOST);
        String path = normalizeWebhookPath(configured(
                "LUMINAL_OPEN_API_WEBHOOK_PATH", "luminal.openApi.webhookPath", DEFAULT_WEBHOOK_PATH));
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Webhook port must `be between 1 and 65535");
        }
        try {
            WEBHOOK_SERVER = HttpServer.create(new InetSocketAddress(host, port), 0);
            WEBHOOK_SERVER.createContext(path, ShareCardSandboxOpenApiIntegrationTest::handleWebhook);
            WEBHOOK_SERVER.start();
            LOGGER.info("Webhook listener started: http://{}:{}{}", host, port, path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start webhook listener on " + host + ":" + port, exception);
        }
    }

    @AfterAll
    static void stopWebhookServer() {
        if (WEBHOOK_SERVER != null) {
            WEBHOOK_SERVER.stop(0);
        }
    }

    @Test
    @Order(1)
    void getTokenFromSandbox() {
        CACHED_TOKEN = issueFreshToken();
        assertToken(CACHED_TOKEN);
        TOKEN_FLOW_READY = true;
    }

    @Test
    @Order(2)
    void refreshTokenFromSandbox() {
        ensureTokenFlowReady();
        OAuth2Token current = issueToken();
        try {
            OAuth2Token refreshed = authorizedClient(current).auth().refreshToken(current.refreshToken());
            CACHED_TOKEN = refreshed;
            assertToken(refreshed);
        } catch (LuminalApiException exception) {
            if (!isUnauthorized(exception)) {
                throw exception;
            }
            LOGGER.warn("Sandbox refresh-token returned unauthorized; relogging in");
            CACHED_TOKEN = issueFreshToken();
        }
    }

    @Test
    @Order(3)
    void logoutFromSandbox() {
        ensureTokenFlowReady();
        assertTrue(authorizedClient().auth().logout());
        CACHED_TOKEN = null;
        TOKEN_FLOW_READY = false;
    }

    @Test
    @Order(4)
    void getTokenAfterLogoutFromSandbox() {
        CACHED_TOKEN = issueFreshToken();
        TOKEN_FLOW_READY = true;
        assertToken(CACHED_TOKEN);
    }

    @Test
    @Order(27)
    void listAccountsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().accounts().list(new WalletInfoRequest(1, 20, "USD"));
        assertNotNull(result);
    }

    @Test
    @Order(27)
    void listTransactionsFromSandbox() {
        PageResult<?> result = authorizedClient().transactions().list(
                new WalletTransactionRequest(1, 20, null, null, null));
        assertNotNull(result);
    }

    @Test
    @Order(10)
    void createSharedAccountFromSandbox() {
        assertNotNull(ensureSharedAccountId());
    }

    @Test
    @Order(27)
    void listSharedAccountsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().sharedAccounts().list(
                new SharedAccountPageRequest(1, 20, null, null));
        assertNotNull(result);
    }

    @Test
    @Order(12)
    void increaseSharedAccountFromSandbox() {
        Long accountId = ensureSharedAccountId();
        var transaction = authorizedClient().sharedAccounts().increase(
                new SharedAccountBalanceRequest(accountId, BigDecimal.TEN));
        assertNotNull(transaction);
        CACHED_INCREASE_TRANSACTION_ID = transaction.sharedAccountTransactionId();
        awaitSharedAccountTransaction(CACHED_INCREASE_TRANSACTION_ID, accountId);
    }

    @Test
    @Order(15)
    void decreaseSharedAccountFromSandbox() {
        Long accountId = ensureSharedAccountId();
        awaitSharedAccountTransaction(CACHED_INCREASE_TRANSACTION_ID, accountId);
        var transaction = authorizedClient().sharedAccounts().decrease(
                new SharedAccountBalanceRequest(accountId, BigDecimal.ONE));
        assertNotNull(transaction);
        CACHED_DECREASE_TRANSACTION_ID = transaction.sharedAccountTransactionId();
        awaitSharedAccountTransaction(CACHED_DECREASE_TRANSACTION_ID, accountId);
    }

    @Test
    @Order(27)
    void getSharedAccountDetailsFromSandbox() {
        assertNotNull(authorizedClient().sharedAccounts().details(new SharedAccountGetRequest(ensureSharedAccountId())));
    }

    @Test
    @Order(27)
    void listSharedAccountTransactionsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().sharedAccounts().transactions(
                new SharedAccountTransactionsRequest(1, 20, null, null, null, null, null));
        assertNotNull(result);
    }

    @Test
    @Order(27)
    void listCardBinsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().cards().bins(
                new CardBinsRequest(1, 20, "SHARED", null, null, null));
        assertNotNull(result);
    }

    @Test
    @Order(12)
    void resolveSharedCardholderAccordingToCardBinFromSandbox() {
        CardBinResponse cardBin = firstCardBin();
        if (supportsCustomCardholder(cardBin)) {
            assertNotNull(cardHolderIdForIssue());
        } else {
            LOGGER.info("Shared-card BIN {} does not support an explicit cardholder; cardHolderId will be omitted",
                    cardBin.cardBin());
        }
    }

    @Test
    @Order(13)
    void issueCardFromSandbox() {
        assertNotNull(issueCardTaskId());
    }

    @Test
    @Order(14)
    void waitForCardOpenStatusWebhookFromSandbox() {
        assertNotNull(ensureCardId());
    }

    @Test
    @Order(16)
    void listSharedCardsAssociatedWithCardholderFromSandbox() {
        CardBinResponse cardBin = firstCardBin();
        if (!supportsCustomCardholder(cardBin)) {
            LOGGER.info("Skipping shared-card/cardholder association check because BIN {} does not support cardholders",
                    cardBin.cardBin());
            return;
        }
        Long cardId = ensureCardId();
        assertTrue(authorizedClient().cardHolders().associatedCards(
                        new CardHolderCardPageRequest(1, 20, cardHolderIdForIssue(), cardId)).list().stream()
                .anyMatch(item -> Objects.equals(cardId, item.memberCardId())));
    }

    @Test
    @Order(27)
    void listCardsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().cards().list(
                new MemberCardPageRequest(1, 20, null, null, null, "SHARED", null, null));
        assertNotNull(result);
    }

    @Test
    @Order(27)
    void getCardCvvFromSandbox() {
        assertNotNull(authorizedClient().cards().cvv(new CardIdRequest(ensureCardId())));
    }

    @Test
    @Order(27)
    void listCardTransactionsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().cards().transactions(
                new CardTransactionsRequest(1, 20, "SHARED", ensureCardId(), null));
        assertNotNull(result);
    }

    @Test
    @Order(27)
    void getCardLimitFromSandbox() {
        assertNotNull(authorizedClient().cards().limit(new CardIdRequest(ensureCardId())));
    }

    @Test
    @Order(27)
    void modifyCardLimitFromSandbox() {
        CACHED_LIMIT_OPERATION_RECORD_ID = Objects.requireNonNull(
                authorizedClient().cards().modifyLimitAsync(new CardLimitUpdateRequest(
                        ensureCardId(), BigDecimal.ONE)),
                "Limit operation-record ID is missing");
        RechargeCardOperationRecordResponse result = awaitLimitOperation(CACHED_LIMIT_OPERATION_RECORD_ID);
        assertTrue("SUCCESS".equalsIgnoreCase(result.status()));
    }

    @Test
    @Order(30)
    void freezeCardFromSandbox() {
        Long cardId = ensureCardId();
        String status = cardStatus(cardId);
        if ("FREEZE".equalsIgnoreCase(status)) {
            return;
        }
        if ("PRE_FREEZE".equalsIgnoreCase(status)) {
            awaitCardStatus(cardId, "FREEZE");
            return;
        }
        assertTrue(authorizedClient().cards().freeze(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "FREEZE");
    }

    @Test
    @Order(31)
    void unfreezeCardFromSandbox() {
        Long cardId = ensureFrozenCardId();
        if ("ACTIVE".equalsIgnoreCase(cardStatus(cardId))) {
            return;
        }
        assertTrue(authorizedClient().cards().unfreeze(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "ACTIVE");
    }

    @Test
    @Order(997)
    void cancelCardFromSandbox() {
        Long cardId = ensureCardId();
        String status = cardStatus(cardId);
        if ("CANCEL".equalsIgnoreCase(status)) {
            return;
        }
        if ("PRE_CANCEL".equalsIgnoreCase(status)) {
            awaitCardStatus(cardId, "CANCEL");
            return;
        }
        if ("FREEZE".equalsIgnoreCase(status)) {
            assertTrue(authorizedClient().cards().unfreeze(new CardIdRequest(cardId)));
            awaitCardStatus(cardId, "ACTIVE");
        }
        assertTrue(authorizedClient().cards().cancel(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "CANCEL");
    }

    @Test
    @Order(27)
    void getIssueDetailsFromSandbox() {
        ensureCardId();
        assertNotNull(authorizedClient().cards().issueDetails(new IssueCardDetailsRequest(
                issueCardTaskId())));
    }

    @Test
    @Order(27)
    void listCardGroupsFromSandbox() {
        PageResultEx<?, ?> result = authorizedClient().cardGroups().list(new CardGroupRequest(1, 20, "SHARED"));
        assertNotNull(result);
    }

    @Test
    @Order(11)
    void createCardGroupFromSandbox() {
        assertNotNull(ensureCardGroupId());
    }

    @Test
    @Order(27)
    void updateCardGroupFromSandbox() {
        assertTrue(authorizedClient().cardGroups().update(new CardGroupUpdateRequest(
                ensureCardGroupId(), uniqueName("sdk-sandbox-updated"))));
    }

    @Test
    @Order(998)
    void deleteCardGroupFromSandbox() {
        assertTrue(authorizedClient().cardGroups().delete(new CardGroupDeleteRequest(
                ensureDeleteCardGroupId())));
    }

    private static OAuth2Token issueToken() {
        OAuth2Token token = CACHED_TOKEN;
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            token = issueFreshToken();
        }
        assertToken(token);
        return token;
    }

    private static OAuth2Token issueFreshToken() {
        OAuth2Token token = PUBLIC_CLIENT.auth().getToken(appId(), appSecret());
        CACHED_TOKEN = token;
        assertToken(token);
        return token;
    }

    private static LuminalOpenApiClient publicClient() {
        return PUBLIC_CLIENT;
    }

    private static LuminalOpenApiClient authorizedClient() {
        return MANAGED_CLIENT;
    }

    private static LuminalOpenApiClient authorizedClient(OAuth2Token token) {
        return publicClient().withBearerToken(token.accessToken());
    }

    private static String appId() {
        return configured("LUMINAL_OPEN_API_APP_ID", "luminal.openApi.appId", DEFAULT_APP_ID);
    }

    private static String appSecret() {
        return configured("LUMINAL_OPEN_API_APP_SECRET", "luminal.openApi.appSecret", DEFAULT_APP_SECRET);
    }


    private static void assertToken(OAuth2Token token) {
        assertNotNull(token);
        assertFalse(token.accessToken().isBlank());
    }


    private static synchronized CardBinResponse firstCardBin() {
        if (CACHED_CARD_BIN == null) {
            PageResultEx<CardBinResponse, Object> bins = authorizedClient().cards().bins(
                    new CardBinsRequest(1, 20, "SHARED", null, TEST_CARD_BIN, null));
            assertNotNull(bins);
            CACHED_CARD_BIN = bins.list().stream()
                    .filter(item -> TEST_CARD_BIN.equals(item.cardBin()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Card BIN not found: " + TEST_CARD_BIN));
        }
        return CACHED_CARD_BIN;
    }

    private static synchronized Long firstCardBinId() {
        return Objects.requireNonNull(firstCardBin().cardBinId(), "Card BIN ID is missing");
    }

    private static synchronized Long ensureSharedAccountId() {
        if (CACHED_SHARED_ACCOUNT_ID == null) {
            if (SHARED_ACCOUNT_CREATION_ATTEMPTED) {
                throw new IllegalStateException("Shared account creation already failed in this test run");
            }
            SHARED_ACCOUNT_CREATION_ATTEMPTED = true;
            CACHED_SHARED_ACCOUNT_ID = createSharedAccountAndReturnId(uniqueName("sdk-sandbox-sa"));
        }
        return CACHED_SHARED_ACCOUNT_ID;
    }

    private static Long createSharedAccountAndReturnId(String name) {
        var created = authorizedClient().sharedAccounts().create(
                new CreateSharedAccountRequest(firstCardBinId(), new BigDecimal("100"), name));
        assertNotNull(created);
        Long accountId = Objects.requireNonNull(created.memberSharedAccountId(), "Created shared account ID is missing");
        CACHED_SHARED_ACCOUNT_ID = accountId;
        awaitSharedAccountOpenStatus(accountId);
        return accountId;
    }

    private static void awaitSharedAccountOpenStatus(Long accountId) {
        CompletableFuture<SharedAccountOpenStatusWebhook> future =
                SHARED_ACCOUNT_OPEN_WEBHOOKS.computeIfAbsent(accountId, ignored -> new CompletableFuture<>());
        try {
            SharedAccountOpenStatusWebhook payload = awaitWebhook(
                    future, "SHARED_ACCOUNT_OPEN_STATUS accountId=" + accountId);
            if (!"SUCCESS".equalsIgnoreCase(payload.status())) {
                throw new IllegalStateException("Shared account opening failed: accountId=" + accountId
                        + " status=" + payload.status());
            }
        } catch (TimeoutException exception) {
            assertNotNull(authorizedClient().sharedAccounts().details(
                    new SharedAccountGetRequest(accountId)));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for SHARED_ACCOUNT_OPEN_STATUS webhook", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("SHARED_ACCOUNT_OPEN_STATUS webhook failed", exception.getCause());
        } finally {
            SHARED_ACCOUNT_OPEN_WEBHOOKS.remove(accountId, future);
        }
    }

    private static boolean supportsCustomCardholder(CardBinResponse cardBin) {
        return cardBin != null && Objects.equals(1, cardBin.customCardholder());
    }

    private static synchronized Long cardHolderIdForIssue() {
        if (CACHED_CARD_HOLDER_ID == null) {
            CardHolderDetailResponse existing = existingCardHolderTemplate();
            if (existing != null && existing.cardHolderId() != null) {
                CACHED_CARD_HOLDER_ID = existing.cardHolderId();
                LOGGER.info("Reusing Sandbox cardholder for shared-card issuance: {}", CACHED_CARD_HOLDER_ID);
            } else {
                CACHED_CARD_HOLDER_ID = Objects.requireNonNull(
                        authorizedClient().cardHolders().add(cardHolderProfile("Shared-card issuance")),
                        "Cardholder ID is missing");
                LOGGER.info("Created Sandbox cardholder for shared-card issuance: {}", CACHED_CARD_HOLDER_ID);
            }
        }
        return CACHED_CARD_HOLDER_ID;
    }

    private static CardHolderCreateRequest cardHolderProfile(String addressLine2) {
        long suffix = System.currentTimeMillis() % 1_000_000L;
        CardHolderDetailResponse template = existingCardHolderTemplate();
        CardHolderCountryResponse country = selectedCardHolderCountry(template);
        return new CardHolderCreateRequest(
                "Sdk",
                "Sandbox" + alphabeticSuffix(suffix),
                LocalDate.of(1990, 1, 15),
                "sdk-shared-card-" + suffix + "@example.com",
                localPhone(country, suffix),
                country.areaCode(),
                country.countryId(),
                template == null ? "10001" : template.postalCode(),
                template == null ? "New York" : template.state(),
                template == null ? "New York" : template.city(),
                template == null ? "350 Fifth Avenue" : template.addressLine1(),
                addressLine2);
    }

    private static CardHolderCountryResponse selectedCardHolderCountry(CardHolderDetailResponse template) {
        List<CardHolderCountryResponse> countries = cardHolderCountries();
        Long configuredId = optionalLong(
                "LUMINAL_OPEN_API_CARD_HOLDER_COUNTRY_ID", "luminal.openApi.cardHolderCountryId");
        String configuredAreaCode = configured(
                "LUMINAL_OPEN_API_CARD_HOLDER_AREA_CODE", "luminal.openApi.cardHolderAreaCode", null);
        if (configuredId != null) {
            CardHolderCountryResponse country = countries.stream()
                    .filter(item -> Objects.equals(configuredId, item.countryId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Configured cardholder country ID is not returned by cardHolders().countries(): "
                                    + configuredId));
            if (configuredAreaCode != null && !Objects.equals(configuredAreaCode, country.areaCode())) {
                throw new IllegalStateException("Configured cardholder area code does not match country "
                        + configuredId + ": expected " + country.areaCode() + ", actual " + configuredAreaCode);
            }
            return country;
        }
        if (configuredAreaCode != null) {
            List<CardHolderCountryResponse> matches = countries.stream()
                    .filter(item -> Objects.equals(configuredAreaCode, item.areaCode()))
                    .toList();
            if (matches.size() != 1) {
                throw new IllegalStateException("Configured cardholder area code must match exactly one country: "
                        + configuredAreaCode);
            }
            return matches.get(0);
        }
        if (template != null && template.countryId() != null) {
            CardHolderCountryResponse templateCountry = countries.stream()
                    .filter(item -> Objects.equals(template.countryId(), item.countryId()))
                    .findFirst()
                    .orElse(null);
            if (templateCountry != null) {
                return templateCountry;
            }
        }
        return countries.stream()
                .filter(item -> "HK".equalsIgnoreCase(item.countryCode()))
                .findFirst()
                .orElse(countries.get(0));
    }

    private static synchronized List<CardHolderCountryResponse> cardHolderCountries() {
        if (CACHED_CARD_HOLDER_COUNTRIES == null) {
            List<CardHolderCountryResponse> countries = authorizedClient().cardHolders().countries();
            if (countries == null || countries.isEmpty()) {
                throw new IllegalStateException("cardHolders().countries() returned no countries");
            }
            CACHED_CARD_HOLDER_COUNTRIES = List.copyOf(countries);
        }
        return CACHED_CARD_HOLDER_COUNTRIES;
    }

    private static String localPhone(CardHolderCountryResponse country, long suffix) {
        Integer phoneMaxLength = country.phoneMaxLength();
        if (phoneMaxLength == null || phoneMaxLength <= 0) {
            throw new IllegalStateException("Country has invalid phoneMaxLength: " + country.countryId());
        }
        String seed = Long.toString(Math.floorMod(suffix, 1_000_000L));
        StringBuilder phone = new StringBuilder(phoneMaxLength).append('5');
        while (phone.length() < phoneMaxLength) {
            phone.append(seed);
        }
        return phone.substring(0, phoneMaxLength);
    }

    private static String alphabeticSuffix(long value) {
        long remaining = Math.floorMod(value, 26L * 26L * 26L);
        StringBuilder suffix = new StringBuilder(3);
        for (int index = 0; index < 3; index++) {
            suffix.append((char) ('A' + remaining % 26));
            remaining /= 26;
        }
        return suffix.toString();
    }

    private static CardHolderDetailResponse existingCardHolderTemplate() {
        if (CACHED_CARD_HOLDER_TEMPLATE == null) {
            var result = authorizedClient().cardHolders().page(
                    new CardHolderPageRequest(1, 1, null, null, null, null, null));
            if (result != null && result.list() != null) {
                CACHED_CARD_HOLDER_TEMPLATE = result.list().stream()
                        .filter(item -> item.cardHolderId() != null)
                        .map(item -> authorizedClient().cardHolders().detail(item.cardHolderId()))
                        .findFirst()
                        .orElse(null);
            }
        }
        return CACHED_CARD_HOLDER_TEMPLATE;
    }

    private static synchronized Long ensureCardGroupId() {
        if (CACHED_CARD_GROUP_ID == null) {
            if (CARD_GROUP_CREATION_ATTEMPTED) {
                throw new IllegalStateException("Card group creation already failed in this test run");
            }
            CARD_GROUP_CREATION_ATTEMPTED = true;
            var created = authorizedClient().cardGroups().create(
                    new CardGroupCreateRequest(uniqueName("sdk-sandbox-group"), "SHARED"));
            assertNotNull(created);
            CACHED_CARD_GROUP_ID = Objects.requireNonNull(created.cardGroupId(), "Created card group ID is missing");
        }
        return CACHED_CARD_GROUP_ID;
    }

    private static synchronized Long ensureDeleteCardGroupId() {
        if (CACHED_DELETE_CARD_GROUP_ID == null) {
            if (DELETE_CARD_GROUP_CREATION_ATTEMPTED) {
                throw new IllegalStateException("Delete-card-group creation already failed in this test run");
            }
            DELETE_CARD_GROUP_CREATION_ATTEMPTED = true;
            var created = authorizedClient().cardGroups().create(
                    new CardGroupCreateRequest(uniqueName("sdk-sandbox-delete-group"), "SHARED"));
            assertNotNull(created);
            CACHED_DELETE_CARD_GROUP_ID = Objects.requireNonNull(created.cardGroupId(),
                    "Created delete-test card group ID is missing");
        }
        return CACHED_DELETE_CARD_GROUP_ID;
    }

    private static synchronized Long issueCardTaskId() {
        if (CACHED_CARD_TASK_ID == null) {
            if (CARD_CREATION_ATTEMPTED) {
                throw new IllegalStateException("Card creation already failed in this test run");
            }
            CARD_CREATION_ATTEMPTED = true;
            CACHED_CARD_NAME = uniqueName("sdk-sandbox-card");
            var privateKey = RsaSignatures.readPrivateKey(readPrivateKeyPem());
            CardBinResponse cardBin = firstCardBin();
            Long cardHolderId = supportsCustomCardholder(cardBin) ? cardHolderIdForIssue() : null;
            CACHED_CARD_TASK_ID = Objects.requireNonNull(authorizedClient().cards().issue(
                    new IssueCardRequest(
                            1,
                            firstCardBinId(),
                            ensureCardGroupId(),
                            CACHED_CARD_NAME,
                            "SHARED",
                            ensureSharedAccountId(),
                            null,
                            BigDecimal.ONE,
                            BigDecimal.ONE,
                            cardHolderId), privateKey), "Card issue task ID is missing");
        }
        return CACHED_CARD_TASK_ID;
    }

    private static Long ensureCardId() {
        Long cardId = CACHED_CARD_ID;
        if (cardId != null) {
            return cardId;
        }
        IllegalStateException failure = CARD_OPEN_FAILURE;
        if (failure != null) {
            throw failure;
        }
        synchronized (ShareCardSandboxOpenApiIntegrationTest.class) {
            if (CACHED_CARD_ID == null) {
                failure = CARD_OPEN_FAILURE;
                if (failure != null) {
                    throw failure;
                }
                try {
                    CACHED_CARD_ID = awaitIssuedCardId(issueCardTaskId());
                    awaitCardStatus(CACHED_CARD_ID, "ACTIVE");
                } catch (IllegalStateException exception) {
                    CARD_OPEN_FAILURE = exception;
                    throw exception;
                }
            }
            return CACHED_CARD_ID;
        }
    }

    private static Long awaitIssuedCardId(Long taskId) {
        Throwable webhookError = WEBHOOK_ERROR.get();
        if (webhookError != null) {
            throw new IllegalStateException("CARD_OPEN_STATUS webhook failed", webhookError);
        }
        CompletableFuture<CardOpenStatusWebhook> future =
                CARD_OPEN_WEBHOOKS.computeIfAbsent(taskId, ignored -> new CompletableFuture<>());
        CardOpenStatusWebhook payload;
        try {
            payload = awaitWebhook(future, "CARD_OPEN_STATUS taskId=" + taskId);
        } catch (TimeoutException exception) {
            return issuedCardIdFromDetails(taskId, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            IllegalStateException failure = new IllegalStateException(
                    "Interrupted while waiting for CARD_OPEN_STATUS webhook", exception);
            WEBHOOK_ERROR.compareAndSet(null, failure);
            throw failure;
        } catch (ExecutionException exception) {
            IllegalStateException failure = new IllegalStateException(
                    "CARD_OPEN_STATUS webhook failed", exception.getCause());
            WEBHOOK_ERROR.compareAndSet(null, failure);
            throw failure;
        } finally {
            CARD_OPEN_WEBHOOKS.remove(taskId, future);
        }

        if (!Objects.equals(taskId, payload.cardApplyTaskId())) {
            throw new IllegalStateException("Unexpected card task in webhook: " + payload.cardApplyTaskId());
        }
        if (payload.list() == null || payload.list().isEmpty()) {
            throw new IllegalStateException("CARD_OPEN_STATUS webhook contains no card result");
        }
        CardOpenResult result = payload.list().stream()
                .filter(item -> item.memberCardId() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Card opening failed: " + payload.list()));
        if (!"SUCCESS".equalsIgnoreCase(result.cardStatus())) {
            throw new IllegalStateException("Card opening failed: " + result.cardStatus() + ", " + result.message());
        }

        List<IssueCardDetailsResponse> details = authorizedClient().cards().issueDetails(
                new IssueCardDetailsRequest(taskId));
        IssueCardDetailsResponse detail = details.stream()
                .filter(item -> Objects.equals(result.memberCardId(), item.memberCardId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Card " + result.memberCardId() + " missing from issue details for task " + taskId));
        if (detail.cardStatus() == null || "APPLYING".equalsIgnoreCase(detail.cardStatus())) {
            throw new IllegalStateException("Card is not ready after successful CARD_OPEN_STATUS webhook: "
                    + detail.cardStatus());
        }
        return result.memberCardId();
    }

    private static Long issuedCardIdFromDetails(Long taskId, TimeoutException timeout) {
        List<IssueCardDetailsResponse> details = authorizedClient().cards().issueDetails(
                new IssueCardDetailsRequest(taskId));
        IssueCardDetailsResponse detail = details.stream()
                .filter(item -> item.memberCardId() != null)
                .filter(item -> item.cardStatus() != null && !"APPLYING".equalsIgnoreCase(item.cardStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CARD_OPEN_STATUS webhook timed out after "
                        + WEBHOOK_QUERY_TIMEOUT_SECONDS + " seconds and card is not ready for task " + taskId, timeout));
        if ("FAIL".equalsIgnoreCase(detail.cardStatus())) {
            throw new IllegalStateException("Card opening failed: " + detail.cardStatus() + ", " + detail.message());
        }
        return detail.memberCardId();
    }

    private static void handleWebhook(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405);
                return;
            }
            String eventName = exchange.getRequestHeaders().getFirst("event");
            String eventId = exchange.getRequestHeaders().getFirst("event_id");
            if (eventName == null || eventName.isBlank() || eventId == null || eventId.isBlank()) {
                respond(exchange, 400);
                return;
            }
            boolean cardOpenEvent = WebhookEventType.CARD_OPEN_STATUS.name().equals(eventName);
            boolean cardStatusEvent = WebhookEventType.CARD_STATUS.name().equals(eventName);
            boolean limitEvent = WebhookEventType.CARD_LIMIT_STATUS.name().equals(eventName);
            boolean sharedAccountOpenEvent = WebhookEventType.SHARED_ACCOUNT_OPEN_STATUS.name().equals(eventName);
            boolean fundTransactionEvent = WebhookEventType.SHARE_ACCOUNT_FUND_TRANSACTIONS.name().equals(eventName);
            boolean settlementEvent = WebhookEventType.CARD_SETTLE_STATUS.name().equals(eventName);
            if (!cardOpenEvent && !cardStatusEvent && !sharedAccountOpenEvent
                    && !fundTransactionEvent && !settlementEvent && !limitEvent) {
                respond(exchange, 200);
                return;
            }
            byte[] rawBody;
            try (InputStream input = exchange.getRequestBody()) {
                rawBody = input.readNBytes(MAX_WEBHOOK_BODY_BYTES + 1);
            }
            if (rawBody.length > MAX_WEBHOOK_BODY_BYTES) {
                respond(exchange, 413);
                return;
            }
            String signature = exchange.getRequestHeaders().getFirst("sign");
            if (signature == null || signature.isBlank()) {
                respond(exchange, 400);
                return;
            }
            String publicKeyPem = webhookPublicKeyPem();
            WebhookEvent<?> event;
            if (publicKeyPem == null) {
                LOGGER.warn("Webhook public key is not configured; event={} signature cannot be verified", eventName);
                Object payload = cardOpenEvent
                        ? JsonSupport.readValue(rawBody, CardOpenStatusWebhook.class)
                        : cardStatusEvent
                        ? JsonSupport.readValue(rawBody, CardStatusWebhook.class)
                        : limitEvent
                        ? JsonSupport.readValue(rawBody, RechargeCardTransferStatusWebhook.class)
                        : sharedAccountOpenEvent
                        ? JsonSupport.readValue(rawBody, SharedAccountOpenStatusWebhook.class)
                        : JsonSupport.readValue(rawBody, TransactionWebhook.class);
                event = new WebhookEvent<>(WebhookEventType.valueOf(eventName), eventId, rawBody, signature, payload);
            } else {
                event = MANAGED_CLIENT.parseWebhook(eventName, eventId, rawBody, signature,
                        RsaSignatures.readPublicKey(publicKeyPem));
            }
            if (cardOpenEvent) {
                CardOpenStatusWebhook payload = (CardOpenStatusWebhook) event.payload();
                if (payload.cardApplyTaskId() == null || payload.list() == null || payload.list().isEmpty()) {
                    respond(exchange, 400);
                    return;
                }
                if (isTerminalCardOpenPayload(payload)) {
                    CARD_OPEN_WEBHOOKS.computeIfAbsent(payload.cardApplyTaskId(), ignored -> new CompletableFuture<>())
                            .complete(payload);
                } else {
                    LOGGER.info("Ignoring non-terminal CARD_OPEN_STATUS webhook taskId={}", payload.cardApplyTaskId());
                }
            } else if (cardStatusEvent) {
                CardStatusWebhook payload = (CardStatusWebhook) event.payload();
                if (payload.memberCardId() == null || payload.cardStatus() == null || payload.cardStatus().isBlank()) {
                    respond(exchange, 400);
                    return;
                }
                CARD_STATUS_WEBHOOKS.computeIfAbsent(Long.valueOf(payload.memberCardId()),
                        ignored -> new CompletableFuture<>()).complete(payload);
            } else if (limitEvent) {
                RechargeCardTransferStatusWebhook payload =
                        (RechargeCardTransferStatusWebhook) event.payload();
                if (!isCurrentSharedCardLimit(payload)) {
                    LOGGER.info("Ignoring CARD_LIMIT_STATUS webhook for operationRecordId={} cardId={} cardType={} operationType={} status={}",
                            payload.memberCardOperationRecordId(), payload.memberCardId(), payload.cardType(),
                            payload.operationType(), payload.status());
                } else {
                    LIMIT_WEBHOOKS.computeIfAbsent(payload.memberCardOperationRecordId(),
                            ignored -> new CompletableFuture<>()).complete(payload);
                }
            } else if (sharedAccountOpenEvent) {
                SharedAccountOpenStatusWebhook payload = (SharedAccountOpenStatusWebhook) event.payload();
                if (payload.memberSharedAccountId() == null || payload.status() == null || payload.status().isBlank()) {
                    respond(exchange, 400);
                    return;
                }
                if (isTerminalSharedAccountOpenPayload(payload)) {
                    CompletableFuture<SharedAccountOpenStatusWebhook> future = SHARED_ACCOUNT_OPEN_WEBHOOKS
                            .computeIfAbsent(payload.memberSharedAccountId(), ignored -> new CompletableFuture<>());
                    if ("SUCCESS".equalsIgnoreCase(payload.status())) {
                        future.complete(payload);
                    } else {
                        future.completeExceptionally(new IllegalStateException(
                                "Shared account opening failed: accountId=" + payload.memberSharedAccountId()
                                        + " status=" + payload.status()));
                    }
                } else {
                    LOGGER.info("Ignoring non-terminal SHARED_ACCOUNT_OPEN_STATUS webhook accountId={} status={}",
                            payload.memberSharedAccountId(), payload.status());
                }
            } else if (settlementEvent) {
                TransactionWebhook payload = (TransactionWebhook) event.payload();
                if (!isCurrentSharedCardSettlement(payload)) {
                    LOGGER.info("Ignoring CARD_SETTLE_STATUS webhook for transactionId={} accountId={} cardId={} cardType={}",
                            payload.sharedAccountTransactionId(), payload.memberSharedAccountId(),
                            payload.memberCardId(), payload.cardType());
                } else {
                    SETTLEMENT_WEBHOOKS.put(payload.sharedAccountTransactionId(), payload);
                    LOGGER.info("Recorded shared-card CARD_SETTLE_STATUS webhook transactionId={} settleStatus={} settleTime={}",
                            payload.sharedAccountTransactionId(), payload.settleStatus(), payload.settleTime());
                }
            } else {
                TransactionWebhook payload = (TransactionWebhook) event.payload();
                if (payload.sharedAccountTransactionId() == null || payload.sharedAccountTransactionId().isBlank()) {
                    respond(exchange, 400);
                    return;
                }
                if (isTerminalTransactionPayload(payload)) {
                    FUND_TRANSACTION_WEBHOOKS.computeIfAbsent(payload.sharedAccountTransactionId(),
                            ignored -> new CompletableFuture<>()).complete(payload);
                } else {
                    LOGGER.info("Ignoring non-terminal shared-account transaction webhook transactionId={} status={}",
                            payload.sharedAccountTransactionId(), payload.status());
                }
            }
            respond(exchange, 200);
        } catch (WebhookVerificationException exception) {
            WEBHOOK_ERROR.compareAndSet(null, exception);
            LOGGER.error("Webhook signature validation failed", exception);
            respond(exchange, 401);
        } catch (Exception exception) {
            WEBHOOK_ERROR.compareAndSet(null, exception);
            LOGGER.error("Webhook handling failed", exception);
            respond(exchange, 400);
        } finally {
            exchange.close();
        }
    }

    private static boolean isTerminalCardOpenPayload(CardOpenStatusWebhook payload) {
        return payload.list() != null && !payload.list().isEmpty()
                && payload.list().stream().allMatch(item ->
                "SUCCESS".equalsIgnoreCase(item.cardStatus()) || "FAIL".equalsIgnoreCase(item.cardStatus()));
    }

    private static boolean isTerminalTransactionPayload(TransactionWebhook payload) {
        return "SUCCESS".equalsIgnoreCase(payload.status()) || "FAIL".equalsIgnoreCase(payload.status());
    }

    private static boolean isTerminalSharedAccountOpenPayload(SharedAccountOpenStatusWebhook payload) {
        return "SUCCESS".equalsIgnoreCase(payload.status()) || "FAIL".equalsIgnoreCase(payload.status());
    }

    private static boolean isFinalStatus(String status) {
        return "SUCCESS".equalsIgnoreCase(status) || "FAIL".equalsIgnoreCase(status);
    }

    private static boolean isCurrentSharedCardSettlement(TransactionWebhook payload) {
        if (payload.sharedAccountTransactionId() == null || payload.sharedAccountTransactionId().isBlank()
                || !"SHARED".equalsIgnoreCase(payload.cardType())) {
            return false;
        }
        Long accountId = parseLong(payload.memberSharedAccountId());
        Long cardId = parseLong(payload.memberCardId());
        return CACHED_SHARED_ACCOUNT_ID != null && Objects.equals(CACHED_SHARED_ACCOUNT_ID, accountId)
                && CACHED_CARD_ID != null && Objects.equals(CACHED_CARD_ID, cardId);
    }

    private static boolean isCurrentSharedCardLimit(RechargeCardTransferStatusWebhook payload) {
        if (payload == null || payload.memberCardOperationRecordId() == null
                || CACHED_CARD_ID == null || !Objects.equals(CACHED_CARD_ID, payload.memberCardId())) {
            return false;
        }
        Long operationRecordId = payload.memberCardOperationRecordId();
        return "SHARED".equalsIgnoreCase(payload.cardType())
                && (Objects.equals(CACHED_LIMIT_OPERATION_RECORD_ID, operationRecordId)
                || LIMIT_WEBHOOKS.containsKey(operationRecordId))
                && "MODIFY_LIMITS".equalsIgnoreCase(payload.operationType())
                && isFinalStatus(payload.status());
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String webhookPublicKeyPem() {
        String configured = configured(
                "LUMINAL_OPEN_API_WEBHOOK_PUBLIC_KEY",
                "luminal.openApi.webhookPublicKey",
                null);
        if (configured != null) {
            return configured;
        }
        String resource = "/org/luminal/openapi/sdk/integration/platform-public-key.pem";
        try (InputStream input = ShareCardSandboxOpenApiIntegrationTest.class.getResourceAsStream(resource)) {
            return input == null ? null : new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read webhook public key resource", exception);
        }
    }

    private static void respond(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    private static String normalizeWebhookPath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static Long ensureFrozenCardId() {
        Long cardId = ensureCardId();
        String status = cardStatus(cardId);
        if ("FREEZE".equalsIgnoreCase(status)) {
            return cardId;
        }
        if ("PRE_FREEZE".equalsIgnoreCase(status)) {
            awaitCardStatus(cardId, "FREEZE");
            return cardId;
        }
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw new IllegalStateException("Card cannot be frozen from status " + status);
        }
        assertTrue(authorizedClient().cards().freeze(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "FREEZE");
        return cardId;
    }

    private static String cardStatus(Long cardId) {
        PageResultEx<MemberCardResponse, Object> result = authorizedClient().cards().list(
                new MemberCardPageRequest(1, 1, cardId, null, null, "SHARED", null, null));
        return result.list().stream()
                .filter(item -> Objects.equals(cardId, item.memberCardId()))
                .map(MemberCardResponse::status)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Card not found: " + cardId));
    }

    private static void awaitCardStatus(Long cardId, String expectedStatus) {
        CompletableFuture<CardStatusWebhook> webhookFuture = CARD_STATUS_WEBHOOKS.computeIfAbsent(
                cardId, ignored -> new CompletableFuture<>());
        String status = null;
        try {
            CardStatusWebhook webhook = awaitWebhook(webhookFuture, "CARD_STATUS cardId=" + cardId);
            if (webhook != null && cardId.equals(Long.valueOf(webhook.memberCardId()))
                    && expectedStatus.equalsIgnoreCase(webhook.cardStatus())) {
                return;
            }
            status = webhook == null ? null : webhook.cardStatus();
        } catch (TimeoutException exception) {
            status = cardStatus(cardId);
            if (expectedStatus.equalsIgnoreCase(status)) {
                return;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for card status webhook", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Card status webhook failed", exception.getCause());
        } finally {
            CARD_STATUS_WEBHOOKS.remove(cardId, webhookFuture);
        }
        throw new IllegalStateException("Card " + cardId + " did not reach status "
                + expectedStatus + "; current status=" + status);
    }

    private static RechargeCardOperationRecordResponse awaitLimitOperation(Long operationRecordId) {
        failOnWebhookError();
        CompletableFuture<RechargeCardTransferStatusWebhook> future = LIMIT_WEBHOOKS.computeIfAbsent(
                operationRecordId, ignored -> new CompletableFuture<>());
        try {
            RechargeCardTransferStatusWebhook webhook = awaitWebhook(
                    future, "CARD_LIMIT_STATUS operationRecordId=" + operationRecordId);
            validateSharedCardLimit(operationRecordId, CACHED_CARD_ID,
                    webhook.memberCardOperationRecordId(), webhook.memberCardId(), webhook.cardType(),
                    webhook.operationType(), webhook.status(), webhook.message());
            return operationRecordFromWebhook(webhook);
        } catch (TimeoutException exception) {
            return queryFinalLimitOperationRecord(operationRecordId, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for CARD_LIMIT_STATUS webhook", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("CARD_LIMIT_STATUS webhook failed", exception.getCause());
        } finally {
            LIMIT_WEBHOOKS.remove(operationRecordId, future);
        }
    }

    private static RechargeCardOperationRecordResponse queryFinalLimitOperationRecord(
            Long operationRecordId, TimeoutException webhookTimeout) {
        RechargeCardOperationRecordResponse result = Objects.requireNonNull(
                authorizedClient().cards().operationRecord(new RechargeCardOperationRecordRequest(operationRecordId)),
                "Limit operation record is missing");
        if (!isFinalStatus(result.status())) {
            String message = "CARD_LIMIT_STATUS webhook timed out after " + WEBHOOK_QUERY_TIMEOUT_SECONDS
                    + " seconds and operation record is not final: operationRecordId=" + operationRecordId
                    + ", status=" + result.status();
            throw new IllegalStateException(message, webhookTimeout);
        }
        validateSharedCardLimit(operationRecordId, CACHED_CARD_ID,
                result.memberCardOperationRecordId(), result.memberCardId(), result.cardType(),
                result.operationType(), result.status(), result.message());
        return result;
    }

    private static RechargeCardOperationRecordResponse operationRecordFromWebhook(
            RechargeCardTransferStatusWebhook webhook) {
        return new RechargeCardOperationRecordResponse(
                webhook.memberCardOperationRecordId(),
                webhook.memberCardId(),
                webhook.cardType(),
                webhook.operationType(),
                webhook.amount(),
                webhook.currencyCode(),
                webhook.balance(),
                webhook.status(),
                webhook.message(),
                null,
                webhook.updateTime() == null ? null : webhook.updateTime().toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    private static void validateSharedCardLimit(
            Long expectedOperationRecordId,
            Long expectedMemberCardId,
            Long actualOperationRecordId,
            Long actualMemberCardId,
            String actualCardType,
            String actualOperationType,
            String status,
            String message) {
        if (!Objects.equals(expectedOperationRecordId, actualOperationRecordId)) {
            throw new IllegalStateException("Unexpected operation record in CARD_LIMIT_STATUS webhook: "
                    + actualOperationRecordId);
        }
        if (!Objects.equals(expectedMemberCardId, actualMemberCardId)) {
            throw new IllegalStateException("Unexpected member card for operation "
                    + expectedOperationRecordId + ": " + actualMemberCardId);
        }
        if (!"SHARED".equalsIgnoreCase(actualCardType)) {
            throw new IllegalStateException("Unexpected card type for operation "
                    + expectedOperationRecordId + ": " + actualCardType);
        }
        if (!"MODIFY_LIMITS".equalsIgnoreCase(actualOperationType)) {
            throw new IllegalStateException("Unexpected operation type for "
                    + expectedOperationRecordId + ": " + actualOperationType);
        }
        if ("FAIL".equalsIgnoreCase(status)) {
            throw new IllegalStateException("CARD_LIMIT_STATUS operation failed: "
                    + expectedOperationRecordId + ", " + message);
        }
    }

    private static void awaitSharedAccountTransaction(String transactionId, Long accountId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalStateException("Shared-account transaction ID is missing");
        }
        String status = null;
        TransactionWebhook settlementWebhook = SETTLEMENT_WEBHOOKS.get(transactionId);
        if (settlementWebhook != null) {
            LOGGER.info("Keeping CARD_SETTLE_STATUS separate while waiting for shared-account transactionId={} "
                            + "fund webhook: settleStatus={} settleTime={}",
                    transactionId, settlementWebhook.settleStatus(), settlementWebhook.settleTime());
        }
        CompletableFuture<TransactionWebhook> webhookFuture = FUND_TRANSACTION_WEBHOOKS.computeIfAbsent(
                transactionId, ignored -> new CompletableFuture<>());
        try {
            TransactionWebhook webhook = awaitWebhook(
                    webhookFuture, "FUND_TRANSACTION transactionId=" + transactionId);
            if (webhook != null) {
                if ("SUCCESS".equalsIgnoreCase(webhook.status())) {
                    return;
                }
                throw new IllegalStateException("Shared-account transaction failed: " + transactionId);
            }
        } catch (TimeoutException exception) {
            PageResultEx<SharedAccountTransactionResponse, Object> result = authorizedClient().sharedAccounts().transactions(
                    new SharedAccountTransactionsRequest(1, 1, Long.valueOf(transactionId), accountId, null, null, null));
            if (result.list() != null && !result.list().isEmpty()) {
                SharedAccountTransactionResponse transaction = result.list().get(0);
                status = transaction.status();
                LOGGER.info("Shared-account transaction status transactionId={} accountId={} status={} processStatus={} accountBalance={}",
                        transactionId, accountId, transaction.status(), transaction.processStatus(), transaction.accountBalance());
                if ("SUCCESS".equalsIgnoreCase(status)) {
                    return;
                }
                if ("FAIL".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Shared-account transaction failed: " + transactionId);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for shared-account transaction webhook", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Shared-account transaction webhook failed", exception.getCause());
        } finally {
            FUND_TRANSACTION_WEBHOOKS.remove(transactionId, webhookFuture);
        }
        throw new IllegalStateException("Shared-account transaction " + transactionId
                + " did not reach SUCCESS; current status=" + status);
    }

    private static boolean isUnauthorized(LuminalApiException exception) {
        return exception.httpStatus() == 401 || exception.apiCode() != null && exception.apiCode() == 401;
    }

    private static void failOnWebhookError() {
        Throwable error = WEBHOOK_ERROR.get();
        if (error != null) {
            throw new IllegalStateException("Shared-card webhook failed", error);
        }
    }

    private static <T> T awaitWebhook(CompletableFuture<T> future, String operation)
            throws InterruptedException, ExecutionException, TimeoutException {
        long startedAt = System.nanoTime();
        long deadline = startedAt + TimeUnit.SECONDS.toNanos(WEBHOOK_QUERY_TIMEOUT_SECONDS);
        while (true) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new TimeoutException("Timed out waiting for " + operation + " webhook");
            }
            try {
                return future.get(Math.min(remainingNanos,
                        TimeUnit.SECONDS.toNanos(WEBHOOK_WAIT_LOG_INTERVAL_SECONDS)), TimeUnit.NANOSECONDS);
            } catch (TimeoutException exception) {
                if (System.nanoTime() >= deadline) {
                    throw exception;
                }
                String[] parts = operation.split(" ", 2);
                String event = parts[0];
                String correlation = parts.length > 1 ? parts[1].replaceFirst("^[a-zA-Z]+Id=", "") : operation;
                LOGGER.info("Waiting for {} transactionId={} webhook, elapsed={}s", event, correlation,
                        TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt));
            }
        }
    }

    private static void sleep(long millis, String operation) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + operation, exception);
        }
    }

    private static void ensureTokenFlowReady() {
        if (!TOKEN_FLOW_READY) {
            CACHED_TOKEN = issueFreshToken();
            TOKEN_FLOW_READY = true;
        }
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    private static String required(String environmentName, String propertyName) {
        String value = configured(environmentName, propertyName, null);
        if (value == null) {
            throw new IllegalStateException("Configure " + environmentName + " or -D" + propertyName);
        }
        return value;
    }

    private static Long requiredLong(String environmentName, String propertyName) {
        String value = required(environmentName, propertyName);
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(environmentName + " must be a long", exception);
        }
    }

    private static Long configuredLong(String environmentName, String propertyName, Long defaultValue) {
        String value = configured(environmentName, propertyName, String.valueOf(defaultValue));
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(environmentName + " must be a long", exception);
        }
    }

    private static Long optionalLong(String environmentName, String propertyName) {
        String value = configured(environmentName, propertyName, null);
        return value == null ? null : requiredLong(environmentName, propertyName);
    }

    private static BigDecimal requiredDecimal(String environmentName, String propertyName) {
        String value = required(environmentName, propertyName);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(environmentName + " must be a decimal", exception);
        }
    }

    private static BigDecimal optionalDecimal(String environmentName, String propertyName) {
        String value = configured(environmentName, propertyName, null);
        return value == null ? null : requiredDecimal(environmentName, propertyName);
    }

    private static String configured(String environmentName, String propertyName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentName);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String readPrivateKeyPem() {
        String resource = "/org/luminal/openapi/sdk/integration/rsa-private-key.pem";
        try (InputStream input = ShareCardSandboxOpenApiIntegrationTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read test private key", exception);
        }
    }
}
