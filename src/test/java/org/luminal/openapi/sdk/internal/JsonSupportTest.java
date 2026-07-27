package org.luminal.openapi.sdk.internal;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void readsEpochMillisAsUtcLocalDateTime() throws Exception {
        record Payload(LocalDateTime createTime) {
        }

        Payload payload = JsonSupport.readValue(
                "{\"createTime\":1704067200000}".getBytes(), Payload.class);

        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), payload.createTime());
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
