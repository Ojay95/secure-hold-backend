package com.prymo.transactionservice.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderUsername;

    @Column(nullable = false)
    private String recipientUsername;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type; // TRANSFER, SECUREHOLD

    @Column(nullable = false)
    private String status; // ACTIVE, COMPLETED, DISPUTED, EXPIRED, REFUNDED

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    private Integer holdDuration;
    private String note;

    @Column(unique = true, nullable = false)
    private String reference;

    private String disputeReason;
}
