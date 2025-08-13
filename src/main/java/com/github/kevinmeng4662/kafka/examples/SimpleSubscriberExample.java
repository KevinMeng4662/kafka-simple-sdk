package com.github.kevinmeng4662.kafka.examples;

import com.github.kevinmeng4662.kafka.KafkaEvent;
import com.github.kevinmeng4662.kafka.KafkaEventSubscriber;

/**
 * Simple example demonstrating how to subscribe to events using the Kafka Simple SDK
 */
public class SimpleSubscriberExample {
    
    public static void main(String[] args) throws Exception {
        String bootstrapServers = "localhost:9092";
        String groupId = "example-consumer-group";
        
        // Create subscriber
        try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(bootstrapServers, groupId)) {
            
            // Subscribe to topics and set event handler
            subscriber.subscribe("user-events", "order-events")
                     .onEvent(event -> {
                         System.out.println("\n=== Received Event ===");
                         System.out.println("ID: " + event.getId());
                         System.out.println("Type: " + event.getType());
                         System.out.println("Source: " + event.getSource());
                         System.out.println("Timestamp: " + event.getTimestamp());
                         System.out.println("Payload: " + event.getPayload());
                         System.out.println("Headers: " + event.getHeaders());
                         
                         // Process different event types
                         switch (event.getType()) {
                             case "user.created":
                                 handleUserCreated(event);
                                 break;
                             case "user.login":
                                 handleUserLogin(event);
                                 break;
                             case "order.placed":
                                 handleOrderPlaced(event);
                                 break;
                             default:
                                 System.out.println("Unknown event type: " + event.getType());
                         }
                     })
                     .onError(error -> {
                         System.err.println("Error processing event: " + error.getMessage());
                         error.printStackTrace();
                     });
            
            // Start consuming
            subscriber.start();
            
            System.out.println("Subscriber started. Waiting for events...");
            System.out.println("Press Ctrl+C to stop");
            
            // Keep the application running
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down subscriber...");
                subscriber.stop();
            }));
            
            // Wait indefinitely
            Thread.currentThread().join();
        }
    }
    
    private static void handleUserCreated(KafkaEvent event) {
        System.out.println("Processing user creation...");
        // Add your business logic here
        // e.g., send welcome email, create user profile, etc.
    }
    
    private static void handleUserLogin(KafkaEvent event) {
        System.out.println("Processing user login...");
        // Add your business logic here
        // e.g., update last login time, log security event, etc.
    }
    
    private static void handleOrderPlaced(KafkaEvent event) {
        System.out.println("Processing order placement...");
        // Add your business logic here
        // e.g., update inventory, send confirmation email, etc.
    }
}