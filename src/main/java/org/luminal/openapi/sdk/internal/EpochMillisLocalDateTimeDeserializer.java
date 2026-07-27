package org.luminal.openapi.sdk.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Reads server timestamps as UTC epoch milliseconds or ISO local date-time text. */
final class EpochMillisLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(parser.getLongValue()), ZoneOffset.UTC);
        }
        return LocalDateTime.parse(parser.getValueAsString());
    }
}
