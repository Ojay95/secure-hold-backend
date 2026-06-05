package com.prymo.disputeservice.application.usecase;

import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class FileDisputeUseCase {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;

    public FileDisputeUseCase(DisputeRepository disputeRepository, DisputeMessageRepository messageRepository) {
        this.disputeRepository = disputeRepository;
        this.messageRepository = messageRepository;
    }

    public Dispute execute(Long transactionId, String transactionReference, String filerUsername, String reason) {
        if (disputeRepository.findByTransactionId(transactionId).isPresent()) {
            throw new IllegalArgumentException("A dispute has already been filed for this transaction");
        }

        Dispute dispute = Dispute.builder()
                .transactionId(transactionId)
                .transactionReference(transactionReference)
                .filerUsername(filerUsername)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .status("OPENED")
                .build();

        Dispute savedDispute = disputeRepository.save(dispute);

        // System notification message
        DisputeMessage systemMsg = DisputeMessage.builder()
                .disputeId(savedDispute.getId())
                .senderUsername("SYSTEM")
                .message("Dispute ticket opened by " + filerUsername + ". Reason: " + reason)
                .sentAt(LocalDateTime.now())
                .build();
        messageRepository.save(systemMsg);

        return savedDispute;
    }
}
