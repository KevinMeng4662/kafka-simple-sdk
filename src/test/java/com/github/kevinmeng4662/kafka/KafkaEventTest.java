package com.github.kevinmeng4662.kafka;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaEventTest {

    @Test
    void testEventCreation() {
        KafkaEvent event = new KafkaEvent("user.created", "user-service", 
                Map.of("userId", 123, "username", "john_doe"));
        
        assertNotNull(event.getId());
        assertEquals("user.created", event.getType());
        assertEquals("user-service", event.getSource());
        assertNotNull(event.getTimestamp());
        assertNotNull(event.getPayload());
        assertTrue(event.getTimestamp().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void testEventSerialization() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 123);
        payload.put("username", "john_doe");
        payload.put("email", "john@example.com");
        
        KafkaEvent originalEvent = new KafkaEvent("user.created", "user-service", payload);
        originalEvent.addHeader("correlation-id", "abc-123")
                    .addHeader("version", "1.0");
        
        String json = originalEvent.toJson();
        assertNotNull(json);
        assertTrue(json.contains("user.created"));
        assertTrue(json.contains("user-service"));
        
        KafkaEvent deserializedEvent = KafkaEvent.fromJson(json);
        assertEquals(originalEvent.getId(), deserializedEvent.getId());
        assertEquals(originalEvent.getType(), deserializedEvent.getType());
        assertEquals(originalEvent.getSource(), deserializedEvent.getSource());
        assertEquals(originalEvent.getHeaders().get("correlation-id"), 
                    deserializedEvent.getHeaders().get("correlation-id"));
    }

    @Test
    void testEventHeaders() {
        KafkaEvent event = new KafkaEvent();
        
        event.addHeader("key1", "value1")
             .addHeader("key2", 42)
             .addHeader("key3", true);
        
        assertEquals("value1", event.getHeaders().get("key1"));
        assertEquals(42, event.getHeaders().get("key2"));
        assertEquals(true, event.getHeaders().get("key3"));
    }

    @Test
    void testInvalidJsonDeserialization() {
        assertThrows(RuntimeException.class, () -> {
            KafkaEvent.fromJson("invalid json");
        });
    }
}