package com.prymo.disputeservice.domain.repository;

import com.prymo.disputeservice.domain.model.Dispute;

import java.util.List;
import java.util.Optional;

public interface DisputeRepository {
    Optional<Dispute> findById(Long id);
    Optional<Dispute> findByTransactionId(Long transactionId);
    List<Dispute> findByFilerUsername(String username);
    Dispute save(Dispute dispute);
}
