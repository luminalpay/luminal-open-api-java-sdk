package org.luminal.openapi.sdk.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.luminal.openapi.sdk.LuminalApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.CookieManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes HTTP requests and decodes the Luminal response envelope.
 */
public final class HttpTransport {

    private static final int SUCCESS_CODE = 0;
    private static final int MAX_RESPONSE_BODY_BYTES = 1 << 20;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String REDACTED = "<redacted>";
    private static final Set<String> SENSITIVE_LOG_HEADERS = Set.of("authorization", "cookie", "set-cookie", "sign");
    private static final Set<String> SENSITIVE_LOG_BODY_KEYS = Set.of(
            "accesstoken", "refreshtoken", "appsecret", "cvv", "cardno", "cardnumber");
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(HttpTransport.class);

    private final URI baseUri;
    private final Supplier<String> bearerTokenSupplier;
    private final HttpClient httpClient;
    private final Consumer<String> responseObserver;
    private final Consumer<String> requestObserver;
    private final String locale;
    private final int unauthorizedRetryCount;
    private final Runnable unauthorizedRetryAction;
    private final boolean httpLoggingEnabled;
    private final Logger logger;

    private HttpTransport(URI baseUri, Supplier<String> bearerTokenSupplier, HttpClient httpClient,
            Consumer<String> responseObserver, Consumer<String> requestObserver, String locale, int unauthorizedRetryCount,
            Runnable unauthorizedRetryAction, boolean httpLoggingEnabled, Logger logger) {
        this.baseUri = baseUri;
        this.bearerTokenSupplier = bearerTokenSupplier;
        this.httpClient = httpClient;
        this.responseObserver = responseObserver;
        this.requestObserver = requestObserver;
        this.locale = locale;
        this.unauthorizedRetryCount = Math.max(0, unauthorizedRetryCount);
        this.unauthorizedRetryAction = unauthorizedRetryAction;
        this.httpLoggingEnabled = httpLoggingEnabled;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Creates an HTTP transport.
     *
     * @param baseUrl     absolute HTTP or HTTPS request URL prefix
     * @param bearerToken optional bearer token
     * @return configured transport
     */
    public static HttpTransport create(String baseUrl, String bearerToken) {
        return create(baseUrl, bearerToken, null, null, "en", 0, null);
    }

    /**
     * Creates an HTTP transport with an optional raw-response observer.
     */
    public static HttpTransport create(String baseUrl, String bearerToken, Consumer<String> responseObserver) {
        return create(baseUrl, bearerToken, responseObserver, null, "en", 0, null);
    }

    public static HttpTransport create(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver) {
        return create(baseUrl, bearerToken, responseObserver, requestObserver, "en", 0, null);
    }

    public static HttpTransport create(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction) {
        return create(baseUrl, bearerToken, responseObserver, requestObserver, locale, unauthorizedRetryCount, unauthorizedRetryAction,
                HttpClient.newBuilder().cookieHandler(new CookieManager()).connectTimeout(Duration.ofSeconds(10)).build());
    }

    public static HttpTransport create(String baseUrl, Supplier<String> bearerTokenSupplier,
            Consumer<String> responseObserver, Consumer<String> requestObserver, String locale, int unauthorizedRetryCount,
            Runnable unauthorizedRetryAction) {
        return create(baseUrl, bearerTokenSupplier, responseObserver, requestObserver, locale, unauthorizedRetryCount, unauthorizedRetryAction,
                HttpClient.newBuilder().cookieHandler(new CookieManager()).connectTimeout(Duration.ofSeconds(10)).build());
    }

    public static HttpTransport create(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction, HttpClient httpClient) {
        return create(baseUrl, bearerToken, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, httpClient, true);
    }

    public static HttpTransport create(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction,
            HttpClient httpClient, boolean httpLoggingEnabled) {
        return create(baseUrl, bearerToken, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, httpClient, httpLoggingEnabled, DEFAULT_LOGGER);
    }

    public static HttpTransport create(String baseUrl, String bearerToken, Consumer<String> responseObserver,
            Consumer<String> requestObserver, String locale, int unauthorizedRetryCount, Runnable unauthorizedRetryAction,
            HttpClient httpClient, boolean httpLoggingEnabled, Logger logger) {
        return new HttpTransport(validateBaseUri(baseUrl), fixedTokenSupplier(normalizeToken(bearerToken)),
                Objects.requireNonNull(httpClient, "httpClient"), responseObserver, requestObserver, normalizeLocale(locale),
                unauthorizedRetryCount, unauthorizedRetryAction, httpLoggingEnabled, logger);
    }

    public static HttpTransport create(String baseUrl, Supplier<String> bearerTokenSupplier,
            Consumer<String> responseObserver, Consumer<String> requestObserver, String locale, int unauthorizedRetryCount,
            Runnable unauthorizedRetryAction, HttpClient httpClient) {
        return create(baseUrl, bearerTokenSupplier, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, httpClient, true);
    }

    public static HttpTransport create(String baseUrl, Supplier<String> bearerTokenSupplier,
            Consumer<String> responseObserver, Consumer<String> requestObserver, String locale, int unauthorizedRetryCount,
            Runnable unauthorizedRetryAction, HttpClient httpClient, boolean httpLoggingEnabled) {
        return create(baseUrl, bearerTokenSupplier, responseObserver, requestObserver, locale, unauthorizedRetryCount,
                unauthorizedRetryAction, httpClient, httpLoggingEnabled, DEFAULT_LOGGER);
    }

    public static HttpTransport create(String baseUrl, Supplier<String> bearerTokenSupplier,
            Consumer<String> responseObserver, Consumer<String> requestObserver, String locale, int unauthorizedRetryCount,
            Runnable unauthorizedRetryAction, HttpClient httpClient, boolean httpLoggingEnabled, Logger logger) {
        return new HttpTransport(validateBaseUri(baseUrl), Objects.requireNonNull(bearerTokenSupplier, "bearerTokenSupplier"),
                Objects.requireNonNull(httpClient, "httpClient"), responseObserver, requestObserver, normalizeLocale(locale),
                unauthorizedRetryCount, unauthorizedRetryAction, httpLoggingEnabled, logger);
    }

    /**
     * Copies this transport with a different bearer token.
     *
     * @param token non-blank bearer token
     * @return transport sharing the HTTP client
     */
    public HttpTransport withBearerToken(String token) {
        return new HttpTransport(baseUri, fixedTokenSupplier(requireNonBlank(token, "token")), httpClient,
                responseObserver, requestObserver, locale, unauthorizedRetryCount, unauthorizedRetryAction,
                httpLoggingEnabled, logger);
    }

    /**
     * Serializes a request using the exact JSON configuration used by HTTP requests.
     *
     * @param body request body
     * @return exact bytes ready for signing and sending
     */
    public byte[] serialize(Object body) {
        return JsonSupport.writeBytes(Objects.requireNonNull(body, "body"));
    }

    /**
     * Sends a request without automatically adding bearer authorization.
     */
    public <T> T postPublic(String path, Object body, Map<String, String> headers, JavaType dataType) {
        return post(path, serializeNullable(body), headers, dataType);
    }

    /**
     * Sends a bearer-authorized JSON request.
     */
    public <T> T postAuthorized(String path, Object body, JavaType dataType) {
        return postAuthorized(path, body, Map.of(), dataType);
    }

    /**
     * Sends a bearer-authorized JSON request with additional headers.
     */
    public <T> T postAuthorized(String path, Object body, Map<String, String> headers, JavaType dataType) {
        return post(path, serializeNullable(body), () -> authorizedHeaders(headers), dataType);
    }

    /**
     * Sends pre-serialized bytes with bearer authorization.
     *
     * <p>This entry point guarantees that signed bytes are identical to transmitted bytes.</p>
     */
    public <T> T postSerializedAuthorized(
            String path, byte[] body, Map<String, String> headers, JavaType dataType) {
        return post(path, Objects.requireNonNull(body, "body"), () -> authorizedHeaders(headers), dataType);
    }

    /**
     * Sends a bearer-authorized request and interprets a nullable Boolean response.
     */
    public boolean postAuthorizedBoolean(String path, Object body) {
        return Boolean.TRUE.equals(postAuthorized(path, body, JsonSupport.type(Boolean.class)));
    }

    private <T> T post(String path, byte[] body, Map<String, String> headers, JavaType dataType) {
        return post(path, body, () -> headers, dataType);
    }

    private <T> T post(String path, byte[] body, Supplier<Map<String, String>> headersSupplier, JavaType dataType) {
        LuminalApiException lastFailure = null;
        for (int attempt = 0; attempt <= unauthorizedRetryCount; attempt++) {
            Map<String, String> headers = Objects.requireNonNull(headersSupplier.get(), "headers");
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint(path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Accept-Language", locale)
                    .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
            if (body != null) {
                builder.header("Content-Type", "application/json");
            }
            headers.forEach(builder::header);
            if (requestObserver != null) {
                requestObserver.accept(formatRequest(path, body, headers));
            }
            traceRequest(path, body, headers);

            final HttpResponse<InputStream> response;
            try {
                response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new LuminalApiException("Luminal API request was interrupted", exception);
            } catch (IOException exception) {
                throw new LuminalApiException("Luminal API request failed", exception);
            }

            byte[] responseBytes;
            try (InputStream input = response.body()) {
                responseBytes = input.readNBytes(MAX_RESPONSE_BODY_BYTES + 1);
            } catch (IOException exception) {
                throw new LuminalApiException("Luminal API response could not be read", exception);
            }
            int status = response.statusCode();
            if (responseBytes.length > MAX_RESPONSE_BODY_BYTES) {
                throw new LuminalApiException(
                        "Luminal API response body exceeds 1048576 bytes", status, null, null);
            }
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            if (responseObserver != null) {
                responseObserver.accept(compact(responseBody));
            }
            traceResponse(path, status, responseBytes);
            if (status == 401) {
                lastFailure = new LuminalApiException("Luminal API returned HTTP 401", 401, null, responseBody);
                if (attempt < unauthorizedRetryCount && unauthorizedRetryAction != null) {
                    unauthorizedRetryAction.run();
                    continue;
                }
                throw lastFailure;
            }
            if (status != 200) {
                throw new LuminalApiException("Luminal API returned HTTP " + status, status, null, responseBody);
            }
            try {
                return decode(status, responseBytes, responseBody, dataType);
            } catch (LuminalApiException exception) {
                if (exception.httpStatus() == 200 && exception.apiCode() != null && exception.apiCode() == 401) {
                    lastFailure = exception;
                    if (attempt < unauthorizedRetryCount && unauthorizedRetryAction != null) {
                        unauthorizedRetryAction.run();
                        continue;
                    }
                }
                throw exception;
            }
        }
        throw lastFailure == null ? new LuminalApiException("Luminal API returned HTTP 401", 401, null, null) : lastFailure;
    }

    private <T> T decode(int httpStatus, byte[] responseBytes, String responseBody, JavaType dataType) {
        try {
            JsonNode envelope = JsonSupport.mapper().readTree(responseBytes);
            JsonNode codeNode = envelope == null ? null : envelope.get("code");
            if (codeNode == null || !codeNode.isIntegralNumber() || !codeNode.canConvertToInt()) {
                throw new LuminalApiException("Luminal API response is missing an integer code",
                        httpStatus, null, responseBody);
            }
            int code = codeNode.intValue();
            JsonNode messageNode = envelope.get("msg");
            String message = messageNode == null || messageNode.isNull() ? null : messageNode.asText();
            if (code != SUCCESS_CODE) {
                throw new LuminalApiException(message == null || message.isBlank()
                        ? "Luminal API returned business code " + code : message,
                        httpStatus, code, responseBody);
            }
            JsonNode data = envelope.get("data");
            return data == null || data.isNull() ? null : JsonSupport.mapper().readerFor(dataType).readValue(data);
        } catch (JsonProcessingException exception) {
            throw new LuminalApiException("Luminal API returned invalid JSON", httpStatus, null, responseBody);
        } catch (IOException exception) {
            throw new LuminalApiException("Luminal API response could not be decoded", exception);
        }
    }

    private byte[] serializeNullable(Object body) {
        return body == null ? null : serialize(body);
    }

    private String formatRequest(String path, byte[] body, Map<String, String> headers) {
        return "REQUEST " + endpoint(path) + " HEADERS " + compact(String.valueOf(headers))
                + " BODY " + (body == null ? "" : compact(new String(body, StandardCharsets.UTF_8)));
    }

    private void traceRequest(String path, byte[] body, Map<String, String> headers) {
        if (!httpLoggingEnabled || !logger.isInfoEnabled()) {
            return;
        }
        logger.info("REQUEST POST {} HEADERS {} BODY {}", endpoint(path), formatLogHeaders(headers), formatLogBody(body));
    }

    public void logWebhook(String event, String eventId, byte[] body) {
        if (!httpLoggingEnabled || !logger.isInfoEnabled()) {
            return;
        }
        logger.info("WEBHOOK event={} eventId={} body={}", event, eventId, formatLogBody(body));
    }

    private void traceResponse(String path, int status, byte[] body) {
        if (!httpLoggingEnabled || !logger.isInfoEnabled()) {
            return;
        }
        logger.info("RESPONSE {} STATUS {} BODY {}", endpoint(path), status, formatLogBody(body));
    }

    private static String formatLogHeaders(Map<String, String> headers) {
        Map<String, String> redacted = new LinkedHashMap<>();
        headers.forEach((name, value) -> redacted.put(name,
                SENSITIVE_LOG_HEADERS.contains(name.toLowerCase(Locale.ROOT)) ? REDACTED : value));
        return compact(String.valueOf(redacted));
    }

    private static String formatLogBody(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        try {
            JsonNode root = JsonSupport.mapper().readTree(body);
            if (root == null) {
                return "<non-json " + body.length + " bytes>";
            }
            redactLogBody(root);
            return JsonSupport.mapper().writeValueAsString(root);
        } catch (IOException exception) {
            return "<non-json " + body.length + " bytes>";
        }
    }

    private static void redactLogBody(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            object.fieldNames().forEachRemaining(name -> {
                if (SENSITIVE_LOG_BODY_KEYS.contains(name.toLowerCase(Locale.ROOT))) {
                    object.put(name, REDACTED);
                } else {
                    redactLogBody(object.get(name));
                }
            });
        } else if (node.isArray()) {
            node.forEach(HttpTransport::redactLogBody);
        }
    }

    private static String compact(String value) {
        return value == null ? "" : value.replace("\r", "\\r").replace("\n", "\\n");
    }

    private Map<String, String> authorizedHeaders(Map<String, String> headers) {
        Map<String, String> result = new HashMap<>(Objects.requireNonNull(headers, "headers"));
        result.put("Authorization", "Bearer " + requireNonBlank(bearerTokenSupplier.get(), "bearerToken"));
        return result;
    }

    private URI endpoint(String path) {
        String value = Objects.requireNonNull(path, "path");
        if (!value.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        return URI.create(baseUri + value);
    }

    private static URI validateBaseUri(String baseUrl) {
        String value = requireNonBlank(baseUrl, "baseUrl").replaceFirst("/+$", "");
        URI uri = URI.create(value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("baseUrl must be an absolute HTTP or HTTPS URL");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("baseUrl must not contain a query string or fragment");
        }
        return uri;
    }

    private static String normalizeToken(String token) {
        return token == null ? null : requireNonBlank(token, "bearerToken");
    }

    private static Supplier<String> fixedTokenSupplier(String token) {
        return () -> token;
    }

    private static String normalizeLocale(String locale) {
        String value = locale == null || locale.isBlank() ? "en" : locale.trim();
        return "zh".equalsIgnoreCase(value) ? "zh" : "en";
    }

    /**
     * Validates a required non-blank string argument.
     */
    public static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
