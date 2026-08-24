package com.prymo.transactionservice.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder()
                .id(1L)
                .senderUsername("sender")
                .recipientUsername("recipient")
                .amount(new BigDecimal("1000.00"))
                .type("SECUREHOLD")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .reference("SH12345")
                .build();
    }

    @Test
    void testReleaseSuccessFromActive() {
        transaction.release();
        assertEquals("COMPLETED", transaction.getStatus());
    }

    @Test
    void testReleaseSuccessFromDisputed() {
        transaction.setStatus("DISPUTED");
        transaction.release();
        assertEquals("COMPLETED", transaction.getStatus());
    }

    @Test
    void testReleaseInvalidState() {
        transaction.setStatus("COMPLETED");
        assertThrows(IllegalStateException.class, () -> transaction.release());
    }

    @Test
    void testDisputeSuccess() {
        transaction.dispute("Services not delivered");
        assertEquals("DISPUTED", transaction.getStatus());
        assertEquals("Services not delivered", transaction.getDisputeReason());
    }

    @Test
    void testDisputeInvalidState() {
        transaction.setStatus("COMPLETED");
        assertThrows(IllegalStateException.class, () -> transaction.dispute("Some reason"));
    }

    @Test
    void testDisputeMissingReason() {
        assertThrows(IllegalArgumentException.class, () -> transaction.dispute(null));
        assertThrows(IllegalArgumentException.class, () -> transaction.dispute(""));
    }

    @Test
    void testExpireSuccess() {
        transaction.expire();
        assertEquals("EXPIRED", transaction.getStatus());
    }

    @Test
    void testExpireInvalidState() {
        transaction.setStatus("COMPLETED");
        assertThrows(IllegalStateException.class, () -> transaction.expire());
    }

    @Test
    void testRefundSuccess() {
        transaction.setStatus("DISPUTED");
        transaction.refund();
        assertEquals("REFUNDED", transaction.getStatus());
    }

    @Test
    void testRefundInvalidState() {
        assertThrows(IllegalStateException.class, () -> transaction.refund());
    }
}
