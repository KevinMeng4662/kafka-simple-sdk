# Changelog

All notable changes to the Kafka Simple SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-08-13

### Added

#### Core Features
- **KafkaEvent**: Structured event model with automatic ID and timestamp generation
- **KafkaEventPublisher**: Simplified Kafka producer with async API
- **KafkaEventSubscriber**: Simplified Kafka consumer with callback-based processing
- **JSON Serialization**: Automatic event serialization/deserialization
- **Header Support**: Custom headers with automatic Kafka metadata enrichment
- **Error Handling**: Comprehensive error handling with custom error callbacks
- **Resource Management**: AutoCloseable implementations for proper cleanup

#### Configuration
- **Sensible Defaults**: Production-ready default configurations
- **Idempotent Producer**: Enabled by default for exactly-once semantics
- **Compression**: Snappy compression enabled by default
- **Retry Logic**: Automatic retries with exponential backoff
- **Consumer Groups**: Automatic consumer group management
- **Offset Management**: Automatic offset commits with configurable intervals

#### Testing
- **Unit Tests**: Comprehensive unit test suite with >90% coverage
- **Integration Tests**: End-to-end testing with Testcontainers
- **Kafka Compatibility**: Tested with Kafka 2.8+ and 3.x
- **Mock Support**: Mockito-based testing utilities
- **Async Testing**: Awaitility for testing asynchronous operations

#### Documentation
- **README**: Comprehensive usage guide with examples
- **API Specification**: Detailed API documentation
- **Technical Specification**: Architecture and design documentation
- **Examples**: Working publisher and subscriber examples
- **Javadoc**: Complete API documentation

#### Build & Deployment
- **Gradle Build**: Modern Gradle build system with Kotlin DSL
- **Maven Compatible**: Published to Maven Central
- **Java 11+**: Compatible with Java 11 and higher
- **Dependencies**: Minimal external dependencies
- **Logging**: SLF4J with Logback for structured logging

### Technical Details

#### Publisher Features
- Asynchronous publishing with CompletableFuture
- Support for default topics and custom topic routing
- Custom key support for partitioning
- Automatic header propagation from events to Kafka records
- Flush and close operations for graceful shutdown
- Built-in error handling with detailed error information

#### Subscriber Features
- Multi-topic subscription support
- Event-driven processing with lambda support
- Automatic Kafka metadata enrichment
- Graceful start/stop lifecycle management
- Background thread management with proper cleanup
- Error isolation - failed events don't block processing

#### Event Model
- UUID-based event identification
- ISO 8601 timestamp formatting
- Flexible payload support (any serializable object)
- Fluent API for header management
- JSON serialization with Jackson
- Immutable event properties after creation

#### Performance Optimizations
- Connection pooling and reuse
- Batch processing where applicable
- Efficient JSON serialization
- Minimal memory allocation
- Optimized for high-throughput scenarios

### Dependencies

- **Apache Kafka Clients**: 3.5.1
- **Jackson Databind**: 2.15.2
- **SLF4J API**: 2.0.7
- **Logback Classic**: 1.4.8
- **JUnit Jupiter**: 5.9.3 (test)
- **Mockito**: 5.4.0 (test)
- **Testcontainers**: 1.18.3 (test)
- **Awaitility**: 4.2.0 (test)

### Breaking Changes

N/A - Initial release

### Migration Guide

N/A - Initial release

### Known Issues

- None at this time

### Security

- All dependencies are up-to-date with latest security patches
- No known security vulnerabilities
- Supports standard Kafka security features (SSL, SASL)

---

## [Unreleased]

### Planned Features

#### Version 1.1.0
- Schema Registry integration
- Avro serialization support
- Prometheus metrics endpoint
- Configuration externalization
- Dead letter queue support

#### Version 1.2.0
- Batch publishing API
- Circuit breaker pattern
- Health check endpoints
- Advanced error handling strategies
- Performance monitoring dashboard

#### Version 2.0.0
- Reactive Streams support
- Kafka Streams integration
- Multi-cluster support
- Advanced routing capabilities
- Cloud-native features

### Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute to this project.

### Support

For questions, issues, or feature requests:

1. Check existing [GitHub Issues](https://github.com/KevinMeng4662/kafka-simple-sdk/issues)
2. Create a new issue with detailed information
3. Join our [Discussions](https://github.com/KevinMeng4662/kafka-simple-sdk/discussions) for community support

### License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.