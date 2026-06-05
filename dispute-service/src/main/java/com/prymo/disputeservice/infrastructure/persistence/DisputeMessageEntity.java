package com.prymo.disputeservice.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long disputeId;

    @Column(nullable = false)
    private String senderUsername;

    @Column(nullable = false)
    private String message;

    private LocalDateTime sentAt;
}
