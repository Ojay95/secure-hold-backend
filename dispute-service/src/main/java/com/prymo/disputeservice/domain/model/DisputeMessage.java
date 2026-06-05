package com.prymo.disputeservice.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeMessage {
    private Long id;
    private Long disputeId;
    private String senderUsername;
    private String message;
    private LocalDateTime sentAt;
}
