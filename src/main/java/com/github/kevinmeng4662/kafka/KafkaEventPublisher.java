package com.github.kevinmeng4662.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Simplified Kafka event publisher with easy-to-use API
 */
public class KafkaEventPublisher implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    private final KafkaProducer<String, String> producer;
    private final String defaultTopic;

    public KafkaEventPublisher(String bootstrapServers) {
        this(bootstrapServers, null);
    }

    public KafkaEventPublisher(String bootstrapServers, String defaultTopic) {
        this.defaultTopic = defaultTopic;
        
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        this.producer = new KafkaProducer<>(props);
        logger.info("KafkaEventPublisher initialized with bootstrap servers: {}", bootstrapServers);
    }

    /**
     * Publish an event to the default topic
     */
    public CompletableFuture<RecordMetadata> publish(KafkaEvent event) {
        if (defaultTopic == null) {
            throw new IllegalStateException("No default topic configured. Use publish(topic, event) instead.");
        }
        return publish(defaultTopic, event);
    }

    /**
     * Publish an event to a specific topic
     */
    public CompletableFuture<RecordMetadata> publish(String topic, KafkaEvent event) {
        return publish(topic, event.getId(), event);
    }

    /**
     * Publish an event to a specific topic with a custom key
     */
    public CompletableFuture<RecordMetadata> publish(String topic, String key, KafkaEvent event) {
        try {
            String eventJson = event.toJson();
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, eventJson);
            
            // Add event headers to Kafka record headers
            if (event.getHeaders() != null) {
                event.getHeaders().forEach((headerKey, headerValue) -> {
                    if (headerValue != null) {
                        record.headers().add(headerKey, headerValue.toString().getBytes());
                    }
                });
            }
            
            CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to publish event {} to topic {}", event.getId(), topic, exception);
                    future.completeExceptionally(exception);
                } else {
                    logger.debug("Successfully published event {} to topic {} at offset {}", 
                            event.getId(), topic, metadata.offset());
                    future.complete(metadata);
                }
            });
            
            return future;
        } catch (Exception e) {
            logger.error("Error publishing event {} to topic {}", event.getId(), topic, e);
            CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Publish a simple event with just type and payload
     */
    public CompletableFuture<RecordMetadata> publish(String topic, String eventType, Object payload) {
        KafkaEvent event = new KafkaEvent(eventType, payload);
        return publish(topic, event);
    }

    /**
     * Flush all pending messages
     */
    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        try {
            producer.close();
            logger.info("KafkaEventPublisher closed successfully");
        } catch (Exception e) {
            logger.error("Error closing KafkaEventPublisher", e);
        }
    }
}