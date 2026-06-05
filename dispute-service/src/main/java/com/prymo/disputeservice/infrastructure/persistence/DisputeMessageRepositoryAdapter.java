package com.prymo.disputeservice.infrastructure.persistence;

import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DisputeMessageRepositoryAdapter implements DisputeMessageRepository {

    private final JpaDisputeMessageRepository jpaRepository;

    public DisputeMessageRepositoryAdapter(JpaDisputeMessageRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<DisputeMessage> findByDisputeIdOrderBySentAtAsc(Long disputeId) {
        return jpaRepository.findByDisputeIdOrderBySentAtAsc(disputeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public DisputeMessage save(DisputeMessage message) {
        DisputeMessageEntity entity = toEntity(message);
        DisputeMessageEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private DisputeMessage toDomain(DisputeMessageEntity entity) {
        return DisputeMessage.builder()
                .id(entity.getId())
                .disputeId(entity.getDisputeId())
                .senderUsername(entity.getSenderUsername())
                .message(entity.getMessage())
                .sentAt(entity.getSentAt())
                .build();
    }

    private DisputeMessageEntity toEntity(DisputeMessage domain) {
        return DisputeMessageEntity.builder()
                .id(domain.getId())
                .disputeId(domain.getDisputeId())
                .senderUsername(domain.getSenderUsername())
                .message(domain.getMessage())
                .sentAt(domain.getSentAt())
                .build();
    }
}
