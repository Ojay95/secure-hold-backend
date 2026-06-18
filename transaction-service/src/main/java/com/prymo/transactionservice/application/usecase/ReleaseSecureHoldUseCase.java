package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.client.PaystackPayoutClient;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReleaseSecureHoldUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaEventProducer kafkaProducer;
    private final PaystackPayoutClient paystackPayoutClient;

    public ReleaseSecureHoldUseCase(TransactionRepository transactionRepository, 
                                    AccountServiceClient accountServiceClient, 
                                    KafkaEventProducer kafkaProducer,
                                    PaystackPayoutClient paystackPayoutClient) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.kafkaProducer = kafkaProducer;
        this.paystackPayoutClient = paystackPayoutClient;
    }

    public Transaction execute(Long id, String currentUser) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!tx.getSenderUsername().equals(currentUser)) {
            throw new SecurityException("Only the sender can release the held funds");
        }

        // Apply domain state transition rules
        tx.release();

        // 1. Trigger Paystack payout if external target
        if (tx.getRecipientUsername() != null && tx.getRecipientUsername().contains(":")) {
            String[] parts = tx.getRecipientUsername().split(":");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid bank recipient format in transaction");
            }
            String bankCode = parts[0];
            String accountNumber = parts[1];
            
            String payoutRef = paystackPayoutClient.initiatePayout(
                    bankCode, 
                    accountNumber, 
                    "External Recipient", 
                    tx.getAmount(), 
                    tx.getReference(), 
                    tx.getNote()
            );
            if (payoutRef == null) {
                throw new IllegalStateException("Failed to initiate external transfer via Paystack");
            }
        }

        // 2. Credit recipient / release escrow hold balance internally
        accountServiceClient.credit(tx.getRecipientUsername(), tx.getSenderUsername(), tx.getAmount(), true);

        // 3. Persist state changes
        Transaction savedTx = transactionRepository.save(tx);

        // 4. Dispatch event
        kafkaProducer.sendEvent("transaction-events", tx.getReference(), "RELEASED:" + tx.getSenderUsername() + ":" + tx.getRecipientUsername() + ":" + tx.getAmount());

        return savedTx;
    }
}
