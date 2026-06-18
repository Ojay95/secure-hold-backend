package com.prymo.disputeservice.application.usecase;

import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import com.prymo.disputeservice.infrastructure.client.TransactionServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ResolveDisputeUseCase {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;
    private final TransactionServiceClient transactionServiceClient;

    public ResolveDisputeUseCase(DisputeRepository disputeRepository, 
                                 DisputeMessageRepository messageRepository,
                                 TransactionServiceClient transactionServiceClient) {
        this.disputeRepository = disputeRepository;
        this.messageRepository = messageRepository;
        this.transactionServiceClient = transactionServiceClient;
    }

    public Dispute execute(Long disputeId, String resolution) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute ticket not found"));

        if (!"OPENED".equals(dispute.getStatus()) && !"UNDER_REVIEW".equals(dispute.getStatus())) {
            throw new IllegalStateException("Dispute ticket is already resolved or closed");
        }

        if ("RELEASE".equalsIgnoreCase(resolution)) {
            dispute.setStatus("RESOLVED_SELLER");
        } else if ("REFUND".equalsIgnoreCase(resolution)) {
            dispute.setStatus("RESOLVED_BUYER");
        } else {
            throw new IllegalArgumentException("Invalid resolution: must be RELEASE or REFUND");
        }

        // Call transaction-service to execute the release/refund
        transactionServiceClient.resolveDispute(dispute.getTransactionId(), resolution);

        Dispute savedDispute = disputeRepository.save(dispute);

        // System notification message
        DisputeMessage systemMsg = DisputeMessage.builder()
                .disputeId(savedDispute.getId())
                .senderUsername("SYSTEM")
                .message("Dispute ticket resolved by Admin. Resolution outcome: " + dispute.getStatus())
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(systemMsg);

        return savedDispute;
    }
}
