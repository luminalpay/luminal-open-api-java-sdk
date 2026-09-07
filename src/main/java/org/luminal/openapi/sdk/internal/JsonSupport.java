package org.luminal.openapi.sdk.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Shared, immutable-after-configuration JSON support for SDK wire data.
 */
public final class JsonSupport {

    private static final long JS_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(javaTimeModule())
            .addModule(longModule())
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    // Server-side card-issue verification rehydrates Long values before canonicalizing them as JSON numbers.
    private static final ObjectMapper SIGNATURE_MAPPER = JsonMapper.builder()
            .addModule(javaTimeModule())
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private static SimpleModule longModule() {
        LongNumberSerializer serializer = new LongNumberSerializer();
        return new SimpleModule()
                .addSerializer(Long.class, serializer)
                .addSerializer(Long.TYPE, serializer);
    }

    private static JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDateTime.class, new EpochMillisLocalDateTimeDeserializer());
        module.addDeserializer(ZonedDateTime.class, new EpochMillisZonedDateTimeDeserializer());
        return module;
    }

    private static final class LongNumberSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (value > -JS_SAFE_INTEGER && value < JS_SAFE_INTEGER) {
                generator.writeNumber(value);
            } else {
                generator.writeString(value.toString());
            }
        }
    }

    private JsonSupport() {
    }

    /**
     * Serializes a value using the canonical SDK JSON configuration.
     *
     * @param value value to serialize
     * @return UTF-8 JSON bytes
     */
    public static byte[] writeBytes(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Value cannot be serialized as JSON", exception);
        }
    }

    /**
     * Serializes the canonical request form used by the deployed card-issue verifier.
     *
     * @param value value to serialize
     * @return UTF-8 canonical JSON bytes
     */
    public static byte[] writeSignatureBytes(Object value) {
        try {
            return SIGNATURE_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Value cannot be serialized as signature JSON", exception);
        }
    }

    static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Deserializes JSON bytes using the canonical SDK configuration.
     *
     * @param content JSON bytes
     * @param type    target Java class
     * @param <T>     target value type
     * @return decoded value
     * @throws IOException when the JSON cannot be decoded as {@code type}
     */
    public static <T> T readValue(byte[] content, Class<T> type) throws IOException {
        return MAPPER.readValue(content, type);
    }

    /**
     * Creates a Jackson type for a non-generic class.
     *
     * @param type Java class
     * @return Jackson type descriptor
     */
    public static JavaType type(Class<?> type) {
        return MAPPER.getTypeFactory().constructType(type);
    }

    /**
     * Creates a Jackson type for a parameterized class.
     *
     * @param rawType        generic raw class
     * @param parameterTypes generic parameter classes
     * @return Jackson parameterized type descriptor
     */
    public static JavaType parametricType(Class<?> rawType, Class<?>... parameterTypes) {
        return MAPPER.getTypeFactory().constructParametricType(rawType, parameterTypes);
    }
}
