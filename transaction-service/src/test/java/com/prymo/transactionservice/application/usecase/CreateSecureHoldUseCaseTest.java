package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.messaging.KafkaEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSecureHoldUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private KafkaEventProducer kafkaProducer;

    private CreateSecureHoldUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateSecureHoldUseCase(transactionRepository, accountServiceClient, kafkaProducer);
    }

    @Test
    void testExecuteSuccess() {
        String sender = "sender_user";
        String recipient = "recipient_user";
        BigDecimal amount = new BigDecimal("5000.00");
        String note = "Escrow payment";
        Integer durationHours = 48;

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(1L);
            return tx;
        });

        Transaction result = useCase.execute(sender, recipient, amount, note, durationHours);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(sender, result.getSenderUsername());
        assertEquals(recipient, result.getRecipientUsername());
        assertEquals(amount, result.getAmount());
        assertEquals("SECUREHOLD", result.getType());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(48, result.getHoldDuration());
        assertNotNull(result.getReference());
        assertTrue(result.getReference().startsWith("SH"));

        // Verify collaborator interactions
        verify(accountServiceClient).deduct(sender, amount, true);
        verify(kafkaProducer).sendEvent(eq("transaction-events"), eq(result.getReference()), startsWith("CREATED:sender_user:recipient_user:5000.00"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testExecuteSelfTransferFails() {
        String sender = "john_doe";
        BigDecimal amount = new BigDecimal("5000.00");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(sender, sender, amount, "Escrow", 24));
        verifyNoInteractions(accountServiceClient, kafkaProducer, transactionRepository);
    }
}
