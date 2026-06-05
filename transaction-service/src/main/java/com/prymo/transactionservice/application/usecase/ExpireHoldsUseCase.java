package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ExpireHoldsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireHoldsUseCase.class);
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaEventProducer kafkaProducer;

    public ExpireHoldsUseCase(TransactionRepository transactionRepository, 
                              AccountServiceClient accountServiceClient, 
                              KafkaEventProducer kafkaProducer) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.kafkaProducer = kafkaProducer;
    }

    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        List<Transaction> expired = transactionRepository.findByStatusAndExpiresAtBefore("ACTIVE", now);

        if (!expired.isEmpty()) {
            log.info("Found {} expired SecureHold transactions. Initiating refunds...", expired.size());
            for (Transaction tx : expired) {
                try {
                    // Apply domain state transition rules
                    tx.expire();

                    // 1. Refund sender locked secureHold balance back to available balance
                    accountServiceClient.refund(tx.getSenderUsername(), tx.getAmount());

                    // 2. Persist state changes
                    transactionRepository.save(tx);

                    log.info("Transaction reference {} has expired. Funds refunded to sender {}.", tx.getReference(), tx.getSenderUsername());

                    // 3. Dispatch event
                    kafkaProducer.sendEvent("transaction-events", tx.getReference(), "EXPIRED:" + tx.getSenderUsername() + ":" + tx.getAmount());
                } catch (Exception e) {
                    log.error("Failed to process expiration refund for transaction {}: {}", tx.getReference(), e.getMessage());
                }
            }
        }
    }
}
