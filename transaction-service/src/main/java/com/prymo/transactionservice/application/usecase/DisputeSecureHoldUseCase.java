package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisputeSecureHoldUseCase {

    private final TransactionRepository transactionRepository;
    private final KafkaEventProducer kafkaProducer;

    public DisputeSecureHoldUseCase(TransactionRepository transactionRepository, KafkaEventProducer kafkaProducer) {
        this.transactionRepository = transactionRepository;
        this.kafkaProducer = kafkaProducer;
    }

    public Transaction execute(Long id, String currentUser, String reason) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!tx.getSenderUsername().equals(currentUser) && !tx.getRecipientUsername().equals(currentUser)) {
            throw new SecurityException("You are not a participant in this transaction");
        }

        // Apply domain state transition rules
        tx.dispute(reason);

        // Persist state changes
        Transaction savedTx = transactionRepository.save(tx);

        // Dispatch event (Format: DISPUTED:txId:user:reason)
        kafkaProducer.sendEvent("transaction-events", tx.getReference(), "DISPUTED:" + tx.getId() + ":" + currentUser + ":" + reason);

        return savedTx;
    }
}
