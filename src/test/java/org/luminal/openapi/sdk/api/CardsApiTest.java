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
import org.luminal.openapi.sdk.model.CardModels.MemberCardRechargeRequest;
import org.luminal.openapi.sdk.model.CardModels.MemberCardWithdrawRequest;
import org.luminal.openapi.sdk.model.CardModels.RechargeCardOperationRecordRequest;
import org.luminal.openapi.sdk.model.CardModels.RechargeCardOperationRecordResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResultEx;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardsApiTest extends ApiTestSupport {

    @Test
    void binsCallsBinEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"cardBinId\":2,\"cardBin\":\"123456\","
                + "\"customCardholder\":1,\"canLimit\":1}],\"extra\":null}");

        PageResultEx<CardBinResponse, Object> result = client.cards().bins(
                new CardBinsRequest(1, 10, "SHARED", "VISA", "123456", "US"));

        assertEquals("123456", result.list().get(0).cardBin());
        assertEquals(1, result.list().get(0).customCardholder());
        assertEquals(1, result.list().get(0).canLimit());
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
    void issueSupportsRechargeCardDailyLimit() throws Exception {
        respondData("503");
        IssueCardRequest body = new IssueCardRequest(
                1, 2L, 3L, "Recharge", "RECHARGE", null,
                new BigDecimal("50.00"), new BigDecimal("500.00"), new BigDecimal("100.00"));

        assertEquals(503L, client.cards().issue(body, "signature"));
        assertBearerPost("/open-api/v1/cards/issue", "\"dailyLimit\":50.00");
    }

    @Test
    void issueSupportsExplicitCardholder() throws Exception {
        respondData("504");
        IssueCardRequest body = new IssueCardRequest(
                1, 2L, 3L, "Cardholder card", "RECHARGE", null,
                new BigDecimal("50.00"), new BigDecimal("500.00"), new BigDecimal("100.00"), 801L);

        assertEquals(504L, client.cards().issue(body, "signature"));
        assertBearerPost("/open-api/v1/cards/issue", "\"cardHolderId\":801");
    }

    @Test
    void issueSupportsExplicitSharedCardholder() throws Exception {
        respondData("505");
        IssueCardRequest body = new IssueCardRequest(
                1, 2L, 3L, "Shared cardholder card", "SHARED", 4L,
                null, null, new BigDecimal("100.00"), 801L);

        assertEquals(505L, client.cards().issue(body, "signature"));
        assertBearerPost("/open-api/v1/cards/issue", "\"cardType\":\"SHARED\"");
        assertBearerPost("/open-api/v1/cards/issue", "\"cardHolderId\":801");
    }

    @Test
    void listCallsCardListEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"memberCardId\":5,\"status\":\"ACTIVE\","
                + "\"dailyLimit\":50,\"monthLimit\":500,\"canLimit\":1}],\"extra\":null}");

        PageResultEx<MemberCardResponse, Object> result = client.cards().list(
                new MemberCardPageRequest(1, 10, 5L, "ACTIVE", null, "SHARED", null, List.of(3L)));

        assertEquals(5L, result.list().get(0).memberCardId());
        assertEquals(new BigDecimal("50"), result.list().get(0).dailyLimit());
        assertEquals(new BigDecimal("500"), result.list().get(0).monthLimit());
        assertEquals(1, result.list().get(0).canLimit());
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
        respondData("{\"total\":1,\"list\":[{\"memberCardId\":5,\"orderNo\":\"O1\","
                + "\"settleStatus\":\"SETTLED\",\"settleTime\":\"2026-08-29T12:00:00\"}],\"extra\":null}");

        PageResultEx<CardTransactionResponse, Object> result = client.cards().transactions(
                new CardTransactionsRequest(1, 10, "SHARED", 5L, List.of(1L, 2L)));

        assertEquals("O1", result.list().get(0).orderNo());
        assertEquals("SETTLED", result.list().get(0).settleStatus());
        assertEquals(java.time.LocalDateTime.of(2026, 8, 29, 12, 0), result.list().get(0).settleTime());
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

        assertTrue(client.cards().modifyLimit(new CardLimitUpdateRequest(
                5L, "RECHARGE", BigDecimal.TEN, new BigDecimal("100"), null)));
        assertBearerPost("/open-api/v1/cards/limit/modify", "\"dailyLimit\":10");
        assertFalse(request().body().contains("cardType"), request().body());
    }

    @Test
    void modifyLimitAsyncReturnsOperationRecordId() throws Exception {
        respondData("701");

        assertEquals(701L, client.cards().modifyLimitAsync(
                new CardLimitUpdateRequest(5L, BigDecimal.TEN, new BigDecimal("100"), null)));
        assertBearerPost("/open-api/v1/cards/limit/modify/operation-record", "\"dailyLimit\":10");
        assertFalse(request().body().contains("cardType"), request().body());
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
    void rechargeCallsRechargeEndpointWithoutSignature() throws Exception {
        respondData("601");
        MemberCardRechargeRequest body = new MemberCardRechargeRequest(
                2_079_813_761_576_865_792L, new BigDecimal("25.00"), "funding");

        assertEquals(601L, client.cards().recharge(body));
        assertBearerPost("/open-api/v1/cards/recharge", "\"remark\":\"funding\"");
        assertBearerPost("/open-api/v1/cards/recharge", "\"memberCardId\":\"2079813761576865792\"");
        assertEquals("", request().header("sign"));
    }

    @Test
    void withdrawCallsWithdrawEndpointWithoutSignature() throws Exception {
        respondData("602");
        MemberCardWithdrawRequest body = new MemberCardWithdrawRequest(5L, new BigDecimal("10.00"), "unused");

        assertEquals(602L, client.cards().withdraw(body));
        assertBearerPost("/open-api/v1/cards/withdraw", "\"amount\":10.00");
        assertEquals("", request().header("sign"));
    }

    @Test
    void operationRecordCallsQueryEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"memberCardOperationRecordId\":601,\"memberCardId\":5,"
                + "\"cardType\":\"RECHARGE\",\"operationType\":\"RECHARGE\","
                + "\"amount\":25,\"currencyCode\":\"USD\",\"balance\":75,"
                + "\"status\":\"SUCCESS\",\"message\":\"completed\","
                + "\"createTime\":1788171330000,"
                + "\"updateTime\":1788171390000}],\"extra\":null}");

        RechargeCardOperationRecordResponse result = client.cards().operationRecord(
                new RechargeCardOperationRecordRequest(601L));

        assertNotNull(result);
        assertEquals("SUCCESS", result.status());
        assertEquals(601L, result.memberCardOperationRecordId());
        assertEquals("RECHARGE", result.operationType());
        assertEquals(new BigDecimal("75"), result.balance());
        assertEquals("completed", result.message());
        assertEquals(1788171330000L, result.createTime());
        assertEquals(1788171390000L, result.updateTime());
        assertBearerPost("/open-api/v1/cards/operation-record", "\"memberCardOperationRecordId\":601");
    }

    @Test
    void operationRecordsSupportsMemberCardPagination() throws Exception {
        respondData("{\"total\":2,\"list\":["
                + "{\"memberCardOperationRecordId\":701,\"memberCardId\":5,"
                + "\"cardType\":\"RECHARGE\",\"operationType\":\"MODIFY_LIMITS\","
                + "\"status\":\"PROCESSING\"}],\"extra\":null}");

        PageResultEx<RechargeCardOperationRecordResponse, Object> result = client.cards().operationRecords(
                new RechargeCardOperationRecordRequest(2, 20, null, 5L));

        assertEquals(2L, result.total());
        assertEquals("MODIFY_LIMITS", result.list().get(0).operationType());
        assertBearerPost("/open-api/v1/cards/operation-record", "\"pageNo\":2");
        assertBearerPost("/open-api/v1/cards/operation-record", "\"memberCardId\":5");
    }

    @Test
    void issueDetailsCallsTaskDetailsEndpoint() throws Exception {
        respondData("[{\"memberCardId\":5,\"cardStatus\":\"ACTIVE\",\"message\":\"created\"}]");

        List<IssueCardDetailsResponse> result = client.cards().issueDetails(new IssueCardDetailsRequest(501L));

        assertEquals("created", result.get(0).message());
        assertBearerPost("/open-api/v1/cards/issue/detail", "\"taskId\":501");
    }

}
