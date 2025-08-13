package com.github.kevinmeng4662.kafka.examples;

import com.github.kevinmeng4662.kafka.KafkaEvent;
import com.github.kevinmeng4662.kafka.KafkaEventPublisher;

import java.util.Map;

/**
 * Simple example demonstrating how to publish events using the Kafka Simple SDK
 */
public class SimplePublisherExample {
    
    public static void main(String[] args) throws Exception {
        String bootstrapServers = "localhost:9092";
        String topic = "user-events";
        
        // Create publisher
        try (KafkaEventPublisher publisher = new KafkaEventPublisher(bootstrapServers)) {
            
            // Example 1: Publish a structured event
            KafkaEvent userCreatedEvent = new KafkaEvent(
                    "user.created", 
                    "user-service", 
                    Map.of(
                        "userId", 123,
                        "username", "john_doe",
                        "email", "john@example.com",
                        "createdAt", System.currentTimeMillis()
                    )
            );
            
            // Add custom headers
            userCreatedEvent.addHeader("correlation-id", "req-123")
                           .addHeader("version", "1.0")
                           .addHeader("source-system", "web-app");
            
            // Publish the event
            publisher.publish(topic, userCreatedEvent)
                    .thenAccept(metadata -> 
                        System.out.println("Published user.created event to " + 
                                         metadata.topic() + " at offset " + metadata.offset()))
                    .exceptionally(throwable -> {
                        System.err.println("Failed to publish event: " + throwable.getMessage());
                        return null;
                    });
            
            // Example 2: Publish a simple event
            publisher.publish(topic, "user.login", 
                    Map.of("userId", 123, "loginTime", System.currentTimeMillis()))
                    .thenAccept(metadata -> 
                        System.out.println("Published user.login event to " + 
                                         metadata.topic() + " at offset " + metadata.offset()));
            
            // Example 3: Publish multiple events
            for (int i = 0; i < 5; i++) {
                KafkaEvent event = new KafkaEvent(
                        "user.activity",
                        "activity-service",
                        Map.of(
                            "userId", 123,
                            "activity", "page_view",
                            "page", "/dashboard",
                            "timestamp", System.currentTimeMillis()
                        )
                );
                
                publisher.publish(topic, "user-" + i, event);
            }
            
            // Ensure all messages are sent
            publisher.flush();
            
            System.out.println("All events published successfully!");
            
            // Wait a bit to see the results
            Thread.sleep(2000);
        }
    }
}