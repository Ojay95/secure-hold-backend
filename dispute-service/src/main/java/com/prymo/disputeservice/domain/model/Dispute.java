package com.prymo.disputeservice.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {
    private Long id;
    private Long transactionId;
    private String transactionReference;
    private String filerUsername;
    private String reason;
    private String status; // OPENED, UNDER_REVIEW, RESOLVED_SELLER, RESOLVED_BUYER
    private LocalDateTime createdAt;
}
