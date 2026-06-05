package com.prymo.transactionservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, Long> {
    Optional<TransactionEntity> findByReference(String reference);
    List<TransactionEntity> findBySenderUsernameOrRecipientUsername(String sender, String recipient);
    List<TransactionEntity> findByStatusAndExpiresAtBefore(String status, LocalDateTime expiryTime);
}
