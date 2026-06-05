package com.prymo.notificationservice.infrastructure.messaging;

import com.prymo.notificationservice.application.usecase.SendNotificationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationListener.class);
    private final SendNotificationUseCase useCase;

    public KafkaNotificationListener(SendNotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "transaction-events", groupId = "notification-group")
    public void consumeTransactionEvent(String message) {
        log.info("Kafka consumer received event message: {}", message);
        useCase.processEvent(message);
    }
}
