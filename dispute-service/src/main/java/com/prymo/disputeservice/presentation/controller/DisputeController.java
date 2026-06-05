package com.prymo.disputeservice.presentation.controller;

import com.prymo.disputeservice.application.usecase.AddDisputeMessageUseCase;
import com.prymo.disputeservice.application.usecase.FileDisputeUseCase;
import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;
    private final FileDisputeUseCase fileDisputeUseCase;
    private final AddDisputeMessageUseCase addDisputeMessageUseCase;

    public DisputeController(DisputeRepository disputeRepository, 
                             DisputeMessageRepository messageRepository, 
                             FileDisputeUseCase fileDisputeUseCase, 
                             AddDisputeMessageUseCase addDisputeMessageUseCase) {
        this.disputeRepository = disputeRepository;
        this.messageRepository = messageRepository;
        this.fileDisputeUseCase = fileDisputeUseCase;
        this.addDisputeMessageUseCase = addDisputeMessageUseCase;
    }

    @GetMapping("/my-disputes")
    public ResponseEntity<?> getMyDisputes() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Dispute> disputes = disputeRepository.findByFilerUsername(username);
        return ResponseEntity.ok(disputes);
    }

    @PostMapping
    public ResponseEntity<?> fileDispute(@RequestBody FileDisputeRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (request.getTransactionId() == null || request.getTransactionReference() == null || request.getReason() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "transactionId, transactionReference, and reason are required"));
        }

        try {
            Dispute dispute = fileDisputeUseCase.execute(
                    request.getTransactionId(), 
                    request.getTransactionReference(), 
                    username, 
                    request.getReason()
            );
            return ResponseEntity.ok(dispute);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDispute(@PathVariable Long id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute ticket not found"));
        return ResponseEntity.ok(dispute);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getDisputeMessages(@PathVariable Long id) {
        // Ensure dispute ticket exists
        disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute ticket not found"));

        List<DisputeMessage> messages = messageRepository.findByDisputeIdOrderBySentAtAsc(id);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendDisputeMessage(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String messageText = body.get("message");

        if (messageText == null || messageText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        try {
            DisputeMessage message = addDisputeMessageUseCase.execute(id, username, messageText);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DTO class
    @lombok.Data
    public static class FileDisputeRequest {
        private Long transactionId;
        private String transactionReference;
        private String reason;
    }
}
