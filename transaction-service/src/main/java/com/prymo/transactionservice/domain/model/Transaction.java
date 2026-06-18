package com.prymo.transactionservice.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    private Long id;
    private String senderUsername;
    private String recipientUsername;
    private BigDecimal amount;
    private String type; // TRANSFER, SECUREHOLD
    private String status; // ACTIVE, COMPLETED, DISPUTED, EXPIRED, REFUNDED
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Integer holdDuration;
    private String note;
    private String reference;
    private String disputeReason;

    public void release() {
        if (!"ACTIVE".equals(status) && !"DISPUTED".equals(status)) {
            throw new IllegalStateException("Only active or disputed transactions can be released");
        }
        this.status = "COMPLETED";
    }

    public void dispute(String reason) {
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("Only active transactions can be disputed");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }
        this.status = "DISPUTED";
        this.disputeReason = reason;
    }

    public void expire() {
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("Only active transactions can be expired");
        }
        this.status = "EXPIRED";
    }

    public void refund() {
        if (!"DISPUTED".equals(status)) {
            throw new IllegalStateException("Only disputed transactions can be refunded");
        }
        this.status = "REFUNDED";
    }
}
