package com.github.kevinmeng4662.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a Kafka event with standard metadata and payload
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaEvent {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .findAndRegisterModules();

    private String id;
    private String type;
    private String source;
    private Instant timestamp;
    private Map<String, Object> headers;
    private Object payload;

    public KafkaEvent() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.headers = new HashMap<>();
    }

    public KafkaEvent(String type, Object payload) {
        this();
        this.type = type;
        this.payload = payload;
    }

    public KafkaEvent(String type, String source, Object payload) {
        this(type, payload);
        this.source = source;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    // Utility methods
    public KafkaEvent addHeader(String key, Object value) {
        this.headers.put(key, value);
        return this;
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event to JSON", e);
        }
    }

    public static KafkaEvent fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, KafkaEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event from JSON", e);
        }
    }

    @Override
    public String toString() {
        return "KafkaEvent{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", headers=" + headers +
                ", payload=" + payload +
                '}';
    }
}