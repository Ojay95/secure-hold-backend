package com.prymo.accountservice.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Builder.Default
    private String kycLevel = "Tier 1";

    @Builder.Default
    private BigDecimal dailyLimit = new BigDecimal("50000.00");

    @Builder.Default
    private BigDecimal balance = new BigDecimal("100000.00");

    @Builder.Default
    private BigDecimal secureHoldBalance = new BigDecimal("0.00");
}
