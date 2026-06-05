package com.prymo.transactionservice.presentation.controller;

import com.prymo.transactionservice.application.usecase.*;
import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final SendMoneyUseCase sendMoneyUseCase;
    private final CreateSecureHoldUseCase createSecureHoldUseCase;
    private final ReleaseSecureHoldUseCase releaseSecureHoldUseCase;
    private final DisputeSecureHoldUseCase disputeSecureHoldUseCase;

    public TransactionController(TransactionRepository transactionRepository, 
                                 SendMoneyUseCase sendMoneyUseCase, 
                                 CreateSecureHoldUseCase createSecureHoldUseCase, 
                                 ReleaseSecureHoldUseCase releaseSecureHoldUseCase, 
                                 DisputeSecureHoldUseCase disputeSecureHoldUseCase) {
        this.transactionRepository = transactionRepository;
        this.sendMoneyUseCase = sendMoneyUseCase;
        this.createSecureHoldUseCase = createSecureHoldUseCase;
        this.releaseSecureHoldUseCase = releaseSecureHoldUseCase;
        this.disputeSecureHoldUseCase = disputeSecureHoldUseCase;
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<?> getMyTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Transaction> txList = transactionRepository.findBySenderUsernameOrRecipientUsername(username, username);
        return ResponseEntity.ok(txList);
    }

    @PostMapping("/send")
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

    @PostMapping("/securehold")
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

    @PostMapping("/securehold/{id}/release")
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

    @PostMapping("/securehold/{id}/dispute")
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

    // DTO class
    @lombok.Data
    public static class TransferRequest {
        private String recipientUsername;
        private BigDecimal amount;
        private String note;
        private Integer holdDuration;
    }
}
