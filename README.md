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

HTTP tracing is enabled by default for all SDK clients, including `new LuminalOpenApiClient(baseUrl)` and `new LuminalOpenApiClient(baseUrl, bearerToken)`. The SDK logs request method, URL, headers, body, response URL, status, and body through SLF4J. Pass `false` as the last argument in the configurable constructor to disable tracing. `Authorization`, `Cookie`, `Set-Cookie`, `sign`, `accessToken`, `refreshToken`, `appSecret`, `cvv`, `cardNo`, `cardNumber`, and `verifyCode` are redacted case-insensitively; non-JSON bodies are logged only as byte counts.

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

The `/cards/issue` request requires a Base64 `SHA256withRSA` signature in the `sign` header. Null properties are omitted
and object keys are sorted alphabetically when the canonical signature JSON is produced. Private keys use PKCS#8.
Recharge and withdrawal requests do not require a signature; the SDK sends them with the bearer token and JSON body only.

```java
PrivateKey privateKey = RsaSignatures.readPrivateKey(privateKeyPem);
Long taskId = client.cards().issue(issueRequest, privateKey);
Long rechargeRecordId = client.cards().recharge(rechargeRequest);
Long withdrawRecordId = client.cards().withdraw(withdrawRequest);
```

A precomputed Base64 signature may also be supplied:

```java
Long taskId = client.cards().issue(issueRequest, signature);
```

Recharge, withdrawal, and card-limit updates are asynchronous. Query the returned operation-record identifier until it
reaches `SUCCESS` or `FAIL`, or consume the corresponding webhook. Use `cards().modifyLimitAsync(...)` for a limit update:

```java
RechargeCardOperationRecordResponse operation = client.cards().operationRecord(
        new RechargeCardOperationRecordRequest(rechargeRecordId));
```

The operation record's `createTime` and `updateTime` fields are Unix epoch timestamps in milliseconds (`Long`).

Shared-account cancellation:

```java
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountCancelRequest;

boolean canceled = client.sharedAccounts().cancel(new SharedAccountCancelRequest(
        memberSharedAccountId,
        "No longer needed",
        verifyCode));
```

The cancellation endpoint uses bearer authorization and returns the server's Boolean `data` value; it does not require a request signature.
`verifyCode` is sensitive and should not be logged or persisted by the application.

Card-pool shared-account opening:

```java
import org.luminal.openapi.sdk.model.CardModels.CardBinResponse;
import org.luminal.openapi.sdk.model.CardModels.CardBinsRequest;
import org.luminal.openapi.sdk.model.CardPoolModels.CardPoolRequest;
import org.luminal.openapi.sdk.model.CardPoolModels.CardPoolResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;
import org.luminal.openapi.sdk.model.SharedAccountModels.CreateSharedAccountRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountIdResponse;
import java.math.BigDecimal;
import java.util.List;

// The endpoint returns a plain List; retain this result for the current flow when selecting a pool.
List<CardPoolResponse> pools = client.cardPools().list(new CardPoolRequest(null, null));
CardPoolResponse pool = pools.get(0);
PageResultEx<CardBinResponse, Object> bins = client.cards().bins(new CardBinsRequest(
        1, 20, pool.cardPoolId(), "SHARED", null, null, null));
SharedAccountIdResponse created = client.sharedAccounts().create(new CreateSharedAccountRequest(
        null, pool.cardPoolId(), new BigDecimal("100.00"), "Main"));
```

`CardPoolRequest` only carries pool-level filters. `cardBinId` is not a card-pool list query condition; resolve the
BIN after selecting the pool for inspection. Shared-account creation may use `cardPoolId` when the BIN is selected
from a pool; card issuance continues to require `cardBinId`.

Card-limit updates now infer the card type from `memberCardId`. New code can use the four-argument
`CardLimitUpdateRequest(memberCardId, dailyLimit, monthLimit, totalLimit)` constructor. The earlier five-argument
constructor remains source-compatible, but its legacy `cardType` value is not serialized.

## Int64 serialization

Java `Long` and `long` request values follow the JavaScript safe-integer rule:

- Values strictly greater than `-9007199254740991` and strictly less than `9007199254740991` serialize as JSON numbers.
- Boundary values and values outside that range serialize as JSON strings.
- For example, `9007199254740991L` serializes as `"9007199254740991"`.
- API responses may contain Int64 values as either JSON numbers or decimal strings; the SDK accepts both forms when decoding into `Long` fields.
These rules apply to ordinary request JSON. Use `Long` wrapper fields when nullable Int64 values are required. Non-Java clients must preserve full Int64 precision instead of passing these identifiers through an IEEE-754 floating-point number.

For example, `100L` becomes `100`, and `9007199254740991L` becomes `"9007199254740991"`.

## Controller coverage

Unless noted otherwise, endpoints use HTTP `POST`. `cardHolders().countries` uses HTTP `GET`.

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
| `sharedAccounts().cancel` | `/open-api/v1/shared-account/cancel` | Cancel a shared account with an email/OTP verification code. |
| `sharedAccounts().details` | `/open-api/v1/shared-account/details` | Retrieve shared-account details. |
| `sharedAccounts().transactions` | `/open-api/v1/shared-account/transactions` | List shared-account transactions. |
| `cardPools().list` | `/open-api/v1/cards/pools` | List available card pools as a plain list. |
| `cards().bins` | `/open-api/v1/cards/bins` | List available card BIN products. |
| `cards().issue` | `/open-api/v1/cards/issue` | Submit a signed card issuance request. |
| `cards().list` | `/open-api/v1/cards/list` | List issued cards. |
| `cards().cvv` | `/open-api/v1/cards/cvv` | Retrieve card number, CVV, and expiry data. |
| `cards().transactions` | `/open-api/v1/cards/transactions` | List card transactions. |
| `cards().limit` | `/open-api/v1/cards/limit` | Retrieve a card limit. |
| `cards().modifyLimit` | `/open-api/v1/cards/limit/modify` | Update a card limit. |
| `cards().modifyLimitAsync` | `/open-api/v1/cards/limit/modify/operation-record` | Update a card limit and return an operation-record ID. |
| `cards().freeze` | `/open-api/v1/cards/freeze` | Freeze a card. |
| `cards().unfreeze` | `/open-api/v1/cards/unfreeze` | Unfreeze a card. |
| `cards().cancel` | `/open-api/v1/cards/cancel` | Cancel a card. |
| `cards().recharge` | `/open-api/v1/cards/recharge` | Submit a recharge-card funding request without a signature. |
| `cards().withdraw` | `/open-api/v1/cards/withdraw` | Submit a recharge-card withdrawal request without a signature. |
| `cards().operationRecords` | `/open-api/v1/cards/operation-record` | Query SHARED or RECHARGE card operation records with pagination. |
| `cardHolders().countries` | `/open-api/v1/card-holders/countries` | List supported cardholder countries and their matching dialing-code rules. |
| `cardHolders().add` | `/open-api/v1/card-holders/add` | Create a cardholder. |
| `cardHolders().modify` | `/open-api/v1/card-holders/modify` | Update a cardholder. |
| `cardHolders().detail` | `/open-api/v1/card-holders/info/{cardHolderId}` | Query cardholder details. |
| `cardHolders().page` | `/open-api/v1/card-holders/page` | Query cardholders with pagination. |
| `cardHolders().associatedCards` | `/open-api/v1/card-holders/card/page` | Query cards associated with a cardholder. |
| `cards().issueDetails` | `/open-api/v1/cards/issue/detail` | Retrieve results for a card issuance task. |
| `cardGroups().list` | `/open-api/v1/cards/group` | List card groups. |
| `cardGroups().create` | `/open-api/v1/cards/group/create` | Create a card group. |
| `cardGroups().update` | `/open-api/v1/cards/group/update` | Update a card group. |
| `cardGroups().delete` | `/open-api/v1/cards/group/delete` | Delete a card group. |

Commented-out controller methods are intentionally excluded because they are not enabled endpoints.

Card transaction query responses (`CardTransactionResponse` and `SharedAccountTransactionResponse`) include the local
`settleStatus` and `settleTime` fields. `settleTime` is returned only when the transaction is `SETTLED`.

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
| `CARD_SETTLE_STATUS` | `TransactionWebhook` | Independent local settlement update for recharge and shared cards. |
| `CARD_STATUS` | `CardStatusWebhook` | Card status change. |
| `CARD_OPEN_STATUS` | `CardOpenStatusWebhook` | Card issuance task result. |
| `CARD_RECHARGE_STATUS` | `RechargeCardTransferStatusWebhook` | Recharge-card funding result. |
| `CARD_WITHDRAW_STATUS` | `RechargeCardTransferStatusWebhook` | Recharge-card withdrawal result. |
| `CARD_LIMIT_STATUS` | `RechargeCardTransferStatusWebhook` | Asynchronous card-limit modification result. |
| `SHARED_ACCOUNT_OPEN_STATUS` | `SharedAccountOpenStatusWebhook` | Shared-account opening result. |
| `SHARE_ACCOUNT_FUND_TRANSACTIONS` | `TransactionWebhook` | Shared-account fund transaction update. |

`TransactionWebhook` includes the local `settleStatus` and `settleTime` fields. `settleTime` is populated only when the
transaction has reached `SETTLED`; it is not the channel settlement time. For `CARD_SETTLE_STATUS`, recharge-card
payloads identify the transaction with `memberCardTransactionId`, while shared-card payloads use
`sharedAccountTransactionId`; union-only fields may be `null`.

The SDK verifies signatures but does not persist event IDs. Applications must store `event_id` values and reject duplicates according to their own retention policy.

## Tests

Enabled controller endpoints have independent JUnit tests that verify HTTP method, path, required headers, request body,
signature handling, and response decoding. Webhook tests cover shared-card and recharge-card events, exact-byte and UTF-8
verification, tampered bodies, invalid signatures, unknown events, and PEM public-key loading. RSA utility tests cover
exact-byte signing, canonical JSON, PKCS#8 private keys, and X.509 public keys.

```shell
mvn -f luminal-module-open-api/luminal-open-api-java-sdk/pom.xml test
```

### Sandbox integration tests

`ShareCardSandboxOpenApiIntegrationTest` covers the fixed-BIN shared-card flow. `CardPoolSharedAccountSandboxOpenApiIntegrationTest`
inherits and runs the complete shared-card flow—including shared-account funding, card issuance, webhook waits, card
queries, limit updates, freeze/unfreeze/cancel, and card-group CRUD—after selecting a card pool and its SHARED BIN.
`RechargeCardSandboxOpenApiIntegrationTest` independently
covers recharge-card issuance, funding, withdrawal, operation-record queries, card transactions, lifecycle operations,
and recharge-card webhooks against the real Sandbox. The supplied Sandbox credentials are test-class defaults;
environment variables or JVM properties override them:

```powershell
$env:LUMINAL_OPEN_API_BASE_URL = "https://sandbox-openapi.luminalads.com"
$env:LUMINAL_OPEN_API_APP_ID = "<app-id>"
$env:LUMINAL_OPEN_API_APP_SECRET = "<app-secret>"
mvn -f luminal-module-open-api/luminal-open-api-java-sdk/pom.xml `
  -Dtest=org.luminal.openapi.sdk.integration.ShareCardSandboxOpenApiIntegrationTest test

mvn -f luminal-module-open-api/luminal-open-api-java-sdk/pom.xml `
  -Dtest=org.luminal.openapi.sdk.integration.RechargeCardSandboxOpenApiIntegrationTest test

mvn -f luminal-module-open-api/luminal-open-api-java-sdk/pom.xml `
  -Dtest=org.luminal.openapi.sdk.integration.CardPoolSharedAccountSandboxOpenApiIntegrationTest test
```

The card-pool test uses the same fixed Sandbox configuration as the shared-card test, caches the plain card-pool list
once, selects the first pool that can open a shared account, resolves the fixed SHARED BIN `22346703`, and opens the
account with an initial amount of `100.00`.

The integration test starts a local HTTP webhook receiver before the first test. Defaults:

- host: `0.0.0.0`
- port: `18081`
- path: `/luminal-open-api-webhook`
- timeout: `180` seconds

Override with `LUMINAL_OPEN_API_WEBHOOK_HOST`, `LUMINAL_OPEN_API_WEBHOOK_PORT`, `LUMINAL_OPEN_API_WEBHOOK_PATH`, and `LUMINAL_OPEN_API_WEBHOOK_TIMEOUT_SECONDS` (or matching `-Dluminal.openApi.*` properties). Card issuance waits specifically for a terminal `CARD_OPEN_STATUS` webhook; configure its timeout with `LUMINAL_OPEN_API_CARD_OPEN_WEBHOOK_TIMEOUT_SECONDS` or `-Dluminal.openApi.cardOpenWebhookTimeoutSeconds`. Configure the Sandbox developer webhook URL to a publicly reachable URL forwarding to this listener. The test cannot change the Sandbox webhook URL automatically.

The recharge-card test defaults to port `18082` to avoid clashing with the shared-card test. Recharge-card amounts and
limits can be overridden with `LUMINAL_OPEN_API_RECHARGE_ISSUE_AMOUNT`, `LUMINAL_OPEN_API_RECHARGE_AMOUNT`,
`LUMINAL_OPEN_API_WITHDRAW_AMOUNT`, `LUMINAL_OPEN_API_RECHARGE_DAILY_LIMIT`, and
`LUMINAL_OPEN_API_RECHARGE_MONTH_LIMIT` when the Sandbox account requires different values. The default recharge-card
BIN is `578391`; override it with `LUMINAL_OPEN_API_RECHARGE_CARD_BIN` when required.
The recharge-card Sandbox test resolves country identifiers, dialing codes, and local-phone length from
`cardHolders().countries()`. `LUMINAL_OPEN_API_CARD_HOLDER_COUNTRY_ID` and
`LUMINAL_OPEN_API_CARD_HOLDER_AREA_CODE` (or matching `-Dluminal.openApi.*` properties) may select a country, but any
configured pair is checked against that endpoint. When neither is configured, the test prefers the country from an
existing Sandbox cardholder, then Hong Kong, then the first supported country. The local number is sent in `phone`
without the international prefix; the matching dialing code is sent separately in `areaCode`.
The cardholder CRUD tests still create and verify a dedicated Sandbox cardholder. Both card-issuance integration tests
inspect the selected BIN's `customCardholder` flag: they pass `cardHolderId` and verify the cardholder association only
when the flag is `1`; otherwise the field is omitted. Recharge-card issuance reuses the first cardholder that existed
before the test run, or the cardholder created by the earlier CRUD test when the Sandbox initially had none. The
successful modify test restores the original profile after verifying the update.
Recharge-card limit-modification tests inspect the selected BIN's `canLimit` flag and skip the update when it is not
`1`; shared-card limit modification is always exercised because shared cards support it.

Card issuance and card-operation integration tests wait for their corresponding webhook first. Only when the webhook
wait times out does the integration test compensate by querying `cards().issueDetails(...)` or
`cards().operationRecord(...)`.

Set `LUMINAL_OPEN_API_WEBHOOK_PUBLIC_KEY` or `-Dluminal.openApi.webhookPublicKey` to verify the `sign` header with the platform X.509 public key. If unset, the test-only receiver parses the raw payload and still confirms the task/card through `cards().issueDetails`; production webhook consumers must always verify signatures.

Card issuance is asynchronous. `waitForCardOpenStatusWebhookFromSandbox` waits for the matching `CARD_OPEN_STATUS` webhook, then confirms the task, card ID, and status through `cards().issueDetails`. CVV, transactions, limits, freeze, unfreeze, and cancel tests reuse that card ID. A timeout fails the test; it is not skipped.

The ordered token tests run first: get token, refresh token, logout, then get a new token. Later tests reuse the cached token; an HTTP 401 or API body `code: 401` refreshes it automatically.

Shared accounts, card groups, and cards are created once per test run and reused. Card issuance is asynchronous: card-operation tests cannot proceed until the matching terminal `CARD_OPEN_STATUS` webhook arrives. Mutation tests always execute; no environment-variable gate exists. SDK HTTP tracing is enabled by default and logs redacted request/response data through SLF4J.
