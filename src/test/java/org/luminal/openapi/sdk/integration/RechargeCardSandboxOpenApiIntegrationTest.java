package org.luminal.openapi.sdk.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.luminal.openapi.sdk.LuminalApiException;
import org.luminal.openapi.sdk.LuminalOpenApiClient;
import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;
import org.luminal.openapi.sdk.model.CardGroupModels.CardGroupCreateRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCardPageRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCountryResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCreateRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderDetailResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderModifyRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderPageRequest;
import org.luminal.openapi.sdk.model.CardModels.CardBinResponse;
import org.luminal.openapi.sdk.model.CardModels.CardBinsRequest;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;
import org.luminal.openapi.sdk.model.CardModels.CardLimitResponse;
import org.luminal.openapi.sdk.model.CardModels.CardLimitUpdateRequest;
import org.luminal.openapi.sdk.model.CardModels.CardTransactionResponse;
import org.luminal.openapi.sdk.model.CardModels.CardTransactionsRequest;
import org.luminal.openapi.sdk.model.CardModels.IssueCardDetailsRequest;
import org.luminal.openapi.sdk.model.CardModels.IssueCardDetailsResponse;
import org.luminal.openapi.sdk.model.CardModels.IssueCardRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardPageRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardRechargeRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardResponse;
import org.luminal.openapi.sdk.model.CardModels.MemberCardWithdrawRequest;
import org.luminal.openapi.sdk.model.CardModels.RechargeCardOperationRecordRequest;
import org.luminal.openapi.sdk.model.CardModels.RechargeCardOperationRecordResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;
import org.luminal.openapi.sdk.model.OpenApiEnums.CardType;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenResult;
import org.luminal.openapi.sdk.model.WebhookModels.CardOpenStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.CardStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.RechargeCardTransferStatusWebhook;
import org.luminal.openapi.sdk.model.WebhookModels.TransactionWebhook;
import org.luminal.openapi.sdk.webhook.WebhookEvent;
import org.luminal.openapi.sdk.webhook.WebhookEventType;
import org.luminal.openapi.sdk.webhook.WebhookVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ordered end-to-end tests for the recharge-card Sandbox flow.
 *
 * <p>The class is intentionally independent from {@link ShareCardSandboxOpenApiIntegrationTest} so it can be run by itself
 * with recharge-card-specific card groups, BINs, amounts, operation records, and webhook correlation.</p>
 */
@TestMethodOrder(OrderAnnotation.class)
class RechargeCardSandboxOpenApiIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RechargeCardSandboxOpenApiIntegrationTest.class);
    private static final String DEFAULT_BASE_URL = "https://sandbox-openapi.luminalads.com";
    private static final String DEFAULT_APP_ID = "lpsha6pj5mwsb7tz";
    private static final String DEFAULT_APP_SECRET = "P11g59PXY33JjqL4CRJ2Oz3nfsjsWRKe";
    private static final String TEST_CARD_BIN = "578391";
    private static final String DEFAULT_WEBHOOK_HOST = "0.0.0.0";
    private static final int DEFAULT_WEBHOOK_PORT = 18082;
    private static final String DEFAULT_WEBHOOK_PATH = "/luminal-open-api-webhook";
    private static final int DEFAULT_ASYNC_TIMEOUT_SECONDS = 30;
    private static final int WEBHOOK_WAIT_LOG_INTERVAL_SECONDS = 10;
    private static final int MAX_WEBHOOK_BODY_BYTES = 1024 * 1024;
    private static final long POLL_INTERVAL_MILLIS = 2_000L;
    private static final String CURRENT_CARD_TYPE = CardType.RECHARGE.name();
    private static final String LIMIT_OPERATION_TYPE = "MODIFY_LIMITS";

    private static final LuminalOpenApiClient PUBLIC_CLIENT = new LuminalOpenApiClient(configured(
            "LUMINAL_OPEN_API_BASE_URL", "luminal.openApi.baseUrl", DEFAULT_BASE_URL));
    private static final LuminalOpenApiClient CLIENT = new LuminalOpenApiClient(
            configured("LUMINAL_OPEN_API_BASE_URL", "luminal.openApi.baseUrl", DEFAULT_BASE_URL),
            configured("LUMINAL_OPEN_API_APP_ID", "luminal.openApi.appId", DEFAULT_APP_ID),
            configured("LUMINAL_OPEN_API_APP_SECRET", "luminal.openApi.appSecret", DEFAULT_APP_SECRET),
            null, 1, configured("LUMINAL_OPEN_API_LOCALE", "luminal.openApi.locale", "en"), true);

    private static final ConcurrentMap<Long, CompletableFuture<CardOpenStatusWebhook>> CARD_OPEN_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, CardStatusWebhook> CARD_STATUS_EVENTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, CompletableFuture<RechargeCardTransferStatusWebhook>> TRANSFER_WEBHOOKS =
            new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, TransactionWebhook> TRANSACTION_EVENTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, TransactionWebhook> SETTLEMENT_WEBHOOKS = new ConcurrentHashMap<>();
    private static final AtomicReference<Throwable> WEBHOOK_ERROR = new AtomicReference<>();

    private static HttpServer webhookServer;
    private static CardHolderDetailResponse cardHolderTemplate;
    private static List<CardHolderCountryResponse> cardHolderCountries;
    private static Long cardHolderId;
    private static Long issueCardHolderId;
    private static CardBinResponse cardBin;
    private static Long cardGroupId;
    private static Long issueTaskId;
    private static Long memberCardId;
    private static Long limitOperationRecordId;
    private static Long rechargeOperationRecordId;
    private static Long withdrawOperationRecordId;

    @BeforeAll
    static void startWebhookServer() {
        int port = configuredLong(
                "LUMINAL_OPEN_API_WEBHOOK_PORT", "luminal.openApi.webhookPort", DEFAULT_WEBHOOK_PORT).intValue();
        String host = configured("LUMINAL_OPEN_API_WEBHOOK_HOST", "luminal.openApi.webhookHost", DEFAULT_WEBHOOK_HOST);
        String path = normalizePath(configured(
                "LUMINAL_OPEN_API_WEBHOOK_PATH", "luminal.openApi.webhookPath", DEFAULT_WEBHOOK_PATH));
        try {
            webhookServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            webhookServer.createContext(path, RechargeCardSandboxOpenApiIntegrationTest::handleWebhook);
            webhookServer.start();
            LOGGER.info("Recharge-card webhook listener started: http://{}:{}{}", host, port, path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start recharge-card webhook listener", exception);
        }
    }

    @AfterAll
    static void stopWebhookServer() {
        if (webhookServer != null) {
            webhookServer.stop(0);
        }
    }

    @Test
    @Order(1)
    void getTokenFromSandbox() {
        OAuth2Token token = PUBLIC_CLIENT.auth().getToken(appId(), appSecret());
        assertNotNull(token);
        assertFalse(token.accessToken().isBlank());
    }

    @Test
    @Order(10)
    void listRechargeCardBinsFromSandbox() {
        assertNotNull(firstRechargeCardBinId());
    }

    @Test
    @Order(20)
    void createRechargeCardGroupFromSandbox() {
        assertNotNull(ensureRechargeCardGroupId());
    }

    @Test
    @Order(11)
    void listCardholderCountriesFromSandbox() {
        CardHolderCountryResponse country = selectedCardHolderCountry(existingCardHolderTemplate());
        assertNotNull(country.countryId());
        assertFalse(country.areaCode().isBlank());
        assertTrue(country.phoneMaxLength() > 0);
    }

    @Test
    @Order(13)
    void createCardholderRejectsBlankPhoneFromSandbox() {
        CardHolderCreateRequest profile = cardHolderProfile("Invalid blank local phone");
        CardHolderCreateRequest invalidRequest = new CardHolderCreateRequest(
                profile.lastName(), profile.firstName(), profile.birthDate(), profile.mail(), "",
                profile.areaCode(), profile.countryId(), profile.postalCode(), profile.state(),
                profile.city(), profile.addressLine1(), profile.addressLine2());

        LuminalApiException exception = assertThrows(
                LuminalApiException.class, () -> CLIENT.cardHolders().add(invalidRequest));

        assertEquals(200, exception.httpStatus());
        assertEquals(400, exception.apiCode());
    }

    @Test
    @Order(15)
    void createAndQueryCardholderFromSandbox() {
        Long id = ensureCardHolderId();
        CardHolderDetailResponse detail = CLIENT.cardHolders().detail(id);
        assertEquals("Sdk", detail.lastName());
        assertTrue(CLIENT.cardHolders().page(
                        new CardHolderPageRequest(1, 20, id, null, null, null, null)).list().stream()
                .anyMatch(item -> Objects.equals(id, item.cardHolderId())));
    }

    @Test
    @Order(16)
    void modifyCardholderFromSandbox() {
        Long id = ensureCardHolderId();
        CardHolderDetailResponse before = CLIENT.cardHolders().detail(id);
        String updatedAddressLine2 = "SDK address updated " + System.currentTimeMillis();
        boolean modified = false;
        try {
            CLIENT.cardHolders().modify(modifyRequest(before, before.phone(), updatedAddressLine2));
            modified = true;
            assertEquals(updatedAddressLine2, CLIENT.cardHolders().detail(id).addressLine2());
        } finally {
            if (modified) {
                CLIENT.cardHolders().modify(modifyRequest(before, before.phone(), before.addressLine2()));
                cardHolderTemplate = CLIENT.cardHolders().detail(id);
            }
        }
    }

    @Test
    @Order(17)
    void modifyCardholderRejectsBlankPhoneFromSandbox() {
        CardHolderDetailResponse before = CLIENT.cardHolders().detail(ensureCardHolderId());
        Long id = before.cardHolderId();
        CardHolderModifyRequest invalidRequest = modifyRequest(before, "", before.addressLine2());

        LuminalApiException exception = assertThrows(
                LuminalApiException.class, () -> CLIENT.cardHolders().modify(invalidRequest));

        assertEquals(200, exception.httpStatus());
        assertEquals(400, exception.apiCode());
        CardHolderDetailResponse after = CLIENT.cardHolders().detail(id);
        assertEquals(before.countryId(), after.countryId());
        assertEquals(before.areaCode(), after.areaCode());
        assertEquals(before.phone(), after.phone());
        assertEquals(before.addressLine2(), after.addressLine2());
    }

    @Test
    @Order(18)
    void reuseExistingCardholderForRechargeCardIssueFromSandbox() {
        if (!supportsCustomCardholder(firstRechargeCardBin())) {
            LOGGER.info("Recharge-card BIN {} does not support an explicit cardholder; cardHolderId will be omitted",
                    firstRechargeCardBin().cardBin());
            return;
        }
        CardHolderDetailResponse existing = existingCardHolderTemplate();
        Long expectedId = existing == null ? ensureCardHolderId() : existing.cardHolderId();
        assertEquals(expectedId, cardHolderIdForIssue());
    }

    @Test
    @Order(30)
    void issueRechargeCardFromSandbox() {
        assertNotNull(issueRechargeCardTaskId());
    }

    @Test
    @Order(40)
    void waitForRechargeCardOpenFromSandbox() {
        assertNotNull(ensureRechargeCardId());
    }

    @Test
    @Order(50)
    void listRechargeCardsFromSandbox() {
        Long cardId = ensureRechargeCardId();
        PageResultEx<MemberCardResponse, Object> result = CLIENT.cards().list(
                new MemberCardPageRequest(1, 20, cardId, null, null, "RECHARGE", null,
                        List.of(ensureRechargeCardGroupId())));
        assertTrue(result.list().stream().anyMatch(item -> Objects.equals(cardId, item.memberCardId())));
    }

    @Test
    @Order(55)
    void listCardsAssociatedWithCardholderFromSandbox() {
        if (!supportsCustomCardholder(firstRechargeCardBin())) {
            LOGGER.info("Skipping recharge-card/cardholder association check because BIN {} does not support cardholders",
                    firstRechargeCardBin().cardBin());
            return;
        }
        Long cardId = ensureRechargeCardId();
        assertTrue(CLIENT.cardHolders().associatedCards(
                        new CardHolderCardPageRequest(1, 20, cardHolderIdForIssue(), cardId)).list().stream()
                .anyMatch(item -> Objects.equals(cardId, item.memberCardId())));
    }

    @Test
    @Order(60)
    void getRechargeCardCvvFromSandbox() {
        assertNotNull(CLIENT.cards().cvv(new CardIdRequest(ensureRechargeCardId())));
    }

    @Test
    @Order(70)
    void getRechargeCardLimitFromSandbox() {
        CardLimitResponse limit = CLIENT.cards().limit(new CardIdRequest(ensureRechargeCardId()));
        assertNotNull(limit);
        assertNotNull(limit.balance());
    }

    @Test
    @Order(80)
    void modifyRechargeCardLimitFromSandbox() {
        CardBinResponse selectedBin = firstRechargeCardBin();
        if (!supportsCardLimit(selectedBin)) {
            LOGGER.info("Recharge-card BIN {} does not support limit modification; skipping limit update",
                    selectedBin.cardBin());
            return;
        }
        CardLimitUpdateRequest request = new CardLimitUpdateRequest(
                ensureRechargeCardId(), "RECHARGE",
                configuredDecimal("LUMINAL_OPEN_API_RECHARGE_DAILY_LIMIT",
                        "luminal.openApi.rechargeDailyLimit", "100.00"),
                configuredDecimal("LUMINAL_OPEN_API_RECHARGE_MONTH_LIMIT",
                        "luminal.openApi.rechargeMonthLimit", "1000.00"), null);
        limitOperationRecordId = Objects.requireNonNull(
                CLIENT.cards().modifyLimitAsync(request), "Limit operation-record ID is missing");
        RechargeCardOperationRecordResponse result = awaitOperation(limitOperationRecordId, "MODIFY_LIMITS");
        assertTrue("SUCCESS".equalsIgnoreCase(result.status()));
    }

    @Test
    @Order(90)
    void rechargeCardFromSandbox() {
        MemberCardRechargeRequest request = new MemberCardRechargeRequest(
                ensureRechargeCardId(), configuredDecimal("LUMINAL_OPEN_API_RECHARGE_AMOUNT",
                "luminal.openApi.rechargeAmount", "10.00"), "java-sdk Sandbox recharge");
        rechargeOperationRecordId = Objects.requireNonNull(
                CLIENT.cards().recharge(request), "Recharge operation-record ID is missing");
        RechargeCardOperationRecordResponse result = awaitOperation(rechargeOperationRecordId, "RECHARGE");
        assertTrue("SUCCESS".equalsIgnoreCase(result.status()));
    }

    @Test
    @Order(100)
    void queryRechargeOperationRecordFromSandbox() {
        RechargeCardOperationRecordResponse result = CLIENT.cards().operationRecord(
                new RechargeCardOperationRecordRequest(Objects.requireNonNull(
                        rechargeOperationRecordId, "Recharge operation has not run")));
        assertTrue("RECHARGE".equalsIgnoreCase(result.operationType()));
        assertTrue("SUCCESS".equalsIgnoreCase(result.status()));
    }

    @Test
    @Order(110)
    void withdrawCardFromSandbox() {
        MemberCardWithdrawRequest request = new MemberCardWithdrawRequest(
                ensureRechargeCardId(), configuredDecimal("LUMINAL_OPEN_API_WITHDRAW_AMOUNT",
                "luminal.openApi.withdrawAmount", "19.90"), "java-sdk Sandbox withdrawal");
        withdrawOperationRecordId = Objects.requireNonNull(
                CLIENT.cards().withdraw(request), "Withdraw operation-record ID is missing");
        RechargeCardOperationRecordResponse result = awaitOperation(withdrawOperationRecordId, "WITHDRAW");
        assertTrue("SUCCESS".equalsIgnoreCase(result.status()));
    }

    @Test
    @Order(120)
    void queryWithdrawOperationRecordFromSandbox() {
        RechargeCardOperationRecordResponse result = CLIENT.cards().operationRecord(
                new RechargeCardOperationRecordRequest(Objects.requireNonNull(
                        withdrawOperationRecordId, "Withdraw operation has not run")));
        assertTrue("WITHDRAW".equalsIgnoreCase(result.operationType()));
        assertTrue("SUCCESS".equalsIgnoreCase(result.status()));
    }

    @Test
    @Order(130)
    void listRechargeCardTransactionsFromSandbox() {
        Long cardId = ensureRechargeCardId();
        PageResultEx<CardTransactionResponse, Object> result = CLIENT.cards().transactions(
                new CardTransactionsRequest(1, 20, CURRENT_CARD_TYPE, cardId, null));
        assertNotNull(result);
        assertNotNull(result.list());
        TransactionWebhook webhook = TRANSACTION_EVENTS.get(cardId);
        if (webhook != null) {
            assertTrue(isCurrentRechargeCard(webhook.cardType(), parseLong(webhook.memberCardId())));
        }
        result.list().stream()
                .map(CardTransactionResponse::memberCardTransactionId)
                .map(SETTLEMENT_WEBHOOKS::get)
                .filter(Objects::nonNull)
                .forEach(settlement -> assertTrue(isCurrentRechargeCard(
                        settlement.cardType(), parseLong(settlement.memberCardId()))));
    }

    @Test
    @Order(140)
    void freezeRechargeCardFromSandbox() {
        Long cardId = ensureRechargeCardId();
        CARD_STATUS_EVENTS.remove(cardId);
        assertTrue(CLIENT.cards().freeze(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "FREEZE");
    }

    @Test
    @Order(150)
    void unfreezeRechargeCardFromSandbox() {
        Long cardId = ensureRechargeCardId();
        CARD_STATUS_EVENTS.remove(cardId);
        assertTrue(CLIENT.cards().unfreeze(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "ACTIVE");
    }

    @Test
    @Order(900)
    void cancelRechargeCardFromSandbox() {
        Long cardId = ensureRechargeCardId();
        CARD_STATUS_EVENTS.remove(cardId);
        assertTrue(CLIENT.cards().cancel(new CardIdRequest(cardId)));
        awaitCardStatus(cardId, "CANCEL");
    }

    private static synchronized CardBinResponse firstRechargeCardBin() {
        if (cardBin == null) {
            String expectedBin = configured(
                    "LUMINAL_OPEN_API_RECHARGE_CARD_BIN", "luminal.openApi.rechargeCardBin", TEST_CARD_BIN);
            PageResultEx<CardBinResponse, Object> bins = CLIENT.cards().bins(
                    new CardBinsRequest(1, 50, "RECHARGE", null, expectedBin, null));
            cardBin = bins.list().stream()
                    .filter(item -> expectedBin.equals(item.cardBin()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Recharge-card BIN not found: " + expectedBin));
        }
        return cardBin;
    }

    private static synchronized Long firstRechargeCardBinId() {
        return Objects.requireNonNull(firstRechargeCardBin().cardBinId(), "Recharge-card BIN ID is missing");
    }

    private static boolean supportsCustomCardholder(CardBinResponse cardBin) {
        return cardBin != null && Objects.equals(1, cardBin.customCardholder());
    }

    private static boolean supportsCardLimit(CardBinResponse cardBin) {
        return cardBin != null && Objects.equals(1, cardBin.canLimit());
    }

    private static synchronized Long ensureRechargeCardGroupId() {
        if (cardGroupId == null) {
            var created = CLIENT.cardGroups().create(
                    new CardGroupCreateRequest(uniqueName("sdk-sandbox-recharge-group"), "RECHARGE"));
            cardGroupId = Objects.requireNonNull(created.cardGroupId(), "Recharge-card group ID is missing");
        }
        return cardGroupId;
    }

    private static synchronized Long issueRechargeCardTaskId() {
        if (issueTaskId == null) {
            CardBinResponse selectedBin = firstRechargeCardBin();
            IssueCardRequest request = new IssueCardRequest(
                    1,
                    firstRechargeCardBinId(),
                    ensureRechargeCardGroupId(),
                    uniqueName("sdk-sandbox-recharge-card"),
                    "RECHARGE",
                    null,
                    configuredDecimal("LUMINAL_OPEN_API_RECHARGE_DAILY_LIMIT",
                            "luminal.openApi.rechargeDailyLimit", "100.00"),
                    configuredDecimal("LUMINAL_OPEN_API_RECHARGE_MONTH_LIMIT",
                            "luminal.openApi.rechargeMonthLimit", "1000.00"),
                    configuredDecimal("LUMINAL_OPEN_API_RECHARGE_ISSUE_AMOUNT",
                            "luminal.openApi.rechargeIssueAmount", "10.00"),
                    supportsCustomCardholder(selectedBin) ? cardHolderIdForIssue() : null);
            issueTaskId = Objects.requireNonNull(
                    CLIENT.cards().issue(request, privateKey()), "Recharge-card issue task ID is missing");
        }
        return issueTaskId;
    }

    private static synchronized Long ensureCardHolderId() {
        if (cardHolderId == null) {
            cardHolderId = Objects.requireNonNull(
                    CLIENT.cardHolders().add(cardHolderProfile("Created by Java SDK Sandbox test")),
                    "Cardholder ID is missing");
        }
        return cardHolderId;
    }

    private static synchronized Long cardHolderIdForIssue() {
        if (issueCardHolderId == null) {
            CardHolderDetailResponse existing = existingCardHolderTemplate();
            if (existing != null && existing.cardHolderId() != null) {
                issueCardHolderId = existing.cardHolderId();
            } else if (cardHolderId != null) {
                issueCardHolderId = cardHolderId;
            } else {
                throw new IllegalStateException("Sandbox has no existing cardholder available for card issuance");
            }
            LOGGER.info("Reusing Sandbox cardholder for recharge-card issuance: {}", issueCardHolderId);
        }
        return issueCardHolderId;
    }

    private static CardHolderModifyRequest modifyRequest(
            CardHolderDetailResponse profile, String phone, String addressLine2) {
        return new CardHolderModifyRequest(
                profile.cardHolderId(), profile.lastName(), profile.firstName(), profile.birthDate(), profile.mail(),
                phone, profile.areaCode(), profile.countryId(), profile.postalCode(), profile.state(), profile.city(),
                profile.addressLine1(), addressLine2);
    }

    private static CardHolderCreateRequest cardHolderProfile(String addressLine2) {
        long suffix = System.currentTimeMillis() % 1_000_000L;
        CardHolderDetailResponse template = existingCardHolderTemplate();
        CardHolderCountryResponse country = selectedCardHolderCountry(template);
        String localPhone = localPhone(country, suffix);
        return new CardHolderCreateRequest(
                "Sdk",
                "Sandbox" + alphabeticSuffix(suffix),
                LocalDate.of(1990, 1, 15),
                "sdk-sandbox-" + suffix + "@example.com",
                localPhone,
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
        if (cardHolderCountries == null) {
            List<CardHolderCountryResponse> countries = CLIENT.cardHolders().countries();
            if (countries == null || countries.isEmpty()) {
                throw new IllegalStateException("cardHolders().countries() returned no countries");
            }
            cardHolderCountries = List.copyOf(countries);
        }
        return cardHolderCountries;
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
        if (cardHolderTemplate == null) {
            cardHolderTemplate = CLIENT.cardHolders()
                    .page(new CardHolderPageRequest(1, 1, null, null, null, null, null)).list().stream()
                    .map(item -> CLIENT.cardHolders().detail(item.cardHolderId()))
                    .findFirst()
                    .orElse(null);
        }
        return cardHolderTemplate;
    }

    private static synchronized Long ensureRechargeCardId() {
        if (memberCardId != null) {
            return memberCardId;
        }
        Long taskId = issueRechargeCardTaskId();
        failOnWebhookError();
        CompletableFuture<CardOpenStatusWebhook> future =
                CARD_OPEN_WEBHOOKS.computeIfAbsent(taskId, ignored -> new CompletableFuture<>());
        try {
            CardOpenStatusWebhook webhook = awaitWebhook(future, "CARD_OPEN_STATUS taskId=" + taskId);
            memberCardId = cardIdFromOpenWebhook(taskId, webhook);
        } catch (TimeoutException exception) {
            memberCardId = issuedCardIdFromDetails(taskId, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for CARD_OPEN_STATUS webhook", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("CARD_OPEN_STATUS webhook failed", exception.getCause());
        } finally {
            CARD_OPEN_WEBHOOKS.remove(taskId, future);
        }
        return memberCardId;
    }

    private static RechargeCardOperationRecordResponse awaitOperation(Long operationRecordId, String expectedType) {
        failOnWebhookError();
        CompletableFuture<RechargeCardTransferStatusWebhook> future =
                TRANSFER_WEBHOOKS.computeIfAbsent(operationRecordId, ignored -> new CompletableFuture<>());
        try {
            RechargeCardTransferStatusWebhook webhook = awaitWebhook(
                    future, expectedType + " operationRecordId=" + operationRecordId);
            validateTransfer(operationRecordId, memberCardId, expectedType,
                    webhook.memberCardOperationRecordId(), webhook.memberCardId(), webhook.cardType(),
                    webhook.operationType(), webhook.status(), webhook.message());
            return operationRecordFromWebhook(webhook);
        } catch (TimeoutException exception) {
            return queryFinalOperationRecord(operationRecordId, expectedType, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + expectedType + " webhook", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(expectedType + " webhook failed", exception.getCause());
        } finally {
            TRANSFER_WEBHOOKS.remove(operationRecordId, future);
        }
    }

    private static Long cardIdFromOpenWebhook(Long taskId, CardOpenStatusWebhook webhook) {
        if (!Objects.equals(taskId, webhook.cardApplyTaskId())) {
            throw new IllegalStateException("Unexpected card task in webhook: " + webhook.cardApplyTaskId());
        }
        CardOpenResult result = webhook.list() == null ? null : webhook.list().stream()
                .filter(item -> item.memberCardId() != null)
                .findFirst()
                .orElse(null);
        if (result == null) {
            throw new IllegalStateException("CARD_OPEN_STATUS webhook contains no card result");
        }
        if (!"SUCCESS".equalsIgnoreCase(result.cardStatus())) {
            throw new IllegalStateException("Recharge-card opening failed: " + result.cardStatus() + ", "
                    + result.message());
        }
        return result.memberCardId();
    }

    private static Long issuedCardIdFromDetails(Long taskId, TimeoutException timeout) {
        List<IssueCardDetailsResponse> details = CLIENT.cards().issueDetails(new IssueCardDetailsRequest(taskId));
        IssueCardDetailsResponse detail = details.stream()
                .filter(item -> item.memberCardId() != null)
                .filter(item -> item.cardStatus() != null && !"APPLYING".equalsIgnoreCase(item.cardStatus()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CARD_OPEN_STATUS webhook timed out after "
                        + asyncTimeoutSeconds() + " seconds and recharge card is not ready for task " + taskId,
                        timeout));
        if (isFailedCardStatus(detail.cardStatus())) {
            throw new IllegalStateException("Recharge-card opening failed: " + detail.cardStatus() + ", "
                    + detail.message());
        }
        return detail.memberCardId();
    }

    private static RechargeCardOperationRecordResponse queryFinalOperationRecord(
            Long operationRecordId, String expectedType, TimeoutException webhookTimeout) {
        RechargeCardOperationRecordResponse result = CLIENT.cards().operationRecord(
                new RechargeCardOperationRecordRequest(operationRecordId));
        if (!isFinalStatus(result.status())) {
            String message = expectedType + " webhook timed out after " + asyncTimeoutSeconds()
                    + " seconds and operation record is not final: operationRecordId=" + operationRecordId
                    + ", status=" + result.status();
            throw webhookTimeout == null ? new IllegalStateException(message)
                    : new IllegalStateException(message, webhookTimeout);
        }
        validateTransfer(operationRecordId, memberCardId, expectedType,
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

    private static void validateTransfer(
            Long expectedOperationRecordId,
            Long expectedMemberCardId,
            String expectedType,
            Long actualOperationRecordId,
            Long actualMemberCardId,
            String actualCardType,
            String actualType,
            String status,
            String message) {
        if (!Objects.equals(expectedOperationRecordId, actualOperationRecordId)) {
            throw new IllegalStateException("Unexpected operation record in webhook: " + actualOperationRecordId);
        }
        if (!isCurrentCardType(actualCardType)) {
            throw new IllegalStateException("Unexpected card type for operation "
                    + expectedOperationRecordId + ": " + actualCardType);
        }
        if (!Objects.equals(expectedMemberCardId, actualMemberCardId)) {
            throw new IllegalStateException("Unexpected member card for operation "
                    + expectedOperationRecordId + ": " + actualMemberCardId);
        }
        if (!expectedType.equalsIgnoreCase(actualType)) {
            throw new IllegalStateException("Unexpected operation type for "
                    + expectedOperationRecordId + ": " + actualType);
        }
        if ("FAIL".equalsIgnoreCase(status)) {
            throw new IllegalStateException(expectedType + " operation failed: "
                    + expectedOperationRecordId + ", " + message);
        }
    }

    private static void awaitCardStatus(Long cardId, String expectedStatus) {
        Instant deadline = deadline();
        while (Instant.now().isBefore(deadline)) {
            failOnWebhookError();
            CardStatusWebhook webhook = CARD_STATUS_EVENTS.get(cardId);
            if (webhook != null && expectedStatus.equalsIgnoreCase(webhook.cardStatus())) {
                return;
            }
            String status = currentCardStatus(cardId);
            if (expectedStatus.equalsIgnoreCase(status)) {
                return;
            }
            sleep("card status " + expectedStatus);
        }
        throw new IllegalStateException("Card " + cardId + " did not reach status " + expectedStatus);
    }

    private static String currentCardStatus(Long cardId) {
        PageResultEx<MemberCardResponse, Object> result = CLIENT.cards().list(
                new MemberCardPageRequest(1, 1, cardId, null, null, "RECHARGE", null, null));
        return result.list().stream()
                .filter(item -> Objects.equals(cardId, item.memberCardId()))
                .map(MemberCardResponse::status)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recharge card not found: " + cardId));
    }

    private static void handleWebhook(HttpExchange exchange) throws IOException {
        int status = 200;
        String response = "ok";
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                status = 405;
                response = "method not allowed";
            } else {
                byte[] rawBody = readLimited(exchange.getRequestBody());
                String eventHeader = exchange.getRequestHeaders().getFirst("event");
                String eventId = exchange.getRequestHeaders().getFirst("event_id");
                String signature = exchange.getRequestHeaders().getFirst("sign");
                WebhookEventType type = parseWebhookType(eventHeader);
                if (type == null || !isRechargeCardWebhook(type)) {
                    LOGGER.info("Ignoring non-recharge-card webhook event={}", eventHeader);
                } else {
                    Object payload;
                    String publicKey = configured(
                            "LUMINAL_OPEN_API_WEBHOOK_PUBLIC_KEY", "luminal.openApi.webhookPublicKey", null);
                    if (publicKey == null) {
                        payload = parseUnchecked(type, rawBody);
                    } else {
                        WebhookEvent<?> event = WebhookVerifier.parse(
                                eventHeader, eventId, rawBody, signature, publicKey.replace("\\n", "\n"));
                        payload = event.payload();
                    }
                    recordWebhook(type, payload);
                    LOGGER.info("Received recharge-card webhook event={} eventId={}", type, eventId);
                }
            }
        } catch (Exception exception) {
            WEBHOOK_ERROR.compareAndSet(null, exception);
            LOGGER.error("Recharge-card webhook processing failed", exception);
            status = 400;
            response = "invalid webhook";
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static Object parseUnchecked(WebhookEventType type, byte[] rawBody) throws IOException {
        return switch (type) {
            case CARD_OPEN_STATUS -> JsonSupport.readValue(rawBody, CardOpenStatusWebhook.class);
            case CARD_STATUS -> JsonSupport.readValue(rawBody, CardStatusWebhook.class);
            case CARD_RECHARGE_STATUS, CARD_WITHDRAW_STATUS, CARD_LIMIT_STATUS ->
                    JsonSupport.readValue(rawBody, RechargeCardTransferStatusWebhook.class);
            case CARD_TRANSACTIONS, CARD_SETTLE_STATUS, SHARE_ACCOUNT_FUND_TRANSACTIONS ->
                    JsonSupport.readValue(rawBody, TransactionWebhook.class);
            case SHARED_ACCOUNT_OPEN_STATUS -> null;
        };
    }

    private static void recordWebhook(WebhookEventType type, Object payload) {
        switch (type) {
            case CARD_OPEN_STATUS -> {
                CardOpenStatusWebhook value = (CardOpenStatusWebhook) payload;
                Long taskId = value.cardApplyTaskId();
                if (taskId == null || (!Objects.equals(issueTaskId, taskId)
                        && !CARD_OPEN_WEBHOOKS.containsKey(taskId))) {
                    LOGGER.info("Ignoring CARD_OPEN_STATUS webhook for taskId={}", value.cardApplyTaskId());
                    return;
                }
                CARD_OPEN_WEBHOOKS.computeIfAbsent(
                        taskId, ignored -> new CompletableFuture<>()).complete(value);
            }
            case CARD_STATUS -> {
                CardStatusWebhook value = (CardStatusWebhook) payload;
                Long cardId = parseLong(value.memberCardId());
                if (memberCardId == null || cardId == null || !Objects.equals(memberCardId, cardId)) {
                    LOGGER.info("Ignoring CARD_STATUS webhook for cardId={}", value.memberCardId());
                    return;
                }
                CARD_STATUS_EVENTS.put(cardId, value);
            }
            case CARD_RECHARGE_STATUS, CARD_WITHDRAW_STATUS, CARD_LIMIT_STATUS -> {
                RechargeCardTransferStatusWebhook value = (RechargeCardTransferStatusWebhook) payload;
                Long operationRecordId = value.memberCardOperationRecordId();
                if (!isCurrentTransfer(type, value)) {
                    LOGGER.info("Ignoring {} webhook for operationRecordId={} cardId={} cardType={} operationType={}",
                            type, operationRecordId, value.memberCardId(), value.cardType(), value.operationType());
                    return;
                }
                TRANSFER_WEBHOOKS.computeIfAbsent(
                        operationRecordId, ignored -> new CompletableFuture<>()).complete(value);
            }
            case CARD_TRANSACTIONS -> {
                TransactionWebhook value = (TransactionWebhook) payload;
                Long cardId = parseLong(value.memberCardId());
                if (!isCurrentRechargeCard(value.cardType(), cardId)) {
                    LOGGER.info("Ignoring CARD_TRANSACTIONS webhook for cardId={} cardType={}",
                            value.memberCardId(), value.cardType());
                    return;
                }
                TRANSACTION_EVENTS.put(cardId, value);
            }
            case CARD_SETTLE_STATUS -> {
                TransactionWebhook value = (TransactionWebhook) payload;
                Long cardId = parseLong(value.memberCardId());
                Long transactionId = parseLong(value.memberCardTransactionId());
                if (transactionId == null || !isCurrentRechargeCard(value.cardType(), cardId)) {
                    LOGGER.info("Ignoring CARD_SETTLE_STATUS webhook for transactionId={} cardId={} cardType={}",
                            value.memberCardTransactionId(), value.memberCardId(), value.cardType());
                    return;
                }
                SETTLEMENT_WEBHOOKS.put(transactionId, value);
                LOGGER.info("Recorded recharge-card CARD_SETTLE_STATUS webhook transactionId={} settleStatus={} settleTime={}",
                        transactionId, value.settleStatus(), value.settleTime());
            }
            case SHARED_ACCOUNT_OPEN_STATUS, SHARE_ACCOUNT_FUND_TRANSACTIONS -> {
                // Not part of this recharge-card flow.
            }
        }
    }

    private static boolean isRechargeCardWebhook(WebhookEventType type) {
        return type == WebhookEventType.CARD_OPEN_STATUS
                || type == WebhookEventType.CARD_STATUS
                || type == WebhookEventType.CARD_RECHARGE_STATUS
                || type == WebhookEventType.CARD_WITHDRAW_STATUS
                || type == WebhookEventType.CARD_LIMIT_STATUS
                || type == WebhookEventType.CARD_TRANSACTIONS
                || type == WebhookEventType.CARD_SETTLE_STATUS;
    }

    private static WebhookEventType parseWebhookType(String eventHeader) {
        if (eventHeader == null || eventHeader.isBlank()) {
            throw new IllegalArgumentException("Webhook event header is missing");
        }
        try {
            return WebhookEventType.valueOf(eventHeader);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isCurrentTransfer(WebhookEventType eventType,
                                             RechargeCardTransferStatusWebhook webhook) {
        Long expectedOperationRecordId = currentOperationRecordId(eventType);
        return isCurrentRechargeCard(webhook.cardType(), webhook.memberCardId())
                && webhook.memberCardOperationRecordId() != null
                && (Objects.equals(expectedOperationRecordId, webhook.memberCardOperationRecordId())
                || TRANSFER_WEBHOOKS.containsKey(webhook.memberCardOperationRecordId()))
                && expectedOperationType(eventType).equalsIgnoreCase(webhook.operationType())
                && isFinalStatus(webhook.status());
    }

    private static boolean isCurrentRechargeCard(String cardType, Long cardId) {
        return memberCardId != null && cardId != null
                && isCurrentCardType(cardType) && Objects.equals(memberCardId, cardId);
    }

    private static boolean isCurrentCardType(String cardType) {
        return CURRENT_CARD_TYPE.equalsIgnoreCase(cardType);
    }

    private static Long currentOperationRecordId(WebhookEventType eventType) {
        return switch (eventType) {
            case CARD_RECHARGE_STATUS -> rechargeOperationRecordId;
            case CARD_WITHDRAW_STATUS -> withdrawOperationRecordId;
            case CARD_LIMIT_STATUS -> limitOperationRecordId;
            default -> null;
        };
    }

    private static String expectedOperationType(WebhookEventType eventType) {
        return switch (eventType) {
            case CARD_RECHARGE_STATUS -> "RECHARGE";
            case CARD_WITHDRAW_STATUS -> "WITHDRAW";
            case CARD_LIMIT_STATUS -> LIMIT_OPERATION_TYPE;
            default -> "";
        };
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

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8_192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_WEBHOOK_BODY_BYTES) {
                throw new IOException("Webhook body exceeds " + MAX_WEBHOOK_BODY_BYTES + " bytes");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static PrivateKey privateKey() {
        String resource = "/org/luminal/openapi/sdk/integration/rsa-private-key.pem";
        try (InputStream input = RechargeCardSandboxOpenApiIntegrationTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + resource);
            }
            return RsaSignatures.readPrivateKey(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read test private key", exception);
        }
    }

    private static Instant deadline() {
        return Instant.now().plus(Duration.ofSeconds(asyncTimeoutSeconds()));
    }

    private static long asyncTimeoutSeconds() {
        return configuredLong(
                "LUMINAL_OPEN_API_WEBHOOK_TIMEOUT_SECONDS", "luminal.openApi.webhookTimeoutSeconds",
                DEFAULT_ASYNC_TIMEOUT_SECONDS);
    }

    private static void failOnWebhookError() {
        Throwable error = WEBHOOK_ERROR.get();
        if (error != null) {
            throw new IllegalStateException("Recharge-card webhook failed", error);
        }
    }

    private static boolean isFinalStatus(String status) {
        return "SUCCESS".equalsIgnoreCase(status) || "FAIL".equalsIgnoreCase(status);
    }

    private static boolean isFailedCardStatus(String status) {
        return "FAIL".equalsIgnoreCase(status) || "CANCEL".equalsIgnoreCase(status)
                || "RISK_CANCEL".equalsIgnoreCase(status) || "ADMIN_CANCEL".equalsIgnoreCase(status);
    }

    private static String normalizePath(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    private static String appId() {
        return configured("LUMINAL_OPEN_API_APP_ID", "luminal.openApi.appId", DEFAULT_APP_ID);
    }

    private static String appSecret() {
        return configured("LUMINAL_OPEN_API_APP_SECRET", "luminal.openApi.appSecret", DEFAULT_APP_SECRET);
    }

    private static BigDecimal configuredDecimal(String environmentName, String propertyName, String defaultValue) {
        try {
            return new BigDecimal(configured(environmentName, propertyName, defaultValue));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(environmentName + " must be a decimal", exception);
        }
    }

    private static Long configuredLong(String environmentName, String propertyName, long defaultValue) {
        String value = configured(environmentName, propertyName, Long.toString(defaultValue));
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(environmentName + " must be a long", exception);
        }
    }

    private static Long optionalLong(String environmentName, String propertyName) {
        String value = configured(environmentName, propertyName, null);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(environmentName + " must be a long", exception);
        }
    }

    private static String configured(String environmentName, String propertyName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentName);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static <T> T awaitWebhook(CompletableFuture<T> future, String operation)
            throws InterruptedException, ExecutionException, TimeoutException {
        long startedAt = System.nanoTime();
        long deadline = startedAt + TimeUnit.SECONDS.toNanos(asyncTimeoutSeconds());
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
                LOGGER.info("Waiting for {} webhook, elapsed={}s", operation,
                        TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt));
            }
        }
    }

    private static void sleep(String operation) {
        try {
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + operation, exception);
        }
    }
}
