package network;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Singleton ObjectMapper dùng chung cho REST + WebSocket.
 * Cấu hình hỗ trợ LocalDateTime (ISO-8601) để khớp với MessageDTO ở common-lib.
 */
public final class JsonMapper {

    private static final ObjectMapper MAPPER = build();

    private JsonMapper() {}

    public static ObjectMapper get() {
        return MAPPER;
    }

    private static ObjectMapper build() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return m;
    }
}
