package org.luminal.openapi.sdk.internal;

import org.junit.jupiter.api.Test;
import org.luminal.openapi.sdk.model.CardModels.CardLimitUpdateRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSupportTest {

    @Test
    void serializesLongValuesUsingJavaScriptSafeIntegerRule() {
        byte[] json = JsonSupport.writeBytes(Map.of(
                "amount", 100L,
                "memberNo", 2_064_991_632_710_991_874L,
                "max", Long.MAX_VALUE,
                "min", Long.MIN_VALUE));

        assertEquals(
                "{\"amount\":100,\"max\":\"9223372036854775807\",\"memberNo\":\"2064991632710991874\",\"min\":\"-9223372036854775808\"}",
                new String(json));
    }

    @Test
    void readsInt64FromJsonNumbersAndStrings() throws Exception {
        record Payload(Long numberValue, Long stringValue) {
        }

        Payload payload = JsonSupport.readValue(
                "{\"numberValue\":2011314055960203265,\"stringValue\":\"2079813737300729858\"}".getBytes(),
                Payload.class);

        assertEquals(2_011_314_055_960_203_265L, payload.numberValue());
        assertEquals(2_079_813_737_300_729_858L, payload.stringValue());
    }

    @Test
    void serializesNullableLongFields() {
        record Payload(Long amount, Long memberNo, Long optional) {
        }

        byte[] json = JsonSupport.writeBytes(new Payload(100L, 2_064_991_632_710_991_874L, null));

        assertEquals(
                "{\"amount\":100,\"memberNo\":\"2064991632710991874\"}",
                new String(json));
    }

    @Test
    void doesNotSerializeLegacyCardTypeForLimitUpdates() {
        CardLimitUpdateRequest request = new CardLimitUpdateRequest(
                5L, "RECHARGE", BigDecimal.TEN, new BigDecimal("100.00"), null);

        String json = new String(JsonSupport.writeBytes(request));

        assertEquals("RECHARGE", request.cardType());
        assertFalse(json.contains("cardType"), json);
        assertTrue(json.contains("\"dailyLimit\":10"), json);
        assertTrue(json.contains("\"monthLimit\":100.00"), json);
    }

    @Test
    void readsEpochMillisAsUtcLocalDateTime() throws Exception {
        record Payload(LocalDateTime createTime) {
        }

        Payload payload = JsonSupport.readValue(
                "{\"createTime\":1704067200000}".getBytes(), Payload.class);

        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), payload.createTime());
    }

    @Test
    void readsEpochMillisAndIsoTextAsZonedDateTime() throws Exception {
        record Payload(ZonedDateTime createTime, ZonedDateTime updateTime) {
        }

        Payload payload = JsonSupport.readValue(
                ("{\"createTime\":1704067200000,"
                        + "\"updateTime\":\"2024-01-01T01:00:00+01:00\"}").getBytes(), Payload.class);

        assertEquals(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), payload.createTime());
        assertEquals(ZonedDateTime.of(2024, 1, 1, 1, 0, 0, 0, ZoneOffset.ofHours(1)), payload.updateTime());
    }

    @Test
    void serializesInt64BoundariesUsingJavaScriptSafeIntegerRule() {
        byte[] json = JsonSupport.writeBytes(Map.of(
                "below", -9_007_199_254_740_992L,
                "min", -9_007_199_254_740_991L,
                "safeNegative", -9_007_199_254_740_990L,
                "safePositive", 9_007_199_254_740_990L,
                "max", 9_007_199_254_740_991L,
                "above", 9_007_199_254_740_992L));

        assertEquals(
                "{\"above\":\"9007199254740992\",\"below\":\"-9007199254740992\",\"max\":\"9007199254740991\",\"min\":\"-9007199254740991\",\"safeNegative\":-9007199254740990,\"safePositive\":9007199254740990}",
                new String(json));
    }
}
