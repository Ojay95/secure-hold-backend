package com.prymo.disputeservice.application.usecase;

import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AddDisputeMessageUseCase {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;

    public AddDisputeMessageUseCase(DisputeRepository disputeRepository, DisputeMessageRepository messageRepository) {
        this.disputeRepository = disputeRepository;
        this.messageRepository = messageRepository;
    }

    public DisputeMessage execute(Long disputeId, String sender, String text) {
        // Ensure dispute ticket exists
        disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute ticket not found"));

        DisputeMessage message = DisputeMessage.builder()
                .disputeId(disputeId)
                .senderUsername(sender)
                .message(text)
                .sentAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }
}
