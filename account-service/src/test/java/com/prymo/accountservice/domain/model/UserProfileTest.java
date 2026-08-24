package com.prymo.accountservice.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileTest {

    private UserProfile profile;

    @BeforeEach
    void setUp() {
        profile = UserProfile.builder()
                .id(1L)
                .username("john_doe")
                .email("john@prymo.com")
                .phoneNumber("+2348031234567")
                .kycLevel("Tier 1")
                .dailyLimit(new BigDecimal("50000.00"))
                .balance(new BigDecimal("100000.00"))
                .secureHoldBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testDeductSuccess() {
        profile.deduct(new BigDecimal("30000.00"), false);
        assertEquals(new BigDecimal("70000.00"), profile.getBalance());
        assertEquals(BigDecimal.ZERO, profile.getSecureHoldBalance());
    }

    @Test
    void testDeductSecureHoldSuccess() {
        profile.deduct(new BigDecimal("30000.00"), true);
        assertEquals(new BigDecimal("70000.00"), profile.getBalance());
        assertEquals(new BigDecimal("30000.00"), profile.getSecureHoldBalance());
    }

    @Test
    void testDeductNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> profile.deduct(new BigDecimal("-100.00"), false));
        assertThrows(IllegalArgumentException.class, () -> profile.deduct(BigDecimal.ZERO, false));
    }

    @Test
    void testDeductInsufficientBalance() {
        assertThrows(IllegalArgumentException.class, () -> profile.deduct(new BigDecimal("100000.01"), false));
    }

    @Test
    void testCreditSuccess() {
        profile.credit(new BigDecimal("20000.00"));
        assertEquals(new BigDecimal("120000.00"), profile.getBalance());
    }

    @Test
    void testCreditNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> profile.credit(new BigDecimal("-50.00")));
        assertThrows(IllegalArgumentException.class, () -> profile.credit(BigDecimal.ZERO));
    }

    @Test
    void testReleaseHeldEscrowSuccess() {
        profile.setSecureHoldBalance(new BigDecimal("50000.00"));
        profile.releaseHeldEscrow(new BigDecimal("30000.00"));
        assertEquals(new BigDecimal("20000.00"), profile.getSecureHoldBalance());
    }

    @Test
    void testReleaseHeldEscrowInsufficient() {
        profile.setSecureHoldBalance(new BigDecimal("10000.00"));
        assertThrows(IllegalArgumentException.class, () -> profile.releaseHeldEscrow(new BigDecimal("10000.01")));
    }

    @Test
    void testRefundHeldEscrowSuccess() {
        profile.setSecureHoldBalance(new BigDecimal("50000.00"));
        profile.refundHeldEscrow(new BigDecimal("30000.00"));
        assertEquals(new BigDecimal("20000.00"), profile.getSecureHoldBalance());
        assertEquals(new BigDecimal("130000.00"), profile.getBalance());
    }

    @Test
    void testRefundHeldEscrowInsufficient() {
        profile.setSecureHoldBalance(new BigDecimal("10000.00"));
        assertThrows(IllegalArgumentException.class, () -> profile.refundHeldEscrow(new BigDecimal("10000.01")));
    }
}
