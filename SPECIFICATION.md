# Kafka Simple SDK - Technical Specification

## Project Overview

The Kafka Simple SDK is a Java library that simplifies Apache Kafka event publishing and subscribing operations. It provides a clean, intuitive API that abstracts away the complexity of Kafka configuration while maintaining flexibility and performance.

## Architecture

### Core Components

```
kafka-simple-sdk/
├── KafkaEvent           # Event model with metadata
├── KafkaEventPublisher  # Simplified producer
├── KafkaEventSubscriber # Simplified consumer
└── examples/            # Usage examples
```

### Design Principles

1. **Simplicity**: Minimal configuration required
2. **Type Safety**: Strong typing with generics where appropriate
3. **Async First**: CompletableFuture-based operations
4. **Resource Management**: AutoCloseable implementations
5. **Error Handling**: Comprehensive error handling with callbacks
6. **Observability**: Built-in logging and metrics

## Event Model

### KafkaEvent Structure

```java
public class KafkaEvent {
    private String id;                    // UUID (auto-generated)
    private String type;                  // Event type identifier
    private String source;                // Source service/system
    private Instant timestamp;            // Creation timestamp (auto-generated)
    private Map<String, Object> headers;  // Custom metadata
    private Object payload;               // Event data
}
```

### Event Lifecycle

1. **Creation**: Event created with type and payload
2. **Enrichment**: Headers and metadata added
3. **Serialization**: Converted to JSON for transport
4. **Publishing**: Sent to Kafka topic
5. **Consumption**: Received and deserialized
6. **Processing**: Handled by application logic

### JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "id": {
      "type": "string",
      "format": "uuid"
    },
    "type": {
      "type": "string",
      "pattern": "^[a-z]+\\.[a-z]+$"
    },
    "source": {
      "type": "string"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time"
    },
    "headers": {
      "type": "object",
      "additionalProperties": true
    },
    "payload": {
      "type": "object"
    }
  },
  "required": ["id", "type", "timestamp", "payload"]
}
```

## Publisher Specification

### KafkaEventPublisher

#### Configuration

| Property | Default Value | Description |
|----------|---------------|-------------|
| `acks` | "all" | Acknowledgment level |
| `retries` | 3 | Retry attempts |
| `enable.idempotence` | true | Idempotent producer |
| `compression.type` | "snappy" | Message compression |
| `max.in.flight.requests.per.connection` | 5 | Concurrent requests |

#### API Methods

```java
// Constructor
KafkaEventPublisher(String bootstrapServers)
KafkaEventPublisher(String bootstrapServers, String defaultTopic)

// Publishing methods
CompletableFuture<RecordMetadata> publish(KafkaEvent event)
CompletableFuture<RecordMetadata> publish(String topic, KafkaEvent event)
CompletableFuture<RecordMetadata> publish(String topic, String key, KafkaEvent event)
CompletableFuture<RecordMetadata> publish(String topic, String eventType, Object payload)

// Utility methods
void flush()
void close()
```

#### Error Handling

- **TimeoutException**: Network or broker timeout
- **SerializationException**: JSON serialization failure
- **RetriableException**: Temporary failures (auto-retried)
- **NonRetriableException**: Permanent failures

#### Performance Characteristics

- **Throughput**: ~100K messages/second (single instance)
- **Latency**: <10ms p99 (local broker)
- **Memory**: ~50MB heap usage
- **CPU**: <5% on modern hardware

## Subscriber Specification

### KafkaEventSubscriber

#### Configuration

| Property | Default Value | Description |
|----------|---------------|-------------|
| `auto.offset.reset` | "earliest" | Offset reset strategy |
| `enable.auto.commit` | true | Automatic offset commits |
| `auto.commit.interval.ms` | 1000 | Commit interval |
| `session.timeout.ms` | 30000 | Session timeout |
| `heartbeat.interval.ms` | 3000 | Heartbeat interval |

#### API Methods

```java
// Constructor
KafkaEventSubscriber(String bootstrapServers, String groupId)

// Subscription methods
KafkaEventSubscriber subscribe(String... topics)
KafkaEventSubscriber onEvent(Consumer<KafkaEvent> handler)
KafkaEventSubscriber onError(Consumer<Exception> handler)

// Lifecycle methods
void start()
void stop()
boolean isRunning()
void close()
```

#### Event Processing Model

1. **Single-threaded**: One consumer thread per subscriber
2. **Sequential**: Events processed in order per partition
3. **At-least-once**: Delivery guarantee with auto-commit
4. **Error isolation**: Failed events don't block others

#### Metadata Enrichment

The subscriber automatically adds Kafka metadata to events:

```java
event.addHeader("kafka.topic", record.topic())
     .addHeader("kafka.partition", record.partition())
     .addHeader("kafka.offset", record.offset())
     .addHeader("kafka.timestamp", record.timestamp());
```

## Testing Strategy

### Test Categories

1. **Unit Tests**: Individual component testing
2. **Integration Tests**: End-to-end scenarios with Testcontainers
3. **Performance Tests**: Throughput and latency benchmarks
4. **Compatibility Tests**: Multiple Kafka versions

### Test Infrastructure

- **Testcontainers**: Kafka broker in Docker
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework
- **Awaitility**: Async testing utilities

### Coverage Requirements

- **Line Coverage**: >90%
- **Branch Coverage**: >85%
- **Method Coverage**: >95%

## Deployment

### Maven Central

```xml
<dependency>
    <groupId>com.github.kevinmeng4662</groupId>
    <artifactId>kafka-simple-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### System Requirements

- **Java**: 11 or higher
- **Kafka**: 2.8+ (broker compatibility)
- **Memory**: 128MB minimum heap
- **Network**: TCP connectivity to Kafka brokers

### Production Considerations

#### Publisher

- Use connection pooling for multiple publishers
- Monitor producer metrics (throughput, errors, latency)
- Configure appropriate batch size and linger time
- Set up proper error handling and dead letter queues

#### Subscriber

- Scale horizontally with multiple consumer instances
- Monitor consumer lag and processing time
- Implement proper error handling and retry logic
- Use separate thread pools for heavy processing

## Security

### Authentication

Supports standard Kafka authentication mechanisms:

- **SASL/PLAIN**: Username/password authentication
- **SASL/SCRAM**: Salted Challenge Response Authentication
- **SSL**: Certificate-based authentication

### Authorization

- **Topic-level**: Read/write permissions per topic
- **Consumer group**: Group-level access control
- **IP-based**: Network-level restrictions

### Encryption

- **In-transit**: SSL/TLS encryption
- **At-rest**: Broker-level encryption (external)

## Monitoring

### Metrics

#### Publisher Metrics

- `kafka.producer.record-send-rate`: Messages sent per second
- `kafka.producer.record-error-rate`: Error rate
- `kafka.producer.request-latency-avg`: Average request latency
- `kafka.producer.batch-size-avg`: Average batch size

#### Subscriber Metrics

- `kafka.consumer.records-consumed-rate`: Messages consumed per second
- `kafka.consumer.records-lag-max`: Maximum consumer lag
- `kafka.consumer.fetch-latency-avg`: Average fetch latency
- `kafka.consumer.commit-latency-avg`: Average commit latency

### Logging

#### Log Levels

- **ERROR**: Critical failures requiring immediate attention
- **WARN**: Recoverable errors and warnings
- **INFO**: Important lifecycle events
- **DEBUG**: Detailed processing information

#### Log Format

```
2025-08-13 17:58:00.123 [kafka-publisher-1] INFO  KafkaEventPublisher - Published event user.created to topic users at offset 12345
```

## Roadmap

### Version 1.1.0

- [ ] Schema Registry integration
- [ ] Avro serialization support
- [ ] Metrics endpoint (Prometheus)
- [ ] Configuration externalization

### Version 1.2.0

- [ ] Batch publishing API
- [ ] Dead letter queue support
- [ ] Circuit breaker pattern
- [ ] Health check endpoints

### Version 2.0.0

- [ ] Reactive Streams support
- [ ] Kafka Streams integration
- [ ] Multi-cluster support
- [ ] Advanced routing capabilities

## Contributing

### Development Setup

1. Clone repository
2. Install Java 11+
3. Install Docker (for tests)
4. Run `./gradlew build`

### Code Standards

- **Style**: Google Java Style Guide
- **Coverage**: Minimum 90% line coverage
- **Documentation**: Javadoc for public APIs
- **Testing**: Unit and integration tests required

### Pull Request Process

1. Fork repository
2. Create feature branch
3. Implement changes with tests
4. Update documentation
5. Submit pull request
6. Address review feedback
7. Merge after approval

## Support

### Community

- **GitHub Issues**: Bug reports and feature requests
- **Discussions**: General questions and usage help
- **Wiki**: Additional documentation and examples

### Commercial Support

For enterprise support, training, and consulting:

- Email: support@kafka-simple-sdk.com
- SLA: 24-hour response time
- Coverage: Business hours (9 AM - 5 PM UTC)

## License

MIT License - see [LICENSE](LICENSE) file for details.