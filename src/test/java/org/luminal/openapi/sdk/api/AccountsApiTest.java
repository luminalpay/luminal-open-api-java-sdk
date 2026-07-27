package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoRequest;
import org.luminal.openapi.sdk.model.AccountModels.WalletInfoResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountsApiTest extends ApiTestSupport {

    @Test
    void listCallsAccountEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"walletNo\":\"W1\",\"currency\":\"USD\"}],\"extra\":null}");

        PageResultEx<WalletInfoResponse, Object> result =
                client.accounts().list(new WalletInfoRequest(1, 10, "USD"));

        assertEquals(1L, result.total());
        assertEquals("W1", result.list().get(0).walletNo());
        assertBearerPost("/open-api/v1/accounts", "\"currency\":\"USD\"");
    }
}
