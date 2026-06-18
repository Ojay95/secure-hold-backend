package com.prymo.transactionservice.presentation.controller;

import com.prymo.transactionservice.application.usecase.*;
import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h2>TransactionController</h2>
 * <p>Handles standard peer-to-peer transfers and lifecycle operations for SecureHold escrow deposits.</p>
 * <p><strong>Developer Guide:</strong></p>
 * <ul>
 *   <li>Exposes standard transfer endpoints and secure hold creation endpoints.</li>
 *   <li>The secure hold flow locks funds from the sender's balance in `account-service` and puts it in escrow.</li>
 *   <li>Provides release and dispute mappings, as well as admin dispute resolution.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Transaction & Escrow Management", description = "Endpoints for standard transfers, creating secure holds, releasing, and disputing held funds")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final SendMoneyUseCase sendMoneyUseCase;
    private final CreateSecureHoldUseCase createSecureHoldUseCase;
    private final ReleaseSecureHoldUseCase releaseSecureHoldUseCase;
    private final DisputeSecureHoldUseCase disputeSecureHoldUseCase;
    private final ResolveDisputedSecureHoldUseCase resolveDisputedSecureHoldUseCase;

    public TransactionController(TransactionRepository transactionRepository, 
                                 SendMoneyUseCase sendMoneyUseCase, 
                                 CreateSecureHoldUseCase createSecureHoldUseCase, 
                                 ReleaseSecureHoldUseCase releaseSecureHoldUseCase, 
                                 DisputeSecureHoldUseCase disputeSecureHoldUseCase,
                                 ResolveDisputedSecureHoldUseCase resolveDisputedSecureHoldUseCase) {
        this.transactionRepository = transactionRepository;
        this.sendMoneyUseCase = sendMoneyUseCase;
        this.createSecureHoldUseCase = createSecureHoldUseCase;
        this.releaseSecureHoldUseCase = releaseSecureHoldUseCase;
        this.disputeSecureHoldUseCase = disputeSecureHoldUseCase;
        this.resolveDisputedSecureHoldUseCase = resolveDisputedSecureHoldUseCase;
    }

    /**
     * Fetches all transactions where the user is either the sender or the recipient.
     */
    @GetMapping({"/transactions/my-transactions", "/securehold/my-transactions"})
    @Operation(
        summary = "Get User Transactions",
        description = "Retrieves the complete transaction history for the currently logged-in user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
        }
    )
    public ResponseEntity<?> getMyTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Transaction> txList = transactionRepository.findBySenderUsernameOrRecipientUsername(username, username);
        return ResponseEntity.ok(txList);
    }

    /**
     * Executes an instant standard transfer to another user.
     */
    @PostMapping({"/transactions/send", "/transfer"})
    @Operation(
        summary = "Standard Money Transfer",
        description = "Performs an instant balance transfer from the sender to the recipient's available ledger balance.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Transfer executed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient funds or invalid recipient")
        }
    )
    public ResponseEntity<?> sendMoney(@RequestBody TransferRequest request) {
        String sender = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Transaction tx = sendMoneyUseCase.execute(
                    sender, 
                    request.getRecipientUsername(), 
                    request.getAmount(), 
                    request.getNote()
            );
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Transaction failed: " + e.getMessage()));
        }
    }

    /**
     * Creates a SecureHold transaction, locking funds in escrow.
     */
    @PostMapping({"/transactions/securehold", "/securehold/create"})
    @Operation(
        summary = "Create SecureHold Escrow",
        description = "Deducts funds from the sender and places them in a secure hold escrow balance, pending release or dispute.",
        responses = {
            @ApiResponse(responseCode = "200", description = "SecureHold created and funds escrowed"),
            @ApiResponse(responseCode = "400", description = "Insufficient funds or validation failure")
        }
    )
    public ResponseEntity<?> createSecureHold(@RequestBody TransferRequest request) {
        String sender = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Transaction tx = createSecureHoldUseCase.execute(
                    sender, 
                    request.getRecipientUsername(), 
                    request.getAmount(), 
                    request.getNote(), 
                    request.getHoldDuration()
            );
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "SecureHold creation failed: " + e.getMessage()));
        }
    }

    /**
     * Fetches details of a specific SecureHold transaction.
     */
    @GetMapping({"/transactions/securehold/{id}", "/securehold/{id}"})
    @Operation(
        summary = "Get SecureHold Details",
        description = "Fetches the full lifecycle details of a specific secure hold transaction.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Transaction details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
        }
    )
    public ResponseEntity<?> getSecureHoldDetails(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Releases securehold funds to the recipient. Can only be initiated by the sender.
     */
    @PostMapping({"/transactions/securehold/{id}/release", "/securehold/{id}/release"})
    @Operation(
        summary = "Release SecureHold Escrow",
        description = "Releases held escrow funds to the recipient's available balance. Must be called by the sender.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Escrow funds released successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transaction state"),
            @ApiResponse(responseCode = "403", description = "Forbidden (only the sender can trigger this)")
        }
    )
    public ResponseEntity<?> releaseSecureHold(@PathVariable Long id) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Transaction tx = releaseSecureHoldUseCase.execute(id, currentUser);
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Release failed: " + e.getMessage()));
        }
    }

    /**
     * Files a dispute on the securehold, locking it until admin resolution.
     */
    @PostMapping({"/transactions/securehold/{id}/dispute", "/securehold/{id}/dispute"})
    @Operation(
        summary = "Dispute SecureHold Escrow",
        description = "Transitions the transaction status to DISPUTED and triggers ticket auto-creation in dispute-service. Can be called by sender or recipient.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dispute recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid state or reason missing"),
            @ApiResponse(responseCode = "403", description = "Forbidden (not a participant)")
        }
    )
    public ResponseEntity<?> disputeSecureHold(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String reason = body.get("reason");

        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required to dispute"));
        }

        try {
            Transaction tx = disputeSecureHoldUseCase.execute(id, currentUser, reason);
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Dispute failed: " + e.getMessage()));
        }
    }

    /**
     * Resolves a disputed escrow. Can only be triggered internally by the dispute-service.
     */
    @PostMapping({"/transactions/securehold/{id}/resolve", "/securehold/{id}/resolve"})
    @Operation(
        summary = "Resolve Escrow Dispute (Internal)",
        description = "Administratively resolves a dispute by either releasing funds to the recipient or refunding them to the sender.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dispute resolved and funds routed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request state or resolution type")
        }
    )
    public ResponseEntity<?> resolveSecureHold(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String resolution = body.get("resolution");
        if (resolution == null || resolution.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Resolution ('RELEASE' or 'REFUND') is required"));
        }
        try {
            Transaction tx = resolveDisputedSecureHoldUseCase.execute(id, resolution);
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Resolution failed: " + e.getMessage()));
        }
    }

    // DTO class
    @lombok.Data
    @Schema(description = "Payload representing standard transfer or secure hold requests")
    public static class TransferRequest {
        @Schema(description = "Username of the money recipient", example = "john_doe")
        private String recipientUsername;
        @Schema(description = "Amount to transfer in NGN", example = "50000.00")
        private BigDecimal amount;
        @Schema(description = "Personal note / description", example = "Payment for services")
        private String note;
        @Schema(description = "Lock duration in hours (only for secure holds)", example = "24")
        private Integer holdDuration;
    }
}
