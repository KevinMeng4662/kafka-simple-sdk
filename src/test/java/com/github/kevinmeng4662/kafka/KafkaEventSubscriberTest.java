package com.github.kevinmeng4662.kafka;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class KafkaEventSubscriberTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEmbeddedZookeeper();

    @Test
    void testSubscribeAndConsume() throws Exception {
        String topic = "test-subscribe-topic";
        List<KafkaEvent> receivedEvents = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // Create subscriber
        try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
                kafka.getBootstrapServers(), "test-group")) {
            
            subscriber.subscribe(topic)
                     .onEvent(receivedEvents::add)
                     .onError(e -> errorCount.incrementAndGet())
                     .start();
            
            assertTrue(subscriber.isRunning());
            
            // Publish some events
            try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers())) {
                for (int i = 0; i < 3; i++) {
                    KafkaEvent event = new KafkaEvent("test.event." + i, "test-service", 
                            Map.of("index", i, "message", "Test message " + i));
                    publisher.publish(topic, event).get();
                }
                publisher.flush();
            }
            
            // Wait for events to be consumed
            Awaitility.await()
                     .atMost(Duration.ofSeconds(30))
                     .until(() -> receivedEvents.size() == 3);
            
            assertEquals(3, receivedEvents.size());
            assertEquals(0, errorCount.get());
            
            // Verify event content
            for (int i = 0; i < 3; i++) {
                KafkaEvent event = receivedEvents.get(i);
                assertEquals("test.event." + i, event.getType());
                assertEquals("test-service", event.getSource());
                assertNotNull(event.getHeaders().get("kafka.topic"));
                assertNotNull(event.getHeaders().get("kafka.partition"));
                assertNotNull(event.getHeaders().get("kafka.offset"));
            }
            
            subscriber.stop();
            assertFalse(subscriber.isRunning());
        }
    }

    @Test
    void testSubscriberWithoutEventHandler() {
        try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
                kafka.getBootstrapServers(), "test-group")) {
            
            subscriber.subscribe("test-topic");
            
            assertThrows(IllegalStateException.class, subscriber::start);
        }
    }

    @Test
    void testMultipleTopicSubscription() throws Exception {
        List<KafkaEvent> receivedEvents = new ArrayList<>();
        
        try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
                kafka.getBootstrapServers(), "multi-topic-group")) {
            
            subscriber.subscribe("topic1", "topic2")
                     .onEvent(receivedEvents::add)
                     .start();
            
            // Publish to both topics
            try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers())) {
                publisher.publish("topic1", "event.topic1", Map.of("source", "topic1")).get();
                publisher.publish("topic2", "event.topic2", Map.of("source", "topic2")).get();
                publisher.flush();
            }
            
            // Wait for events
            Awaitility.await()
                     .atMost(Duration.ofSeconds(30))
                     .until(() -> receivedEvents.size() == 2);
            
            assertEquals(2, receivedEvents.size());
        }
    }
}