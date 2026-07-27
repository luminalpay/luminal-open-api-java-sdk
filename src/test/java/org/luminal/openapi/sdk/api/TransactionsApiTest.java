package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.model.CommonModels.PageResult;
import org.luminal.openapi.sdk.model.TransactionModels.WalletTransactionRequest;
import org.luminal.openapi.sdk.model.TransactionModels.WalletTransactionResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionsApiTest extends ApiTestSupport {

    @Test
    void listCallsWalletTransactionEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"transactionNo\":99,\"currency\":\"USD\"}]}");
        WalletTransactionRequest body = new WalletTransactionRequest(
                1, 20, 101, List.of(LocalDateTime.of(2026, 7, 1, 0, 0)), 8L);

        PageResult<WalletTransactionResponse> result = client.transactions().list(body);

        assertEquals(99L, result.list().get(0).transactionNo());
        assertBearerPost("/open-api/v1/transactions/list", "\"type\":101");
    }
}
