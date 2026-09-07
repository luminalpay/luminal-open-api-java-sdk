package org.luminal.openapi.sdk.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Reads server timestamps as UTC epoch milliseconds or ISO zoned date-time text.
 */
final class EpochMillisZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    @Override
    public ZonedDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(parser.getLongValue()), ZoneOffset.UTC);
        }
        return ZonedDateTime.parse(parser.getValueAsString());
    }
}
