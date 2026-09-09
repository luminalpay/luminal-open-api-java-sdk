package org.luminal.openapi.sdk.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.LuminalOpenApiClient;
import org.luminal.openapi.sdk.model.CardModels.CardBinResponse;
import org.luminal.openapi.sdk.model.CardModels.CardBinsRequest;
import org.luminal.openapi.sdk.model.CardPoolModels.CardPoolRequest;
import org.luminal.openapi.sdk.model.CardPoolModels.CardPoolResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;
import org.luminal.openapi.sdk.model.SharedAccountModels.CreateSharedAccountRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountIdResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountPageRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent card-pool shared-account flow tests.
 *
 * <p>The pool-specific Sandbox integration class reuses the complete shared-card flow. These tests verify the
 * pool-based request and response wiring against a local HTTP server so they do not create Sandbox accounts during
 * the normal unit-test run.</p>
 */
class CardPoolSharedAccountApiTest {

    private static final String POOL_RESPONSE = "{\"code\":0,\"msg\":\"success\",\"data\":["
            + "{\"cardPoolId\":9001,\"poolName\":\"Main Pool\",\"availableCount\":1,"
            + "\"sharedAccountCount\":0,\"cardBins\":[\"4416\"],\"canApplyAccount\":1,\"canApply\":0}]}";
    private static final String BINS_RESPONSE = "{\"code\":0,\"msg\":\"success\",\"data\":"
            + "{\"total\":1,\"list\":[{\"cardBinId\":1001,\"cardPoolId\":9001,"
            + "\"poolName\":\"Main Pool\",\"cardBin\":\"4416\",\"cardType\":\"SHARED\"}],"
            + "\"extra\":null}}";
    private static final String CREATE_RESPONSE = "{\"code\":0,\"msg\":\"success\",\"data\":"
            + "{\"memberSharedAccountId\":2001}}";
    private static final String SHARED_ACCOUNTS_RESPONSE = "{\"code\":0,\"msg\":\"success\",\"data\":"
            + "{\"total\":1,\"list\":[{\"memberSharedAccountId\":2001,"
            + "\"accountName\":\"Main\",\"cardBinId\":1001,\"cardPoolId\":9001,"
            + "\"poolName\":\"Main Pool\"}],\"extra\":null}}";

    private HttpServer server;
    private LuminalOpenApiClient client;
    private List<CapturedRequest> requests;

    @BeforeEach
    void startServer() throws IOException {
        requests = Collections.synchronizedList(new ArrayList<>());
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        client = new LuminalOpenApiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), "test-token");
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void opensSharedAccountFromOneCachedPoolListResult() {
        List<CardPoolResponse> cachedPools = client.cardPools().list(new CardPoolRequest(null, null));
        assertEquals(1, cachedPools.size());

        CardPoolResponse selectedPool = cachedPools.get(0);
        PageResultEx<CardBinResponse, Object> bins = client.cards().bins(
                new CardBinsRequest(1, 20, selectedPool.cardPoolId(), "SHARED", null, null, null));
        CardBinResponse selectedBin = bins.list().get(0);

        SharedAccountIdResponse created = client.sharedAccounts().create(
                new CreateSharedAccountRequest(
                        null, selectedPool.cardPoolId(), new BigDecimal("100.00"), "Main"));

        assertEquals(9001L, selectedPool.cardPoolId());
        assertEquals(1001L, selectedBin.cardBinId());
        assertEquals(2001L, created.memberSharedAccountId());
        assertEquals(3, requests.size());

        CapturedRequest poolRequest = requests.get(0);
        assertEquals("/open-api/v1/cards/pools", poolRequest.path());
        assertEquals("Bearer test-token", poolRequest.authorization());
        assertFalse(poolRequest.body().contains("cardBinId"), poolRequest.body());

        CapturedRequest binsRequest = requests.get(1);
        assertEquals("/open-api/v1/cards/bins", binsRequest.path());
        assertTrue(binsRequest.body().contains("\"cardPoolId\":9001"), binsRequest.body());

        CapturedRequest createRequest = requests.get(2);
        assertEquals("/open-api/v1/shared-account/create", createRequest.path());
        assertTrue(createRequest.body().contains("\"cardPoolId\":9001"), createRequest.body());
        assertFalse(createRequest.body().contains("\"cardBinId\""), createRequest.body());
    }

    @Test
    void opensSharedAccountWithCardBinAndCardPoolTogether() {
        CardPoolResponse selectedPool = client.cardPools().list(new CardPoolRequest(null, null)).get(0);
        CardBinResponse selectedBin = client.cards().bins(
                new CardBinsRequest(1, 20, selectedPool.cardPoolId(), "SHARED", null, null, null)).list().get(0);

        assertEquals(selectedPool.cardPoolId(), selectedBin.cardPoolId());
        SharedAccountIdResponse created = client.sharedAccounts().create(
                new CreateSharedAccountRequest(
                        selectedBin.cardBinId(), selectedPool.cardPoolId(), new BigDecimal("100.00"), "Main with BIN"));

        assertEquals(2001L, created.memberSharedAccountId());
        assertEquals(3, requests.size());
        CapturedRequest createRequest = requests.get(2);
        assertEquals("/open-api/v1/shared-account/create", createRequest.path());
        assertTrue(createRequest.body().contains("\"cardBinId\":1001"), createRequest.body());
        assertTrue(createRequest.body().contains("\"cardPoolId\":9001"), createRequest.body());
    }

    @Test
    void listsSharedAccountsByPoolAndDecodesPoolFields() {
        PageResultEx<SharedAccountResponse, Object> result = client.sharedAccounts().list(
                new SharedAccountPageRequest(1, 20, null, null, 9001L));

        SharedAccountResponse account = result.list().get(0);
        assertEquals(9001L, account.cardPoolId());
        assertEquals("Main Pool", account.poolName());
        assertEquals(1, requests.size());
        assertEquals("/open-api/v1/shared-account/list", requests.get(0).path());
        assertTrue(requests.get(0).body().contains("\"cardPoolId\":9001"), requests.get(0).body());
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new CapturedRequest(path, exchange.getRequestHeaders().getFirst("Authorization"), body));

        String response = switch (path) {
            case "/open-api/v1/cards/pools" -> POOL_RESPONSE;
            case "/open-api/v1/cards/bins" -> BINS_RESPONSE;
            case "/open-api/v1/shared-account/create" -> CREATE_RESPONSE;
            case "/open-api/v1/shared-account/list" -> SHARED_ACCOUNTS_RESPONSE;
            default -> "{\"code\":404,\"msg\":\"not found\",\"data\":null}";
        };
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(path.startsWith("/open-api/v1/") ? 200 : 404, responseBytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
        }
    }

    private record CapturedRequest(String path, String authorization, String body) {
    }
}
