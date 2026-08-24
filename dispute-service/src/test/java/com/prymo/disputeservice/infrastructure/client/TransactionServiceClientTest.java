package com.prymo.disputeservice.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceClientTest {

    @InjectMocks
    private TransactionServiceClient client;

    @Test
    void testResolveDisputeFallbackThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                client.resolveDisputeFallback(123L, "RELEASE", new RuntimeException("Service down"))
        );
        assertTrue(exception.getMessage().contains("Transaction service is currently unavailable"));
    }
}
