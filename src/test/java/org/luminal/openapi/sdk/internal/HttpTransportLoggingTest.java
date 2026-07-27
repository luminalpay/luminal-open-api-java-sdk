package org.luminal.openapi.sdk.internal;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.LuminalApiException;
import org.luminal.openapi.sdk.LuminalOpenApiClient;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTransportLoggingTest {

    private HttpServer server;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile String receivedRequestBody;
    private volatile String receivedAuthorization;
    private volatile String receivedCookie;
    private volatile String receivedSetCookie;
    private volatile String receivedSign;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 200;
        responseBody = "{\"code\":0,\"msg\":\"success\",\"data\":true}";
        receivedRequestBody = null;
        receivedAuthorization = null;
        receivedCookie = null;
        receivedSetCookie = null;
        receivedSign = null;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();

        logger = (Logger) LoggerFactory.getLogger(HttpTransport.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
        server.stop(0);
    }

    @Test
    void defaultLoggingRedactsHeadersAndRecursiveJsonButKeepsOriginalDataRaw() throws Exception {
        responseBody = "{\"code\":0,\"msg\":\"success\",\"data\":{" +
                "\"RefreshToken\":\"response-refresh-secret\"," +
                "\"nested\":[{\"CVV\":\"123\",\"safe\":\"visible-response\"}]}}";
        AtomicReference<String> requestObserved = new AtomicReference<>();
        AtomicReference<String> responseObserved = new AtomicReference<>();
        HttpTransport transport = HttpTransport.create(baseUrl(), "unused-token", responseObserved::set,
                requestObserved::set, "en", 0, null, HttpClient.newHttpClient());

        Map<String, Object> requestBody = Map.of(
                "AccessToken", "request-access-secret",
                "refreshToken", "request-refresh-secret",
                "AppSecret", "request-app-secret",
                "nested", List.of(Map.of(
                        "cvv", "456",
                        "cardNo", "4111111111111111",
                        "CardNumber", "5555555555554444",
                        "safe", "visible-request")));
        Map<String, String> requestHeaders = Map.of(
                "Authorization", "Basic authorization-secret",
                "COOKIE", "cookie-secret",
                "Set-Cookie", "set-cookie-secret",
                "SIGN", "sign-secret",
                "X-Visible", "visible-header");

        JsonNode result = transport.postPublic("/test", requestBody, requestHeaders, JsonSupport.type(JsonNode.class));

        assertEquals("visible-response", result.path("nested").path(0).path("safe").asText());
        assertTrue(requestObserved.get().contains("request-access-secret"));
        assertTrue(requestObserved.get().contains("authorization-secret"));
        assertTrue(responseObserved.get().contains("response-refresh-secret"));
        assertTrue(JsonSupport.mapper().writeValueAsString(requestBody).contains("request-access-secret"));
        assertEquals("Basic authorization-secret", requestHeaders.get("Authorization"));
        assertTrue(receivedRequestBody.contains("request-access-secret"));
        assertEquals("Basic authorization-secret", receivedAuthorization);
        assertEquals("cookie-secret", receivedCookie);
        assertEquals("set-cookie-secret", receivedSetCookie);
        assertEquals("sign-secret", receivedSign);

        String logs = logs();
        assertTrue(logs.contains("REQUEST POST " + baseUrl() + "/test"), logs);
        assertTrue(logs.contains("RESPONSE " + baseUrl() + "/test STATUS 200"), logs);
        assertTrue(logs.contains("visible-header"), logs);
        assertTrue(logs.contains("visible-request"), logs);
        assertTrue(logs.contains("visible-response"), logs);
        assertTrue(logs.contains("<redacted>"), logs);
        for (String secret : List.of("authorization-secret", "cookie-secret", "set-cookie-secret", "sign-secret",
                "request-access-secret", "request-refresh-secret", "request-app-secret", "4111111111111111",
                "5555555555554444", "response-refresh-secret", "\"123\"", "\"456\"")) {
            assertFalse(logs.contains(secret), logs);
        }
    }

    @Test
    void loggingCanBeDisabled() {
        HttpTransport transport = HttpTransport.create(baseUrl(), "unused-token", null, null, "en", 0, null,
                HttpClient.newHttpClient(), false);

        transport.postPublic("/test", Map.of("safe", "value"), Map.of(), JsonSupport.type(Boolean.class));

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void clientUsesInjectedLoggerAndWithBearerTokenKeepsIt() {
        Logger customLogger = (Logger) LoggerFactory.getLogger("luminal-open-api-custom-" + System.nanoTime());
        customLogger.setAdditive(false);
        customLogger.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> customAppender = new ListAppender<>();
        customAppender.start();
        customLogger.addAppender(customAppender);
        try {
            LuminalOpenApiClient client = new LuminalOpenApiClient(baseUrl(), "old-token", null, true, customLogger)
                    .withBearerToken("new-token");

            client.cards().freeze(new CardIdRequest(7L));

            assertEquals(2, customAppender.list.size());
            assertTrue(customAppender.list.get(0).getFormattedMessage().contains("REQUEST POST"));
            assertTrue(customAppender.list.get(1).getFormattedMessage().contains("RESPONSE"));
            assertFalse(logs().contains("/open-api/v1/cards/freeze"), logs());
        } finally {
            customLogger.detachAppender(customAppender);
            customAppender.stop();
        }
    }

    @Test
    void nonJsonBodiesAreSummarizedWithoutRawContent() {
        byte[] requestBytes = "plain-request-secret".getBytes(StandardCharsets.UTF_8);
        responseStatus = 503;
        responseBody = "plain-response-secret";
        HttpTransport transport = HttpTransport.create(baseUrl(), "bearer-secret", null, null, "en", 0, null,
                HttpClient.newHttpClient());

        assertThrows(LuminalApiException.class,
                () -> transport.postSerializedAuthorized("/plain", requestBytes, Map.of(), JsonSupport.type(Boolean.class)));

        String logs = logs();
        assertTrue(logs.contains("<non-json " + requestBytes.length + " bytes>"), logs);
        assertTrue(logs.contains("<non-json " + responseBody.getBytes(StandardCharsets.UTF_8).length + " bytes>"), logs);
        assertFalse(logs.contains("plain-request-secret"), logs);
        assertFalse(logs.contains("plain-response-secret"), logs);
        assertFalse(logs.contains("bearer-secret"), logs);
    }

    @Test
    void emptyBodyUsesEmptyString() {
        HttpTransport transport = HttpTransport.create(baseUrl(), "unused-token", null, null, "en", 0, null,
                HttpClient.newHttpClient());

        transport.postSerializedAuthorized("/empty", new byte[0], Map.of(), JsonSupport.type(Boolean.class));

        String logs = logs();
        assertTrue(logs.contains("REQUEST POST " + baseUrl() + "/empty"), logs);
        assertFalse(logs.contains("<non-json 0 bytes>"), logs);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private String logs() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", (left, right) -> left + '\n' + right);
    }

    private void handle(HttpExchange exchange) throws IOException {
        receivedRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        receivedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        receivedCookie = exchange.getRequestHeaders().getFirst("Cookie");
        receivedSetCookie = exchange.getRequestHeaders().getFirst("Set-Cookie");
        receivedSign = exchange.getRequestHeaders().getFirst("sign");
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
