package com.prymo.disputeservice.infrastructure.persistence;

import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DisputeRepositoryAdapter implements DisputeRepository {

    private final JpaDisputeRepository jpaRepository;

    public DisputeRepositoryAdapter(JpaDisputeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Dispute> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Dispute> findByTransactionId(Long transactionId) {
        return jpaRepository.findByTransactionId(transactionId).map(this::toDomain);
    }

    @Override
    public List<Dispute> findByFilerUsername(String username) {
        return jpaRepository.findByFilerUsername(username).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Dispute save(Dispute dispute) {
        DisputeEntity entity = toEntity(dispute);
        DisputeEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private Dispute toDomain(DisputeEntity entity) {
        return Dispute.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .transactionReference(entity.getTransactionReference())
                .filerUsername(entity.getFilerUsername())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private DisputeEntity toEntity(Dispute domain) {
        return DisputeEntity.builder()
                .id(domain.getId())
                .transactionId(domain.getTransactionId())
                .transactionReference(domain.getTransactionReference())
                .filerUsername(domain.getFilerUsername())
                .reason(domain.getReason())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
