package com.prymo.disputeservice.presentation.controller;

import com.prymo.disputeservice.application.usecase.AddDisputeMessageUseCase;
import com.prymo.disputeservice.application.usecase.FileDisputeUseCase;
import com.prymo.disputeservice.application.usecase.ResolveDisputeUseCase;
import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <h2>DisputeController</h2>
 * <p>Handles escrow dispute management, including filing disputes, sending message logs between parties, and administrative resolutions.</p>
 * <p><strong>Developer Guide:</strong></p>
 * <ul>
 *   <li>When a dispute is resolved administratively (either refunded or released), it triggers a REST request to `transaction-service` to process the respective ledger transaction update.</li>
 *   <li>The communication channel stores messages linked to dispute tickets for documentation and reference during review.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/disputes")
@Tag(name = "Dispute Management", description = "Endpoints for creating and managing escrow disputes, communication threads, and resolutions")
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;
    private final FileDisputeUseCase fileDisputeUseCase;
    private final AddDisputeMessageUseCase addDisputeMessageUseCase;
    private final ResolveDisputeUseCase resolveDisputeUseCase;

    public DisputeController(DisputeRepository disputeRepository, 
                             DisputeMessageRepository messageRepository, 
                             FileDisputeUseCase fileDisputeUseCase, 
                             AddDisputeMessageUseCase addDisputeMessageUseCase,
                             ResolveDisputeUseCase resolveDisputeUseCase) {
        this.disputeRepository = disputeRepository;
        this.messageRepository = messageRepository;
        this.fileDisputeUseCase = fileDisputeUseCase;
        this.addDisputeMessageUseCase = addDisputeMessageUseCase;
        this.resolveDisputeUseCase = resolveDisputeUseCase;
    }

    /**
     * Retrieves all disputes filed by the currently authenticated user.
     */
    @GetMapping("/my-disputes")
    @Operation(
        summary = "Get User Disputes",
        description = "Fetches the history of dispute tickets filed by the currently logged-in user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of disputes retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
        }
    )
    public ResponseEntity<?> getMyDisputes() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Dispute> disputes = disputeRepository.findByFilerUsername(username);
        return ResponseEntity.ok(disputes);
    }

    /**
     * Files a new dispute ticket for a given transaction.
     */
    @PostMapping
    @Operation(
        summary = "File a Dispute",
        description = "Creates a new dispute ticket for a secure hold transaction, changing its status to DISPUTED.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dispute filed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or transaction reference")
        }
    )
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

    /**
     * Retrieves the details of a specific dispute ticket.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get Dispute Details",
        description = "Fetches the details and current status of a specific dispute ticket by ID.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dispute details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Dispute ticket not found")
        }
    )
    public ResponseEntity<?> getDispute(@PathVariable Long id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute ticket not found"));
        return ResponseEntity.ok(dispute);
    }

    /**
     * Retrieves the list of messages exchanged on a specific dispute thread.
     */
    @GetMapping("/{id}/messages")
    @Operation(
        summary = "Get Dispute Messages",
        description = "Fetches the historical message thread associated with a dispute ticket.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of messages retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Dispute ticket not found")
        }
    )
    public ResponseEntity<?> getDisputeMessages(@PathVariable Long id) {
        // Ensure dispute ticket exists
        disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute ticket not found"));

        List<DisputeMessage> messages = messageRepository.findByDisputeIdOrderBySentAtAsc(id);
        return ResponseEntity.ok(messages);
    }

    /**
     * Sends a new message in the dispute ticket conversation thread.
     */
    @PostMapping("/{id}/messages")
    @Operation(
        summary = "Send Dispute Message",
        description = "Posts a message to the dispute conversation thread for communication between participants.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Message sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid message text or dispute ticket status")
        }
    )
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

    /**
     * Resolves a dispute administratively, routing the held escrow funds accordingly.
     */
    @PostMapping("/{id}/resolve")
    @Operation(
        summary = "Resolve Dispute",
        description = "Administratively resolves the dispute, triggering the release or refund of held funds.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dispute resolved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid resolution option or dispute state")
        }
    )
    public ResponseEntity<?> resolveDispute(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String resolution = body.get("resolution");
        if (resolution == null || resolution.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Resolution ('RELEASE' or 'REFUND') is required"));
        }
        try {
            Dispute dispute = resolveDisputeUseCase.execute(id, resolution);
            return ResponseEntity.ok(dispute);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Resolution failed: " + e.getMessage()));
        }
    }

    // DTO class
    @lombok.Data
    @Schema(description = "Payload representing a request to file a dispute")
    public static class FileDisputeRequest {
        @Schema(description = "Unique ID of the transaction to dispute", example = "42")
        private Long transactionId;
        @Schema(description = "Reference string of the transaction", example = "TXN-123456789")
        private String transactionReference;
        @Schema(description = "The reason for filing the dispute", example = "Services not rendered as agreed")
        private String reason;
    }
}
