package org.luminal.openapi.sdk;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ApiTestSupport {

    protected HttpServer server;
    protected LuminalOpenApiClient client;
    private volatile int responseStatus;
    private volatile String responseBody;
    private CompletableFuture<CapturedRequest> captured;

    @BeforeEach
    protected void startServer() throws IOException {
        responseStatus = 200;
        responseBody = success("true");
        captured = new CompletableFuture<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        client = new LuminalOpenApiClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-token");
    }

    @AfterEach
    protected void stopServer() {
        server.stop(0);
    }

    protected void respondData(String dataJson) {
        responseStatus = 200;
        responseBody = success(dataJson);
    }

    protected void respond(int status, String body) {
        responseStatus = status;
        responseBody = body;
    }

    protected CapturedRequest request() throws Exception {
        return captured.get(2, TimeUnit.SECONDS);
    }

    protected void assertBearerPost(String expectedPath, String expectedBodyPart) throws Exception {
        CapturedRequest request = request();
        assertEquals("POST", request.method());
        assertEquals(expectedPath, request.path());
        assertEquals("Bearer test-token", request.header("Authorization"));
        if (expectedBodyPart != null) {
            assertTrue(request.body().contains(expectedBodyPart), request.body());
            assertTrue(request.header("Content-Type").startsWith("application/json"));
        }
    }

    protected void assertBearerGet(String expectedPath) throws Exception {
        CapturedRequest request = request();
        assertEquals("GET", request.method());
        assertEquals(expectedPath, request.path());
        assertEquals("Bearer test-token", request.header("Authorization"));
        assertEquals("", request.body());
    }

    static String success(String dataJson) {
        return "{\"code\":0,\"msg\":\"success\",\"data\":" + dataJson + '}';
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        captured.complete(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders(),
                requestBytes));
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public record CapturedRequest(String method, String path, Map<String, List<String>> headers, byte[] bodyBytes) {
        public CapturedRequest {
            bodyBytes = Arrays.copyOf(bodyBytes, bodyBytes.length);
        }

        @Override
        public byte[] bodyBytes() {
            return Arrays.copyOf(bodyBytes, bodyBytes.length);
        }

        public String body() {
            return new String(bodyBytes, StandardCharsets.UTF_8);
        }

        public String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .flatMap(entry -> entry.getValue().stream())
                    .findFirst()
                    .orElse("");
        }
    }
}
