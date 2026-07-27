# Luminal Open API Java SDK

Standalone Java 17 SDK for every enabled Luminal Open API controller endpoint and webhook event.

## Requirements

- Java 17+
- Runtime dependencies: Jackson Databind, Jackson Java Time, and SLF4J API
- No dependency on Spring, Lombok, or any other Luminal project module

## Client setup

```java
import org.luminal.openapi.sdk.LuminalOpenApiClient;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoRequest;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoResponse;
import org.luminal.openapi.sdk.model.AuthModels.OAuth2Token;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

LuminalOpenApiClient publicClient = new LuminalOpenApiClient("https://api.example.com");
OAuth2Token token = publicClient.auth().getToken("app-id", "app-secret");

LuminalOpenApiClient client = publicClient.withBearerToken(token.accessToken());
PageResultEx<WalletInfoResponse, Object> accounts =
        client.accounts().list(new WalletInfoRequest(1, 10, "USD"));
```

Auto token mode:

```java
LuminalOpenApiClient client = new LuminalOpenApiClient(
        "https://api.example.com",
        "app-id",
        "app-secret",
        null,
        1,
        "en");

String accessToken = client.accessToken();
```

Auto token mode caches the token, refreshes it when remaining lifetime drops below half, and retries unauthorized requests up to the configured count.

HTTP trace:

```java
LuminalOpenApiClient client = new LuminalOpenApiClient(
        "https://api.example.com",
        "app-id",
        "app-secret",
        System.out::println,
        1,
        "en",
        false);
```

HTTP tracing is enabled by default for all SDK clients, including `new LuminalOpenApiClient(baseUrl)` and `new LuminalOpenApiClient(baseUrl, bearerToken)`. The SDK logs request method, URL, headers, body, response URL, status, and body through SLF4J. Pass `false` as the last argument in the configurable constructor to disable tracing. `Authorization`, `Cookie`, `Set-Cookie`, `sign`, `accessToken`, `refreshToken`, `appSecret`, `cvv`, `cardNo`, and `cardNumber` are redacted case-insensitively; non-JSON bodies are logged only as byte counts.

### Logging

The SDK uses SLF4J. In tests, `logback-test.xml` enables console output.

Example `logback` config:

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

The constructor `baseUrl` is the URL prefix for every SDK request. It may include a gateway context path. Do not include a query string or fragment.

```java
LuminalOpenApiClient client = new LuminalOpenApiClient(
        "https://gateway.example.com/luminal",
        accessToken);
// accounts().list(...) calls:
// https://gateway.example.com/luminal/open-api/v1/accounts
```

The client returns the API `data` value directly. Only HTTP `200` is treated as transport success and decoded as a Luminal `{code, msg, data}` envelope. Every other HTTP status is an API/transport error. Malformed response envelopes and non-zero API business codes also throw `LuminalApiException`. Response bodies are limited to 1 MiB. The exception retains the HTTP status, API code, and original response body when available.

Locale header support: SDK requests send `Accept-Language: en` by default. Pass `zh` in auto-token mode to request Chinese responses.

## Card issuance signing

The `/cards/issue` body is the exception to ordinary Int64 serialization: every Long/ID value is a JSON number. Null properties are omitted, object keys are sorted alphabetically, and the exact UTF-8 bytes signed with `SHA256withRSA` are sent unchanged. Private keys use PKCS#8.

```java
PrivateKey privateKey = RsaSignatures.readPrivateKey(privateKeyPem);
Long taskId = client.cards().issue(issueRequest, privateKey);
```

A precomputed Base64 signature may also be supplied:

```java
Long taskId = client.cards().issue(issueRequest, signature);
```

## Int64 serialization

Java `Long` and `long` request values follow the JavaScript safe-integer rule:

- Values strictly greater than `-9007199254740991` and strictly less than `9007199254740991` serialize as JSON numbers.
- Boundary values and values outside that range serialize as JSON strings.
- For example, `9007199254740991L` serializes as `"9007199254740991"`.
- API responses may contain Int64 values as either JSON numbers or decimal strings; the SDK accepts both forms when decoding into `Long` fields.
These rules apply to ordinary request JSON. Use `Long` wrapper fields when nullable Int64 values are required. Non-Java clients must preserve full Int64 precision instead of passing these identifiers through an IEEE-754 floating-point number.

For example, `100L` becomes `100`, and `9007199254740991L` becomes `"9007199254740991"`.

## Controller coverage

All endpoints use HTTP `POST`.

| SDK method | Path | Description |
|---|---|---|
| `auth().getToken` | `/open-api/v1/auth/token` | Obtain an OAuth2 token with Basic authorization. |
| `auth().refreshToken` | `/open-api/v1/auth/refresh-token` | Exchange a refresh token for a new token. |
| `auth().logout` | `/open-api/v1/auth/logout` | Invalidate the configured bearer token. |
| `accounts().list` | `/open-api/v1/accounts` | List wallet accounts. |
| `transactions().list` | `/open-api/v1/transactions/list` | List wallet transactions. |
| `sharedAccounts().create` | `/open-api/v1/shared-account/create` | Create and fund a shared account. |
| `sharedAccounts().list` | `/open-api/v1/shared-account/list` | List shared accounts. |
| `sharedAccounts().increase` | `/open-api/v1/shared-account/increase` | Deposit into a shared account. |
| `sharedAccounts().decrease` | `/open-api/v1/shared-account/decrease` | Withdraw from a shared account. |
| `sharedAccounts().details` | `/open-api/v1/shared-account/details` | Retrieve shared-account details. |
| `sharedAccounts().transactions` | `/open-api/v1/shared-account/transactions` | List shared-account transactions. |
| `cards().bins` | `/open-api/v1/cards/bins` | List available card BIN products. |
| `cards().issue` | `/open-api/v1/cards/issue` | Submit a signed card issuance request. |
| `cards().list` | `/open-api/v1/cards/list` | List issued cards. |
| `cards().cvv` | `/open-api/v1/cards/cvv` | Retrieve card number, CVV, and expiry data. |
| `cards().transactions` | `/open-api/v1/cards/transactions` | List card transactions. |
| `cards().limit` | `/open-api/v1/cards/limit` | Retrieve a card limit. |
| `cards().modifyLimit` | `/open-api/v1/cards/limit/modify` | Update a card limit. |
| `cards().freeze` | `/open-api/v1/cards/freeze` | Freeze a card. |
| `cards().unfreeze` | `/open-api/v1/cards/unfreeze` | Unfreeze a card. |
| `cards().cancel` | `/open-api/v1/cards/cancel` | Cancel a card. |
| `cards().issueDetails` | `/open-api/v1/cards/issue/detail` | Retrieve results for a card issuance task. |
| `cardGroups().list` | `/open-api/v1/cards/group` | List card groups. |
| `cardGroups().create` | `/open-api/v1/cards/group/create` | Create a card group. |
| `cardGroups().update` | `/open-api/v1/cards/group/update` | Update a card group. |
| `cardGroups().delete` | `/open-api/v1/cards/group/delete` | Delete a card group. |

Commented-out controller methods are intentionally excluded because they are not enabled endpoints.

## Webhooks

Webhook requests use:

- Method: `POST`
- Content type: `application/json`
- `sign`: Base64 `SHA256withRSA` signature
- `event`: event type
- `event_id`: unique event identifier
- Body: exact JSON bytes covered by the signature

Always verify the original raw body. Do not parse and reserialize JSON before verification.

```java
import org.luminal.openapi.sdk.model.WebhookModels.CardStatusWebhook;
import org.luminal.openapi.sdk.webhook.WebhookEvent;
import org.luminal.openapi.sdk.webhook.WebhookVerifier;

byte[] rawRequestBody = requestBodyBytes;
WebhookEvent<?> event = WebhookVerifier.parse(
        eventHeader,
        eventIdHeader,
        rawRequestBody,
        signHeader,
        publicKeyPem);

switch (event.type()) {
    case CARD_STATUS -> {
        CardStatusWebhook payload = (CardStatusWebhook) event.payload();
        // Process payload, then persist event.eventId() for idempotency.
    }
    default -> {
        // Handle other supported events.
    }
}
```

Supported events:

| Event | Payload model | Description |
|---|---|---|
| `CARD_TRANSACTIONS` | `TransactionWebhook` | Card transaction update. |
| `CARD_STATUS` | `CardStatusWebhook` | Card status change. |
| `CARD_OPEN_STATUS` | `CardOpenStatusWebhook` | Card issuance task result. |
| `SHARED_ACCOUNT_OPEN_STATUS` | `SharedAccountOpenStatusWebhook` | Shared-account opening result. |
| `SHARE_ACCOUNT_FUND_TRANSACTIONS` | `TransactionWebhook` | Shared-account fund transaction update. |

The SDK verifies signatures but does not persist event IDs. Applications must store `event_id` values and reject duplicates according to their own retention policy.

## Tests

Each of the 26 enabled controller endpoints has an independent JUnit test that verifies HTTP method, path, required headers, request body, and response decoding. Webhook tests cover all five event types, exact-byte and UTF-8 verification, tampered bodies, invalid signatures, unknown events, and PEM public-key loading. RSA utility tests cover exact-byte signing, canonical JSON, PKCS#8 private keys, and X.509 public keys.

```shell
mvn -f luminal-module-open-api/luminal-open-api-java-sdk/pom.xml test
```

### Sandbox integration tests

`SandboxOpenApiIntegrationTest` calls all enabled REST endpoints against the real Sandbox. The supplied Sandbox credentials are test-class defaults; environment variables or JVM properties override them:

```powershell
$env:LUMINAL_OPEN_API_BASE_URL = "https://sandbox-openapi.luminalads.com"
$env:LUMINAL_OPEN_API_APP_ID = "<app-id>"
$env:LUMINAL_OPEN_API_APP_SECRET = "<app-secret>"
mvn -f luminal-module-open-api/luminal-open-api-java-sdk/pom.xml `
  -Dtest=org.luminal.openapi.sdk.integration.SandboxOpenApiIntegrationTest test
```

The integration test starts a local HTTP webhook receiver before the first test. Defaults:

- host: `0.0.0.0`
- port: `18081`
- path: `/luminal-open-api-webhook`
- timeout: `180` seconds

Override with `LUMINAL_OPEN_API_WEBHOOK_HOST`, `LUMINAL_OPEN_API_WEBHOOK_PORT`, `LUMINAL_OPEN_API_WEBHOOK_PATH`, and `LUMINAL_OPEN_API_WEBHOOK_TIMEOUT_SECONDS` (or matching `-Dluminal.openApi.*` properties). Card issuance waits specifically for a terminal `CARD_OPEN_STATUS` webhook; configure its timeout with `LUMINAL_OPEN_API_CARD_OPEN_WEBHOOK_TIMEOUT_SECONDS` or `-Dluminal.openApi.cardOpenWebhookTimeoutSeconds`. Configure the Sandbox developer webhook URL to a publicly reachable URL forwarding to this listener. The test cannot change the Sandbox webhook URL automatically.

Set `LUMINAL_OPEN_API_WEBHOOK_PUBLIC_KEY` or `-Dluminal.openApi.webhookPublicKey` to verify the `sign` header with the platform X.509 public key. If unset, the test-only receiver parses the raw payload and still confirms the task/card through `cards().issueDetails`; production webhook consumers must always verify signatures.

Card issuance is asynchronous. `waitForCardOpenStatusWebhookFromSandbox` waits for the matching `CARD_OPEN_STATUS` webhook, then confirms the task, card ID, and status through `cards().issueDetails`. CVV, transactions, limits, freeze, unfreeze, and cancel tests reuse that card ID. A timeout fails the test; it is not skipped.

The ordered token tests run first: get token, refresh token, logout, then get a new token. Later tests reuse the cached token; an HTTP 401 or API body `code: 401` refreshes it automatically.

Shared accounts, card groups, and cards are created once per test run and reused. Card issuance is asynchronous: card-operation tests cannot proceed until the matching terminal `CARD_OPEN_STATUS` webhook arrives. Mutation tests always execute; no environment-variable gate exists. SDK HTTP tracing is enabled by default and logs redacted request/response data through SLF4J.
