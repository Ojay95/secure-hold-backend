package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReleaseSecureHoldUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaEventProducer kafkaProducer;

    public ReleaseSecureHoldUseCase(TransactionRepository transactionRepository, 
                                    AccountServiceClient accountServiceClient, 
                                    KafkaEventProducer kafkaProducer) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.kafkaProducer = kafkaProducer;
    }

    public Transaction execute(Long id, String currentUser) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!tx.getSenderUsername().equals(currentUser)) {
            throw new SecurityException("Only the sender can release the held funds");
        }

        // Apply domain state transition rules
        tx.release();

        // 1. Credit recipient from sender secureHold balance
        accountServiceClient.credit(tx.getRecipientUsername(), tx.getSenderUsername(), tx.getAmount(), true);

        // 2. Persist state changes
        Transaction savedTx = transactionRepository.save(tx);

        // 3. Dispatch event
        kafkaProducer.sendEvent("transaction-events", tx.getReference(), "RELEASED:" + tx.getSenderUsername() + ":" + tx.getRecipientUsername() + ":" + tx.getAmount());

        return savedTx;
    }
}
