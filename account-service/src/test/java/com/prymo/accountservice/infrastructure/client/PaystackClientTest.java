package com.prymo.accountservice.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaystackClientTest {

    @InjectMocks
    private PaystackClient paystackClient;

    @Test
    void testResolveAccountNumberFallbackThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                paystackClient.resolveAccountNumberFallback("1234567890", "044", new RuntimeException("Timeout"))
        );
        assertTrue(exception.getMessage().contains("Paystack resolution service is currently unavailable"));
    }
}
