package com.prymo.transactionservice.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceClientTest {

    @InjectMocks
    private AccountServiceClient client;

    @Test
    void testDeductFallbackThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                client.deductFallback("john_doe", BigDecimal.TEN, false, new RuntimeException("Service down"))
        );
        assertTrue(exception.getMessage().contains("Available balance deduction failed"));
    }

    @Test
    void testCreditFallbackThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                client.creditFallback("recipient", "sender", BigDecimal.TEN, true, new RuntimeException("Service down"))
        );
        assertTrue(exception.getMessage().contains("Crediting funds failed"));
    }

    @Test
    void testRefundFallbackThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                client.refundFallback("john_doe", BigDecimal.TEN, new RuntimeException("Service down"))
        );
        assertTrue(exception.getMessage().contains("Refunding held funds failed"));
    }
}
