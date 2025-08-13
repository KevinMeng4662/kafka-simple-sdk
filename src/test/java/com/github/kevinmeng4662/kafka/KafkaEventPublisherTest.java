package com.github.kevinmeng4662.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class KafkaEventPublisherTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEmbeddedZookeeper();

    @Test
    void testPublishEvent() throws Exception {
        try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers())) {
            KafkaEvent event = new KafkaEvent("test.event", "test-service", 
                    Map.of("message", "Hello Kafka!"));
            
            CompletableFuture<RecordMetadata> future = publisher.publish("test-topic", event);
            RecordMetadata metadata = future.get(10, TimeUnit.SECONDS);
            
            assertNotNull(metadata);
            assertEquals("test-topic", metadata.topic());
            assertTrue(metadata.offset() >= 0);
        }
    }

    @Test
    void testPublishWithDefaultTopic() throws Exception {
        try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers(), "default-topic")) {
            KafkaEvent event = new KafkaEvent("test.event", Map.of("data", "test"));
            
            CompletableFuture<RecordMetadata> future = publisher.publish(event);
            RecordMetadata metadata = future.get(10, TimeUnit.SECONDS);
            
            assertNotNull(metadata);
            assertEquals("default-topic", metadata.topic());
        }
    }

    @Test
    void testPublishSimpleEvent() throws Exception {
        try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers())) {
            CompletableFuture<RecordMetadata> future = publisher.publish("test-topic", "simple.event", 
                    Map.of("key", "value"));
            RecordMetadata metadata = future.get(10, TimeUnit.SECONDS);
            
            assertNotNull(metadata);
            assertEquals("test-topic", metadata.topic());
        }
    }

    @Test
    void testPublishWithoutDefaultTopic() {
        try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers())) {
            KafkaEvent event = new KafkaEvent("test.event", Map.of("data", "test"));
            
            assertThrows(IllegalStateException.class, () -> {
                publisher.publish(event);
            });
        }
    }
}