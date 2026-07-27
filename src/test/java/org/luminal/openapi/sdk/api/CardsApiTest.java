package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.internal.JsonSupport;
import org.luminal.openapi.sdk.model.CardModels.CardBinResponse;
import org.luminal.openapi.sdk.model.CardModels.CardBinsRequest;
import org.luminal.openapi.sdk.model.CardModels.CardCvvResponse;
import org.luminal.openapi.sdk.model.CardModels.CardIdRequest;
import org.luminal.openapi.sdk.model.CardModels.CardLimitResponse;
import org.luminal.openapi.sdk.model.CardModels.CardLimitUpdateRequest;
import org.luminal.openapi.sdk.model.CardModels.CardTransactionResponse;
import org.luminal.openapi.sdk.model.CardModels.CardTransactionsRequest;
import org.luminal.openapi.sdk.model.CardModels.IssueCardDetailsRequest;
import org.luminal.openapi.sdk.model.CardModels.IssueCardDetailsResponse;
import org.luminal.openapi.sdk.model.CardModels.IssueCardRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardPageRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardsApiTest extends ApiTestSupport {

    @Test
    void binsCallsBinEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"cardBinId\":2,\"cardBin\":\"123456\"}],\"extra\":null}");

        PageResultEx<CardBinResponse, Object> result = client.cards().bins(
                new CardBinsRequest(1, 10, "SHARED", "VISA", "123456", "US"));

        assertEquals("123456", result.list().get(0).cardBin());
        assertBearerPost("/open-api/v1/cards/bins", "\"cardOrganization\":\"VISA\"");
    }

    @Test
    void issueCallsSignedIssueEndpoint() throws Exception {
        respondData("501");
        IssueCardRequest body = new IssueCardRequest(
                1, 2_011_314_055_960_203_265L, 2_079_813_737_300_729_858L,
                "Travel", "SHARED", 2_079_813_761_576_865_792L, null, new BigDecimal("100.00"));
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Long taskId = client.cards().issue(body, keyPair.getPrivate());

        assertEquals(501L, taskId);
        assertBearerPost("/open-api/v1/cards/issue", "\"applyCount\":1");
        assertBearerPost("/open-api/v1/cards/issue", "\"cardBinId\":\"2011314055960203265\"");
        CapturedRequest request = request();
        assertTrue(RsaSignatures.verify(JsonSupport.writeSignatureBytes(body), request.header("sign"), keyPair.getPublic()));
    }

    @Test
    void issueAcceptsPrecomputedSignature() throws Exception {
        respondData("502");
        IssueCardRequest body = new IssueCardRequest(
                1, 2L, 3L, "Travel", "SHARED", 4L, null, new BigDecimal("100.00"));

        Long taskId = client.cards().issue(body, "precomputed-signature");

        assertEquals(502L, taskId);
        assertBearerPost("/open-api/v1/cards/issue", "\"applyCount\":1");
        assertEquals("precomputed-signature", request().header("sign"));
    }

    @Test
    void listCallsCardListEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"memberCardId\":5,\"status\":\"ACTIVE\"}],\"extra\":null}");

        PageResultEx<MemberCardResponse, Object> result = client.cards().list(
                new MemberCardPageRequest(1, 10, 5L, "ACTIVE", null, "SHARED", null, List.of(3L)));

        assertEquals(5L, result.list().get(0).memberCardId());
        assertBearerPost("/open-api/v1/cards/list", "\"status\":\"ACTIVE\"");
    }

    @Test
    void cvvCallsSensitiveDetailsEndpoint() throws Exception {
        respondData("{\"memberCardId\":5,\"cardNo\":\"4111111111111111\",\"cvv\":\"123\",\"expiryDate\":\"2030-01\"}");

        CardCvvResponse result = client.cards().cvv(new CardIdRequest(5L));

        assertEquals("123", result.cvv());
        assertBearerPost("/open-api/v1/cards/cvv", "\"memberCardId\":5");
    }

    @Test
    void transactionsCallsCardTransactionEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"memberCardId\":5,\"orderNo\":\"O1\"}],\"extra\":null}");

        PageResultEx<CardTransactionResponse, Object> result = client.cards().transactions(
                new CardTransactionsRequest(1, 10, "SHARED", 5L, List.of(1L, 2L)));

        assertEquals("O1", result.list().get(0).orderNo());
        assertBearerPost("/open-api/v1/cards/transactions", "\"tradeTime\":[1,2]");
    }

    @Test
    void limitCallsCardLimitEndpoint() throws Exception {
        respondData("{\"memberCardId\":5,\"totalLimit\":100,\"balance\":80}");

        CardLimitResponse result = client.cards().limit(new CardIdRequest(5L));

        assertEquals(new BigDecimal("100"), result.totalLimit());
        assertBearerPost("/open-api/v1/cards/limit", "\"memberCardId\":5");
    }

    @Test
    void modifyLimitCallsLimitUpdateEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.cards().modifyLimit(new CardLimitUpdateRequest(5L, BigDecimal.TEN)));
        assertBearerPost("/open-api/v1/cards/limit/modify", "\"totalLimit\":10");
    }

    @Test
    void freezeCallsFreezeEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.cards().freeze(new CardIdRequest(5L)));
        assertBearerPost("/open-api/v1/cards/freeze", "\"memberCardId\":5");
    }

    @Test
    void unfreezeCallsUnfreezeEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.cards().unfreeze(new CardIdRequest(5L)));
        assertBearerPost("/open-api/v1/cards/unfreeze", "\"memberCardId\":5");
    }

    @Test
    void cancelCallsCancelEndpoint() throws Exception {
        respondData("true");

        assertTrue(client.cards().cancel(new CardIdRequest(5L)));
        assertBearerPost("/open-api/v1/cards/cancel", "\"memberCardId\":5");
    }

    @Test
    void issueDetailsCallsTaskDetailsEndpoint() throws Exception {
        respondData("[{\"memberCardId\":5,\"cardStatus\":\"ACTIVE\",\"message\":\"created\"}]");

        List<IssueCardDetailsResponse> result = client.cards().issueDetails(new IssueCardDetailsRequest(501L));

        assertEquals("created", result.get(0).message());
        assertBearerPost("/open-api/v1/cards/issue/detail", "\"taskId\":501");
    }

}
