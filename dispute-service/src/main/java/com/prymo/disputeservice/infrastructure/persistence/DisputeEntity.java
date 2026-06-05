package com.prymo.disputeservice.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private String transactionReference;

    @Column(nullable = false)
    private String filerUsername;

    @Column(nullable = false)
    private String reason;

    @Builder.Default
    private String status = "OPENED"; // OPENED, UNDER_REVIEW, RESOLVED_SELLER, RESOLVED_BUYER

    private LocalDateTime createdAt;
}
