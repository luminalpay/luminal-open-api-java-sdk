package org.luminal.openapi.sdk.api;

import org.luminal.openapi.sdk.RsaSignatures;
import org.luminal.openapi.sdk.internal.HttpTransport;
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

import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Card operations exposed by the Luminal Open API.
 */
public final class CardsApi {

    private static final String PATH = "/open-api/v1/cards";
    private final HttpTransport transport;

    /**
     * Creates the card API.
     *
     * @param transport shared HTTP transport
     */
    public CardsApi(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /**
     * Lists available card BIN products.
     *
     * @param request page and card-product filters
     * @return matching card BIN products and page metadata
     */
    public PageResultEx<CardBinResponse, Object> bins(CardBinsRequest request) {
        return page("/bins", request, CardBinResponse.class);
    }

    /**
     * Submits a card issuance request with a precomputed Base64 signature.
     *
     * @param request   card issuance parameters
     * @param signature non-blank Base64 signature of the canonical request bytes
     * @return issuance task identifier
     */
    public Long issue(IssueCardRequest request, String signature) {
        byte[] body = transport.serialize(Objects.requireNonNull(request, "request"));
        return issue(body, HttpTransport.requireNonBlank(signature, "signature"));
    }

    /**
     * Signs and submits a card issuance request.
     *
     * @param request    card issuance parameters
     * @param privateKey RSA private key used with {@code SHA256withRSA}
     * @return issuance task identifier
     */
    public Long issue(IssueCardRequest request, PrivateKey privateKey) {
        IssueCardRequest value = Objects.requireNonNull(request, "request");
        byte[] body = transport.serialize(value);
        return issue(body, RsaSignatures.sign(JsonSupport.writeSignatureBytes(value),
                Objects.requireNonNull(privateKey, "privateKey")));
    }

    /**
     * Lists issued cards belonging to the current member.
     *
     * @param request page, card, status, BIN, type, keyword, and group filters
     * @return matching issued cards and page metadata
     */
    public PageResultEx<MemberCardResponse, Object> list(MemberCardPageRequest request) {
        return page("/list", request, MemberCardResponse.class);
    }

    /**
     * Retrieves sensitive card number, CVV, and expiry data.
     *
     * @param request target member-card identifier
     * @return sensitive card details; avoid logging or persisting this value
     */
    public CardCvvResponse cvv(CardIdRequest request) {
        return post("/cvv", request, CardCvvResponse.class);
    }

    /**
     * Lists card transactions.
     *
     * @param request page, card type, card identifier, and trade-time filters
     * @return matching card transactions and page metadata
     */
    public PageResultEx<CardTransactionResponse, Object> transactions(CardTransactionsRequest request) {
        return page("/transactions", request, CardTransactionResponse.class);
    }

    /**
     * Retrieves the current card limit and balance.
     *
     * @param request target member-card identifier
     * @return configured total limit and available balance
     */
    public CardLimitResponse limit(CardIdRequest request) {
        return post("/limit", request, CardLimitResponse.class);
    }

    /**
     * Updates a card limit.
     *
     * @param request target member-card identifier and new total limit
     * @return {@code true} when the server reports success
     */
    public boolean modifyLimit(CardLimitUpdateRequest request) {
        return action("/limit/modify", request);
    }

    /**
     * Freezes a card.
     *
     * @param request target member-card identifier
     * @return {@code true} when the server reports success
     */
    public boolean freeze(CardIdRequest request) {
        return action("/freeze", request);
    }

    /**
     * Unfreezes a card.
     *
     * @param request target member-card identifier
     * @return {@code true} when the server reports success
     */
    public boolean unfreeze(CardIdRequest request) {
        return action("/unfreeze", request);
    }

    /**
     * Cancels a card.
     *
     * @param request target member-card identifier
     * @return {@code true} when the server reports success
     */
    public boolean cancel(CardIdRequest request) {
        return action("/cancel", request);
    }

    /**
     * Retrieves individual card results for an issuance task.
     *
     * @param request card issuance task identifier
     * @return immutable list of per-card issuance results
     */
    public List<IssueCardDetailsResponse> issueDetails(IssueCardDetailsRequest request) {
        return transport.postAuthorized(PATH + "/issue/detail", Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(List.class, IssueCardDetailsResponse.class));
    }

    private Long issue(byte[] body, String signature) {
        return transport.postSerializedAuthorized(PATH + "/issue", body, Map.of("sign", signature),
                JsonSupport.type(Long.class));
    }

    private boolean action(String path, Object request) {
        return transport.postAuthorizedBoolean(PATH + path, Objects.requireNonNull(request, "request"));
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        return transport.postAuthorized(PATH + path, Objects.requireNonNull(request, "request"),
                JsonSupport.type(responseType));
    }

    private <T> PageResultEx<T, Object> page(String path, Object request, Class<T> itemType) {
        return transport.postAuthorized(PATH + path, Objects.requireNonNull(request, "request"),
                JsonSupport.parametricType(PageResultEx.class, itemType, Object.class));
    }
}
