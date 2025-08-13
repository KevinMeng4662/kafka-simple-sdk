# Kafka Simple SDK

A simplified Java SDK for Apache Kafka that provides easy-to-use APIs for event publishing and subscribing. This SDK abstracts away the complexity of Kafka configuration and provides a clean, intuitive interface for event-driven applications.

## Features

- 🚀 **Simple API**: Easy-to-use publisher and subscriber classes
- 📦 **Event Model**: Structured event format with metadata and headers
- 🔄 **Async Support**: CompletableFuture-based asynchronous operations
- 🛡️ **Error Handling**: Built-in error handling and retry mechanisms
- 📊 **Monitoring**: Built-in logging and metrics
- 🧪 **Testing**: Comprehensive test suite with Testcontainers
- 📖 **Examples**: Ready-to-run example applications

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.kevinmeng4662</groupId>
    <artifactId>kafka-simple-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'com.github.kevinmeng4662:kafka-simple-sdk:1.0.0'
```

## Usage

### Publishing Events

```java
import com.github.kevinmeng4662.kafka.KafkaEvent;
import com.github.kevinmeng4662.kafka.KafkaEventPublisher;

// Create publisher
try (KafkaEventPublisher publisher = new KafkaEventPublisher("localhost:9092")) {
    
    // Create and publish an event
    KafkaEvent event = new KafkaEvent(
        "user.created", 
        "user-service", 
        Map.of("userId", 123, "username", "john_doe")
    );
    
    // Add custom headers
    event.addHeader("correlation-id", "req-123")
         .addHeader("version", "1.0");
    
    // Publish asynchronously
    publisher.publish("user-events", event)
            .thenAccept(metadata -> 
                System.out.println("Published to offset: " + metadata.offset()))
            .exceptionally(throwable -> {
                System.err.println("Failed to publish: " + throwable.getMessage());
                return null;
            });
}
```

### Subscribing to Events

```java
import com.github.kevinmeng4662.kafka.KafkaEventSubscriber;

// Create subscriber
try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
        "localhost:9092", "my-consumer-group")) {
    
    // Subscribe and handle events
    subscriber.subscribe("user-events", "order-events")
             .onEvent(event -> {
                 System.out.println("Received: " + event.getType());
                 // Process the event
                 processEvent(event);
             })
             .onError(error -> {
                 System.err.println("Error: " + error.getMessage());
             })
             .start();
    
    // Keep running
    Thread.currentThread().join();
}
```

## Event Model

The `KafkaEvent` class provides a structured format for events:

```java
public class KafkaEvent {
    private String id;           // Unique event ID (auto-generated)
    private String type;         // Event type (e.g., "user.created")
    private String source;       // Source service/system
    private Instant timestamp;   // Event timestamp (auto-generated)
    private Map<String, Object> headers;  // Custom headers
    private Object payload;      // Event payload/data
}
```

### Event Serialization

Events are automatically serialized to/from JSON:

```java
// Serialize to JSON
String json = event.toJson();

// Deserialize from JSON
KafkaEvent event = KafkaEvent.fromJson(json);
```

## Configuration

### Publisher Configuration

The publisher uses sensible defaults but can be customized:

```java
// With default topic
KafkaEventPublisher publisher = new KafkaEventPublisher(
    "localhost:9092", 
    "default-topic"
);

// Publish to default topic
publisher.publish(event);
```

### Subscriber Configuration

The subscriber automatically handles:
- Consumer group management
- Offset management
- Error handling
- Graceful shutdown

```java
KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
    "localhost:9092",    // Bootstrap servers
    "consumer-group-1"   // Consumer group ID
);
```

## Advanced Usage

### Custom Headers

```java
KafkaEvent event = new KafkaEvent("order.placed", orderData)
    .addHeader("correlation-id", correlationId)
    .addHeader("user-id", userId)
    .addHeader("version", "2.0")
    .addHeader("priority", "high");
```

### Error Handling

```java
subscriber.onError(error -> {
    if (error instanceof SerializationException) {
        // Handle serialization errors
        logger.error("Failed to deserialize event", error);
    } else if (error instanceof TimeoutException) {
        // Handle timeout errors
        logger.warn("Consumer timeout", error);
    } else {
        // Handle other errors
        logger.error("Unexpected error", error);
    }
});
```

### Multiple Topic Subscription

```java
subscriber.subscribe("user-events", "order-events", "notification-events")
         .onEvent(event -> {
             switch (event.getType()) {
                 case "user.created":
                     handleUserCreated(event);
                     break;
                 case "order.placed":
                     handleOrderPlaced(event);
                     break;
                 default:
                     logger.info("Unknown event type: {}", event.getType());
             }
         });
```

## Testing

The SDK includes comprehensive tests using Testcontainers:

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests KafkaEventPublisherTest

# Run integration tests
./gradlew test --tests IntegrationTest
```

## Examples

See the `examples` package for complete working examples:

- `SimplePublisherExample.java` - Basic event publishing
- `SimpleSubscriberExample.java` - Basic event subscription

To run examples:

```bash
# Start Kafka (using Docker)
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluentinc/cp-kafka:7.4.0

# Run publisher example
./gradlew run -PmainClass=com.github.kevinmeng4662.kafka.examples.SimplePublisherExample

# Run subscriber example (in another terminal)
./gradlew run -PmainClass=com.github.kevinmeng4662.kafka.examples.SimpleSubscriberExample
```

## Requirements

- Java 11 or higher
- Apache Kafka 2.8+ (for broker compatibility)

## Dependencies

- Apache Kafka Clients 3.5.1
- Jackson Databind 2.15.2
- SLF4J API 2.0.7
- Logback Classic 1.4.8

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions, issues, or contributions, please:

1. Check the [Issues](https://github.com/KevinMeng4662/kafka-simple-sdk/issues) page
2. Create a new issue if needed
3. Provide detailed information about your use case

## Changelog

### Version 1.0.0
- Initial release
- Basic publisher and subscriber functionality
- Event model with JSON serialization
- Comprehensive test suite
- Example applications
- Full documentation