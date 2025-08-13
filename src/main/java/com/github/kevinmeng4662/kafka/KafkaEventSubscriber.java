package com.github.kevinmeng4662.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Simplified Kafka event subscriber with easy-to-use API
 */
public class KafkaEventSubscriber implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KafkaEventSubscriber.class);
    
    private final KafkaConsumer<String, String> consumer;
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Consumer<KafkaEvent> eventHandler;
    private Consumer<Exception> errorHandler;

    public KafkaEventSubscriber(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        this.consumer = new KafkaConsumer<>(props);
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "kafka-event-subscriber");
            t.setDaemon(true);
            return t;
        });
        
        logger.info("KafkaEventSubscriber initialized with bootstrap servers: {} and group ID: {}", 
                bootstrapServers, groupId);
    }

    /**
     * Subscribe to topics and start consuming events
     */
    public KafkaEventSubscriber subscribe(String... topics) {
        consumer.subscribe(Arrays.asList(topics));
        logger.info("Subscribed to topics: {}", Arrays.toString(topics));
        return this;
    }

    /**
     * Set the event handler function
     */
    public KafkaEventSubscriber onEvent(Consumer<KafkaEvent> handler) {
        this.eventHandler = handler;
        return this;
    }

    /**
     * Set the error handler function
     */
    public KafkaEventSubscriber onError(Consumer<Exception> handler) {
        this.errorHandler = handler;
        return this;
    }

    /**
     * Start consuming events asynchronously
     */
    public void start() {
        if (eventHandler == null) {
            throw new IllegalStateException("Event handler must be set before starting");
        }
        
        if (running.compareAndSet(false, true)) {
            executorService.submit(this::consumeEvents);
            logger.info("KafkaEventSubscriber started");
        } else {
            logger.warn("KafkaEventSubscriber is already running");
        }
    }

    /**
     * Stop consuming events
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            consumer.wakeup();
            logger.info("KafkaEventSubscriber stopped");
        }
    }

    /**
     * Check if the subscriber is running
     */
    public boolean isRunning() {
        return running.get();
    }

    private void consumeEvents() {
        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        KafkaEvent event = KafkaEvent.fromJson(record.value());
                        
                        // Add Kafka record metadata to event headers
                        event.addHeader("kafka.topic", record.topic())
                             .addHeader("kafka.partition", record.partition())
                             .addHeader("kafka.offset", record.offset())
                             .addHeader("kafka.timestamp", record.timestamp());
                        
                        // Add Kafka headers to event headers
                        record.headers().forEach(header -> 
                            event.addHeader("kafka.header." + header.key(), new String(header.value()))
                        );
                        
                        eventHandler.accept(event);
                        
                        logger.debug("Processed event {} from topic {} partition {} offset {}", 
                                event.getId(), record.topic(), record.partition(), record.offset());
                        
                    } catch (Exception e) {
                        logger.error("Error processing record from topic {} partition {} offset {}", 
                                record.topic(), record.partition(), record.offset(), e);
                        
                        if (errorHandler != null) {
                            try {
                                errorHandler.accept(e);
                            } catch (Exception handlerException) {
                                logger.error("Error in error handler", handlerException);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                logger.error("Unexpected error in event consumption loop", e);
                if (errorHandler != null) {
                    try {
                        errorHandler.accept(e);
                    } catch (Exception handlerException) {
                        logger.error("Error in error handler", handlerException);
                    }
                }
            }
        } finally {
            running.set(false);
        }
    }

    @Override
    public void close() {
        stop();
        try {
            executorService.shutdown();
            consumer.close();
            logger.info("KafkaEventSubscriber closed successfully");
        } catch (Exception e) {
            logger.error("Error closing KafkaEventSubscriber", e);
        }
    }
}