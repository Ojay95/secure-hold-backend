package com.prymo.transactionservice.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
            log.info("Sent event to Kafka topic [{}]: key={}, payload={}", topic, key, payload);
        } catch (Exception e) {
            log.warn("Kafka broker is not available. Skipping real-time event propagation. Event payload: {}", payload);
        }
    }
}
