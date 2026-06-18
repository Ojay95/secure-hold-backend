package com.prymo.disputeservice.infrastructure.messaging;

import com.prymo.disputeservice.application.usecase.FileDisputeUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class KafkaDisputeListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaDisputeListener.class);
    private final FileDisputeUseCase fileDisputeUseCase;

    public KafkaDisputeListener(FileDisputeUseCase fileDisputeUseCase) {
        this.fileDisputeUseCase = fileDisputeUseCase;
    }

    @KafkaListener(topics = "transaction-events", groupId = "dispute-group")
    public void consumeTransactionEvent(String message, @Header(KafkaHeaders.RECEIVED_KEY) String reference) {
        try {
            log.info("Kafka consumer received event message: {} with key: {}", message, reference);
            String[] parts = message.split(":");
            if (parts.length >= 4 && "DISPUTED".equals(parts[0])) {
                Long txId = Long.parseLong(parts[1]);
                String user = parts[2];
                StringBuilder reasonBuilder = new StringBuilder();
                for (int i = 3; i < parts.length; i++) {
                    if (i > 3) reasonBuilder.append(":");
                    reasonBuilder.append(parts[i]);
                }
                String reason = reasonBuilder.toString();

                log.info("Auto-creating dispute ticket for transaction ID: {}, Reference: {}, User: {}", txId, reference, user);
                fileDisputeUseCase.execute(txId, reference, user, reason);
            }
        } catch (Exception e) {
            log.warn("Dispute auto-creation skipped or failed: {}", e.getMessage());
        }
    }
}
