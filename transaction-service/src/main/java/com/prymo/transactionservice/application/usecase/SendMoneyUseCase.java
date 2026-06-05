package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
public class SendMoneyUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaEventProducer kafkaProducer;
    private final Random random = new Random();

    public SendMoneyUseCase(TransactionRepository transactionRepository, 
                            AccountServiceClient accountServiceClient, 
                            KafkaEventProducer kafkaProducer) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.kafkaProducer = kafkaProducer;
    }

    private String generateReference() {
        return "SH" + System.currentTimeMillis() + String.format("%04d", random.nextInt(10000));
    }

    public Transaction execute(String sender, String recipient, BigDecimal amount, String note) {
        if (sender.equals(recipient)) {
            throw new IllegalArgumentException("Cannot send money to yourself");
        }

        String reference = generateReference();
        
        // 1. Deduct sender
        accountServiceClient.deduct(sender, amount, false);

        // 2. Credit recipient
        accountServiceClient.credit(recipient, sender, amount, false);

        // 3. Create transaction record
        Transaction tx = Transaction.builder()
                .senderUsername(sender)
                .recipientUsername(recipient)
                .amount(amount)
                .type("TRANSFER")
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .note(note)
                .reference(reference)
                .build();

        Transaction savedTx = transactionRepository.save(tx);

        // 4. Send notifications
        kafkaProducer.sendEvent("transaction-events", reference, "COMPLETED:" + sender + ":" + recipient + ":" + amount);

        return savedTx;
    }
}
