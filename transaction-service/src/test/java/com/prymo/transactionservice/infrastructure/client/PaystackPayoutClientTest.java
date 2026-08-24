package com.prymo.transactionservice.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PaystackPayoutClientTest {

    @InjectMocks
    private PaystackPayoutClient client;

    @Test
    void testInitiatePayoutFallbackThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                client.initiatePayoutFallback("044", "1234567890", "Name", BigDecimal.TEN, "REF", "Reason", new RuntimeException("API error"))
        );
        assertTrue(exception.getMessage().contains("Paystack API is currently unavailable"));
    }
}
