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

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class IntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEmbeddedZookeeper();

    @Test
    void testEndToEndEventFlow() throws Exception {
        String topic = "integration-test-topic";
        List<KafkaEvent> receivedEvents = new ArrayList<>();
        
        // Start subscriber
        try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
                kafka.getBootstrapServers(), "integration-group")) {
            
            subscriber.subscribe(topic)
                     .onEvent(event -> {
                         System.out.println("Received event: " + event);
                         receivedEvents.add(event);
                     })
                     .onError(error -> {
                         System.err.println("Error processing event: " + error.getMessage());
                         error.printStackTrace();
                     })
                     .start();
            
            // Publish events
            try (KafkaEventPublisher publisher = new KafkaEventPublisher(kafka.getBootstrapServers())) {
                // Publish different types of events
                KafkaEvent userEvent = new KafkaEvent("user.created", "user-service", 
                        Map.of("userId", 123, "username", "john_doe", "email", "john@example.com"));
                userEvent.addHeader("correlation-id", "user-123")
                        .addHeader("version", "1.0");
                
                KafkaEvent orderEvent = new KafkaEvent("order.placed", "order-service", 
                        Map.of("orderId", 456, "userId", 123, "amount", 99.99));
                orderEvent.addHeader("correlation-id", "order-456")
                         .addHeader("version", "2.0");
                
                KafkaEvent notificationEvent = new KafkaEvent("notification.sent", "notification-service", 
                        Map.of("notificationId", 789, "userId", 123, "type", "email"));
                
                // Publish all events
                publisher.publish(topic, userEvent).get();
                publisher.publish(topic, orderEvent).get();
                publisher.publish(topic, notificationEvent).get();
                publisher.flush();
            }
            
            // Wait for all events to be consumed
            Awaitility.await()
                     .atMost(Duration.ofSeconds(30))
                     .until(() -> receivedEvents.size() == 3);
            
            assertEquals(3, receivedEvents.size());
            
            // Verify events
            KafkaEvent userEvent = receivedEvents.stream()
                    .filter(e -> "user.created".equals(e.getType()))
                    .findFirst()
                    .orElseThrow();
            
            assertEquals("user-service", userEvent.getSource());
            assertEquals("user-123", userEvent.getHeaders().get("correlation-id"));
            assertNotNull(userEvent.getHeaders().get("kafka.topic"));
            assertNotNull(userEvent.getHeaders().get("kafka.offset"));
            
            KafkaEvent orderEvent = receivedEvents.stream()
                    .filter(e -> "order.placed".equals(e.getType()))
                    .findFirst()
                    .orElseThrow();
            
            assertEquals("order-service", orderEvent.getSource());
            assertEquals("order-456", orderEvent.getHeaders().get("correlation-id"));
            
            KafkaEvent notificationEvent = receivedEvents.stream()
                    .filter(e -> "notification.sent".equals(e.getType()))
                    .findFirst()
                    .orElseThrow();
            
            assertEquals("notification-service", notificationEvent.getSource());
        }
    }
}