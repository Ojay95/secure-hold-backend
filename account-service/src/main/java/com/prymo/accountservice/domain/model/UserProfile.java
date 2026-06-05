package com.prymo.accountservice.domain.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String kycLevel;
    private BigDecimal dailyLimit;
    private BigDecimal balance;
    private BigDecimal secureHoldBalance;

    public void deduct(BigDecimal amount, boolean isSecureHold) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance");
        }
        balance = balance.subtract(amount);
        if (isSecureHold) {
            secureHoldBalance = secureHoldBalance.add(amount);
        }
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        balance = balance.add(amount);
    }

    public void releaseHeldEscrow(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Release amount must be positive");
        }
        if (secureHoldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient escrow hold balance");
        }
        secureHoldBalance = secureHoldBalance.subtract(amount);
    }

    public void refundHeldEscrow(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        if (secureHoldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient escrow hold balance to refund");
        }
        secureHoldBalance = secureHoldBalance.subtract(amount);
        balance = balance.add(amount);
    }
}
