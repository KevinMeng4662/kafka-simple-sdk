# Kafka Simple SDK - API Specification

## Overview

This document provides detailed API specifications for the Kafka Simple SDK, including all classes, methods, and their usage patterns.

## Core Classes

### KafkaEvent

Represents a structured event with metadata and payload.

#### Constructor

```java
// Default constructor
public KafkaEvent()

// Constructor with type and payload
public KafkaEvent(String type, Object payload)

// Constructor with type, source, and payload
public KafkaEvent(String type, String source, Object payload)
```

#### Properties

| Property | Type | Description | Auto-generated |
|----------|------|-------------|----------------|
| `id` | String | Unique event identifier | Yes (UUID) |
| `type` | String | Event type (e.g., "user.created") | No |
| `source` | String | Source service/system | No |
| `timestamp` | Instant | Event creation timestamp | Yes |
| `headers` | Map<String, Object> | Custom headers | Yes (empty map) |
| `payload` | Object | Event data/payload | No |

#### Methods

```java
// Header management
public KafkaEvent addHeader(String key, Object value)
public Map<String, Object> getHeaders()
public void setHeaders(Map<String, Object> headers)

// Serialization
public String toJson()
public static KafkaEvent fromJson(String json)

// Standard getters and setters
public String getId()
public void setId(String id)
public String getType()
public void setType(String type)
// ... (other getters/setters)
```

#### Example Usage

```java
// Create event with fluent API
KafkaEvent event = new KafkaEvent("user.created", "user-service", userData)
    .addHeader("correlation-id", "req-123")
    .addHeader("version", "1.0");

// Serialize to JSON
String json = event.toJson();

// Deserialize from JSON
KafkaEvent deserializedEvent = KafkaEvent.fromJson(json);
```

### KafkaEventPublisher

Simplified Kafka producer for publishing events.

#### Constructor

```java
// Basic constructor
public KafkaEventPublisher(String bootstrapServers)

// Constructor with default topic
public KafkaEventPublisher(String bootstrapServers, String defaultTopic)
```

#### Configuration

The publisher uses the following default Kafka producer configurations:

| Property | Value | Description |
|----------|-------|-------------|
| `acks` | "all" | Wait for all replicas to acknowledge |
| `retries` | 3 | Number of retry attempts |
| `enable.idempotence` | true | Enable idempotent producer |
| `max.in.flight.requests.per.connection` | 5 | Max unacknowledged requests |
| `compression.type` | "snappy" | Message compression |

#### Methods

```java
// Publish to default topic
public CompletableFuture<RecordMetadata> publish(KafkaEvent event)

// Publish to specific topic
public CompletableFuture<RecordMetadata> publish(String topic, KafkaEvent event)

// Publish with custom key
public CompletableFuture<RecordMetadata> publish(String topic, String key, KafkaEvent event)

// Publish simple event
public CompletableFuture<RecordMetadata> publish(String topic, String eventType, Object payload)

// Utility methods
public void flush()
public void close()
```

#### Example Usage

```java
try (KafkaEventPublisher publisher = new KafkaEventPublisher("localhost:9092")) {
    KafkaEvent event = new KafkaEvent("order.placed", orderData);
    
    publisher.publish("orders", event)
            .thenAccept(metadata -> 
                System.out.println("Published at offset: " + metadata.offset()))
            .exceptionally(throwable -> {
                System.err.println("Publish failed: " + throwable.getMessage());
                return null;
            });
}
```

### KafkaEventSubscriber

Simplified Kafka consumer for subscribing to events.

#### Constructor

```java
public KafkaEventSubscriber(String bootstrapServers, String groupId)
```

#### Configuration

The subscriber uses the following default Kafka consumer configurations:

| Property | Value | Description |
|----------|-------|-------------|
| `auto.offset.reset` | "earliest" | Start from earliest offset |
| `enable.auto.commit` | true | Enable automatic offset commits |
| `auto.commit.interval.ms` | 1000 | Auto-commit interval |
| `session.timeout.ms` | 30000 | Session timeout |
| `heartbeat.interval.ms` | 3000 | Heartbeat interval |

#### Methods

```java
// Subscription management
public KafkaEventSubscriber subscribe(String... topics)

// Event handling
public KafkaEventSubscriber onEvent(Consumer<KafkaEvent> handler)
public KafkaEventSubscriber onError(Consumer<Exception> handler)

// Lifecycle management
public void start()
public void stop()
public boolean isRunning()
public void close()
```

#### Event Handler Interface

```java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}
```

#### Example Usage

```java
try (KafkaEventSubscriber subscriber = new KafkaEventSubscriber(
        "localhost:9092", "my-group")) {
    
    subscriber.subscribe("orders", "users")
             .onEvent(event -> {
                 System.out.println("Received: " + event.getType());
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

## Event Metadata

When events are consumed, the subscriber automatically adds Kafka metadata to the event headers:

| Header Key | Type | Description |
|------------|------|-------------|
| `kafka.topic` | String | Source topic name |
| `kafka.partition` | Integer | Source partition |
| `kafka.offset` | Long | Message offset |
| `kafka.timestamp` | Long | Kafka record timestamp |
| `kafka.header.*` | String | Original Kafka headers (prefixed) |

## Error Handling

### Publisher Errors

The publisher returns `CompletableFuture<RecordMetadata>` which can be handled:

```java
publisher.publish(topic, event)
        .thenAccept(metadata -> {
            // Success handling
        })
        .exceptionally(throwable -> {
            if (throwable instanceof TimeoutException) {
                // Handle timeout
            } else if (throwable instanceof SerializationException) {
                // Handle serialization error
            }
            return null;
        });
```

### Subscriber Errors

The subscriber provides an error handler for processing errors:

```java
subscriber.onError(error -> {
    if (error instanceof JsonProcessingException) {
        // Handle JSON parsing errors
        logger.error("Failed to parse event JSON", error);
    } else {
        // Handle other errors
        logger.error("Unexpected error", error);
    }
});
```

## Thread Safety

- **KafkaEvent**: Thread-safe for read operations, not thread-safe for modifications
- **KafkaEventPublisher**: Thread-safe, can be used from multiple threads
- **KafkaEventSubscriber**: Thread-safe, uses internal thread for consumption

## Performance Considerations

### Publisher

- Uses asynchronous sending by default
- Batching is handled automatically by Kafka producer
- Call `flush()` to ensure all messages are sent before closing

### Subscriber

- Uses single-threaded consumption model
- For high-throughput scenarios, consider multiple subscriber instances
- Event processing should be non-blocking or use separate thread pool

## Best Practices

### Event Design

```java
// Good: Structured event with clear type and source
KafkaEvent event = new KafkaEvent(
    "order.status.changed",  // Clear, hierarchical type
    "order-service",         // Identifiable source
    Map.of(
        "orderId", orderId,
        "oldStatus", oldStatus,
        "newStatus", newStatus,
        "changedBy", userId
    )
);

// Add correlation ID for tracing
event.addHeader("correlation-id", correlationId);
```

### Publisher Usage

```java
// Use try-with-resources for automatic cleanup
try (KafkaEventPublisher publisher = new KafkaEventPublisher(bootstrapServers)) {
    // Publish events
    // Publisher will be automatically closed
}

// For long-running applications, reuse publisher instance
KafkaEventPublisher publisher = new KafkaEventPublisher(bootstrapServers);
// Use publisher throughout application lifecycle
// Remember to call publisher.close() on shutdown
```

### Subscriber Usage

```java
// Keep event processing lightweight
subscriber.onEvent(event -> {
    // Quick processing or delegate to thread pool
    if (isQuickOperation(event)) {
        processQuickly(event);
    } else {
        threadPool.submit(() -> processSlowly(event));
    }
});

// Always handle errors
subscriber.onError(error -> {
    logger.error("Event processing error", error);
    // Consider dead letter queue for failed events
});
```

## Monitoring and Observability

The SDK provides built-in logging using SLF4J:

- **DEBUG**: Detailed event processing information
- **INFO**: Lifecycle events (start, stop, configuration)
- **WARN**: Recoverable errors and warnings
- **ERROR**: Serious errors that need attention

Example log configuration (logback.xml):

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.github.kevinmeng4662.kafka" level="INFO"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```