package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResolveDisputedSecureHoldUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveDisputedSecureHoldUseCase.class);
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaEventProducer kafkaProducer;

    public ResolveDisputedSecureHoldUseCase(TransactionRepository transactionRepository, 
                                            AccountServiceClient accountServiceClient, 
                                            KafkaEventProducer kafkaProducer) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.kafkaProducer = kafkaProducer;
    }

    public Transaction execute(Long id, String resolution) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!"DISPUTED".equals(tx.getStatus())) {
            throw new IllegalStateException("Transaction is not in DISPUTED state");
        }

        if ("RELEASE".equalsIgnoreCase(resolution)) {
            tx.release();
            
            // Credit recipient from sender secureHold balance
            accountServiceClient.credit(tx.getRecipientUsername(), tx.getSenderUsername(), tx.getAmount(), true);
            Transaction savedTx = transactionRepository.save(tx);
            
            log.info("Dispute resolved with RELEASE for tx reference {}. Funds credited to recipient {}.", tx.getReference(), tx.getRecipientUsername());
            
            // Dispatch event
            kafkaProducer.sendEvent("transaction-events", tx.getReference(), "RELEASED:" + tx.getSenderUsername() + ":" + tx.getRecipientUsername() + ":" + tx.getAmount());
            return savedTx;
            
        } else if ("REFUND".equalsIgnoreCase(resolution)) {
            tx.refund();
            
            // Refund sender from secureHold balance back to available balance
            accountServiceClient.refund(tx.getSenderUsername(), tx.getAmount());
            Transaction savedTx = transactionRepository.save(tx);
            
            log.info("Dispute resolved with REFUND for tx reference {}. Funds refunded to sender {}.", tx.getReference(), tx.getSenderUsername());
            
            // Dispatch event (Format: REFUNDED:sender:amount)
            kafkaProducer.sendEvent("transaction-events", tx.getReference(), "REFUNDED:" + tx.getSenderUsername() + ":" + tx.getAmount());
            return savedTx;
            
        } else {
            throw new IllegalArgumentException("Invalid resolution: must be RELEASE or REFUND");
        }
    }
}
