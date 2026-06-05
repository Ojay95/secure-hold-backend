package com.prymo.transactionservice.domain.repository;

import com.prymo.transactionservice.domain.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Optional<Transaction> findById(Long id);
    Optional<Transaction> findByReference(String reference);
    List<Transaction> findBySenderUsernameOrRecipientUsername(String sender, String recipient);
    List<Transaction> findByStatusAndExpiresAtBefore(String status, LocalDateTime expiryTime);
    Transaction save(Transaction transaction);
}
