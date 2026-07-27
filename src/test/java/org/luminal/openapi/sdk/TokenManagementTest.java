package org.luminal.openapi.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenManagementTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void accessTokenIsCachedAndRefreshedAfterHalfLife() throws Exception {
        AtomicInteger authCalls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/open-api/v1/auth/token", exchange -> respondToken(exchange, authCalls.incrementAndGet()));
        server.start();

        LuminalOpenApiClient client = new LuminalOpenApiClient(
                baseUrl(), "app", "secret", null, 0, "en");

        assertEquals("token-1", client.accessToken());
        Thread.sleep(700L);
        assertEquals("token-2", client.accessToken());
        assertEquals(2, authCalls.get());
    }

    @Test
    void unauthorizedRequestRetriesWithFreshTokenAndLocaleHeader() throws Exception {
        assertUnauthorizedRetry(401);
    }

    @Test
    void businessUnauthorizedRequestRetriesWithFreshToken() throws Exception {
        assertUnauthorizedRetry(200);
    }

    private void assertUnauthorizedRetry(int firstStatus) throws Exception {
        AtomicInteger authCalls = new AtomicInteger();
        AtomicInteger protectedCalls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/open-api/v1/auth/token", exchange -> respondToken(exchange, authCalls.incrementAndGet()));
        server.createContext("/open-api/v1/cards/freeze",
                exchange -> respondFreeze(exchange, protectedCalls.incrementAndGet(), firstStatus));
        server.start();

        LuminalOpenApiClient client = new LuminalOpenApiClient(
                baseUrl(), "app", "secret", null, 1, "zh");

        client.cards().freeze(new CardIdRequest(1L));

        assertEquals(2, authCalls.get());
        assertEquals(2, protectedCalls.get());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respondToken(HttpExchange exchange, int issued) throws IOException {
        String body = "{\"code\":0,\"msg\":\"success\",\"data\":{"
                + "\"accessToken\":\"token-" + issued + "\","
                + "\"tokenType\":\"Bearer\","
                + "\"expiresTime\":" + (System.currentTimeMillis() + 1200L) + ","
                + "\"refreshToken\":\"refresh-" + issued + "\","
                + "\"scope\":null,\"jti\":\"jti-" + issued + "\"}}";
        writeJson(exchange, 200, body);
    }

    private void respondFreeze(HttpExchange exchange, int call, int firstStatus) throws IOException {
        assertEquals("zh", exchange.getRequestHeaders().getFirst("Accept-Language"));
        assertEquals("Bearer token-" + call, exchange.getRequestHeaders().getFirst("Authorization"));
        int status = call == 1 ? firstStatus : 200;
        String body = call == 1
                ? "{\"code\":401,\"msg\":\"unauthorized\",\"data\":null}"
                : "{\"code\":0,\"msg\":\"success\",\"data\":true}";
        writeJson(exchange, status, body);
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
