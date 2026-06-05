package com.prymo.disputeservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaDisputeMessageRepository extends JpaRepository<DisputeMessageEntity, Long> {
    List<DisputeMessageEntity> findByDisputeIdOrderBySentAtAsc(Long disputeId);
}
