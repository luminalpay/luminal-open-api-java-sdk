package org.luminal.openapi.sdk.api;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.ApiTestSupport;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCardPageRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCardResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCountryResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderCreateRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderDetailResponse;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderModifyRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderPageRequest;
import org.luminal.openapi.sdk.model.CardHolderModels.CardHolderPageResponse;
import org.luminal.openapi.sdk.model.CommonModels.PageResult;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardHoldersApiTest extends ApiTestSupport {

    @Test
    void countriesCallsCountryListEndpoint() throws Exception {
        respondData("[{\"countryId\":244,\"countryName\":\"Hong Kong\","
                + "\"countryCode\":\"HK\",\"areaCode\":\"852\",\"phoneMaxLength\":8}]");

        List<CardHolderCountryResponse> result = client.cardHolders().countries();

        assertEquals(244L, result.get(0).countryId());
        assertEquals("852", result.get(0).areaCode());
        assertEquals(8, result.get(0).phoneMaxLength());
        assertBearerGet("/open-api/v1/card-holders/countries");
    }

    @Test
    void addCallsCardholderCreateEndpoint() throws Exception {
        respondData("801");
        CardHolderCreateRequest request = createRequest();

        assertEquals(801L, client.cardHolders().add(request));
        assertBearerPost("/open-api/v1/card-holders/add", "\"lastName\":\"Smith\"");
        assertBearerPost("/open-api/v1/card-holders/add", "\"birthDate\":\"1990-01-15\"");
    }

    @Test
    void modifyCallsCardholderUpdateEndpoint() throws Exception {
        respondData("null");
        CardHolderCreateRequest create = createRequest();

        client.cardHolders().modify(new CardHolderModifyRequest(
                801L, create.lastName(), create.firstName(), create.birthDate(), create.mail(), create.phone(),
                create.areaCode(), create.countryId(), create.postalCode(), create.state(), create.city(),
                create.addressLine1(), "Suite 12"));

        assertBearerPost("/open-api/v1/card-holders/modify", "\"cardHolderId\":801");
        assertBearerPost("/open-api/v1/card-holders/modify", "\"addressLine2\":\"Suite 12\"");
    }

    @Test
    void detailCallsPathEndpointAndDecodesResponse() throws Exception {
        respondData("{\"cardHolderId\":801,\"lastName\":\"Smith\",\"firstName\":\"John\","
                + "\"birthDate\":\"1990-01-15\",\"mail\":\"john.smith@example.com\","
                + "\"createTime\":1788220800000}");

        CardHolderDetailResponse result = client.cardHolders().detail(801L);

        assertEquals("Smith", result.lastName());
        assertEquals(LocalDate.of(1990, 1, 15), result.birthDate());
        assertEquals(2026, result.createTime().getYear());
        assertBearerPost("/open-api/v1/card-holders/info/801", "{}");
    }

    @Test
    void pageCallsCardholderPageEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"cardHolderId\":801,\"fullName\":\"John Smith\","
                + "\"fullPhone\":\"+12025550123\",\"mail\":\"john.smith@example.com\","
                + "\"createTime\":1788220800000}]}");

        PageResult<CardHolderPageResponse> result = client.cardHolders().page(
                new CardHolderPageRequest(1, 20, 801L, "John", "202555", "john.smith@example.com",
                        new Long[]{1788134400000L, 1788220800000L}));

        assertEquals(1L, result.total());
        assertEquals("John Smith", result.list().get(0).fullName());
        assertBearerPost("/open-api/v1/card-holders/page", "\"createTime\":[1788134400000,1788220800000]");
    }

    @Test
    void associatedCardsCallsCardholderCardPageEndpoint() throws Exception {
        respondData("{\"total\":1,\"list\":[{\"memberCardId\":901,"
                + "\"maskCardNo\":\"515783******1234\",\"openTime\":1788220800000}]}");

        PageResult<CardHolderCardResponse> result = client.cardHolders().associatedCards(
                new CardHolderCardPageRequest(1, 20, 801L, 901L));

        assertEquals(901L, result.list().get(0).memberCardId());
        assertEquals(2026, result.list().get(0).openTime().getYear());
        assertBearerPost("/open-api/v1/card-holders/card/page", "\"cardHolderId\":801");
    }

    private static CardHolderCreateRequest createRequest() {
        return new CardHolderCreateRequest(
                "Smith", "John", LocalDate.of(1990, 1, 15), "john.smith@example.com", "2025550123", "1",
                100000000000000001L, "10001", "New York", "New York", "350 Fifth Avenue", null);
    }
}
