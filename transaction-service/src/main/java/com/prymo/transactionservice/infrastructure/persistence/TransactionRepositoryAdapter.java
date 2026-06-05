package com.prymo.transactionservice.infrastructure.persistence;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaRepository;

    public TransactionRepositoryAdapter(JpaTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Transaction> findByReference(String reference) {
        return jpaRepository.findByReference(reference).map(this::toDomain);
    }

    @Override
    public List<Transaction> findBySenderUsernameOrRecipientUsername(String sender, String recipient) {
        return jpaRepository.findBySenderUsernameOrRecipientUsername(sender, recipient).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByStatusAndExpiresAtBefore(String status, LocalDateTime expiryTime) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, expiryTime).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = toEntity(transaction);
        TransactionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private Transaction toDomain(TransactionEntity entity) {
        return Transaction.builder()
                .id(entity.getId())
                .senderUsername(entity.getSenderUsername())
                .recipientUsername(entity.getRecipientUsername())
                .amount(entity.getAmount())
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .holdDuration(entity.getHoldDuration())
                .note(entity.getNote())
                .reference(entity.getReference())
                .disputeReason(entity.getDisputeReason())
                .build();
    }

    private TransactionEntity toEntity(Transaction domain) {
        return TransactionEntity.builder()
                .id(domain.getId())
                .senderUsername(domain.getSenderUsername())
                .recipientUsername(domain.getRecipientUsername())
                .amount(domain.getAmount())
                .type(domain.getType())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .expiresAt(domain.getExpiresAt())
                .holdDuration(domain.getHoldDuration())
                .note(domain.getNote())
                .reference(domain.getReference())
                .disputeReason(domain.getDisputeReason())
                .build();
    }
}
