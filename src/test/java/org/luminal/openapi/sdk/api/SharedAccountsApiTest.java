package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;
import org.luminal.openapi.sdk.model.SharedAccountModels.CreateSharedAccountRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountBalanceRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountCancelRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountGetRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountIdResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountPageRequest;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionIdResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionResponse;
import org.luminal.openapi.sdk.model.SharedAccountModels.SharedAccountTransactionsRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedAccountsApiTest extends ApiTestSupport {

    @Test
    void createCallsCreateEndpoint() throws Exception {
        respondData("{\"memberSharedAccountId\":11}");

        SharedAccountIdResponse result = client.sharedAccounts().create(
                new CreateSharedAccountRequest(2L, new BigDecimal("100.00"), "Main"));

        assertEquals(11L, result.memberSharedAccountId());
        assertBearerPost("/open-api/v1/shared-account/create", "\"accountName\":\"Main\"");
    }

    @Test
    void listCallsListEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"memberSharedAccountId\":11,\"accountName\":\"Main\"}],\"extra\":null}");

        PageResultEx<SharedAccountResponse, Object> result = client.sharedAccounts().list(
                new SharedAccountPageRequest(1, 10, 11L, "Main"));

        assertEquals("Main", result.list().get(0).accountName());
        assertBearerPost("/open-api/v1/shared-account/list", "\"memberSharedAccountId\":11");
    }

    @Test
    void increaseCallsDepositEndpoint() throws Exception {
        respondData("{\"sharedAccountTransactionId\":\"21\"}");

        SharedAccountTransactionIdResponse result = client.sharedAccounts().increase(
                new SharedAccountBalanceRequest(11L, BigDecimal.TEN));

        assertEquals("21", result.sharedAccountTransactionId());
        assertBearerPost("/open-api/v1/shared-account/increase", "\"amount\":10");
    }

    @Test
    void decreaseCallsWithdrawEndpoint() throws Exception {
        respondData("{\"sharedAccountTransactionId\":\"22\"}");

        SharedAccountTransactionIdResponse result = client.sharedAccounts().decrease(
                new SharedAccountBalanceRequest(11L, BigDecimal.ONE));

        assertEquals("22", result.sharedAccountTransactionId());
        assertBearerPost("/open-api/v1/shared-account/decrease", "\"amount\":1");
    }

    @Test
    void cancelCallsCancelEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.sharedAccounts().cancel(
                new SharedAccountCancelRequest(11L, "close account", "otp-123")));

        assertBearerPost("/open-api/v1/shared-account/cancel", "\"memberSharedAccountId\":11");
        assertEquals("", request().header("sign"));
        assertTrue(request().body().contains("\"remark\":\"close account\""));
        assertTrue(request().body().contains("\"verifyCode\":\"otp-123\""));
    }

    @Test
    void detailsCallsDetailsEndpoint() throws Exception {
        respondData("{\"memberSharedAccountId\":11,\"accountName\":\"Main\",\"status\":\"ACTIVE\"}");

        SharedAccountResponse result = client.sharedAccounts().details(new SharedAccountGetRequest(11L));

        assertEquals("ACTIVE", result.status());
        assertBearerPost("/open-api/v1/shared-account/details", "\"memberSharedAccountId\":11");
    }

    @Test
    void transactionsCallsTransactionEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"sharedAccountTransactionId\":21,\"status\":\"SUCCESS\","
                + "\"settleStatus\":\"PENDING\",\"settleTime\":\"2026-08-29T12:00:00\"}],\"extra\":null}");

        PageResultEx<SharedAccountTransactionResponse, Object> result =
                client.sharedAccounts().transactions(new SharedAccountTransactionsRequest(
                        1, 10, 21L, 11L, null, "DEPOSIT", List.of()));

        assertEquals("SUCCESS", result.list().get(0).status());
        assertEquals("PENDING", result.list().get(0).settleStatus());
        assertEquals(LocalDateTime.of(2026, 8, 29, 12, 0), result.list().get(0).settleTime());
        assertBearerPost("/open-api/v1/shared-account/transactions", "\"type\":\"DEPOSIT\"");
    }
}
