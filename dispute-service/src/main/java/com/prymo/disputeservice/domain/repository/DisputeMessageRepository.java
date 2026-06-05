package com.prymo.disputeservice.domain.repository;

import com.prymo.disputeservice.domain.model.DisputeMessage;

import java.util.List;

public interface DisputeMessageRepository {
    List<DisputeMessage> findByDisputeIdOrderBySentAtAsc(Long disputeId);
    DisputeMessage save(DisputeMessage message);
}
