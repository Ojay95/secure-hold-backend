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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveDisputedSecureHoldUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private KafkaEventProducer kafkaProducer;

    private ResolveDisputedSecureHoldUseCase useCase;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        useCase = new ResolveDisputedSecureHoldUseCase(transactionRepository, accountServiceClient, kafkaProducer);

        tx = Transaction.builder()
                .id(789L)
                .senderUsername("sender_user")
                .recipientUsername("recipient_user")
                .amount(new BigDecimal("30000.00"))
                .type("SECUREHOLD")
                .status("DISPUTED")
                .reference("SH-REF-003")
                .build();
    }

    @Test
    void testResolveReleaseSuccess() {
        when(transactionRepository.findById(789L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = useCase.execute(789L, "RELEASE");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        // Verify it credited the recipient from secure hold
        verify(accountServiceClient).credit("recipient_user", "sender_user", new BigDecimal("30000.00"), true);
        verify(kafkaProducer).sendEvent("transaction-events", "SH-REF-003", "RELEASED:sender_user:recipient_user:30000.00");
        verify(transactionRepository).save(tx);
    }

    @Test
    void testResolveRefundSuccess() {
        when(transactionRepository.findById(789L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = useCase.execute(789L, "REFUND");

        assertNotNull(result);
        assertEquals("REFUNDED", result.getStatus());

        // Verify it refunded the sender back to available balance
        verify(accountServiceClient).refund("sender_user", new BigDecimal("30000.00"));
        verify(kafkaProducer).sendEvent("transaction-events", "SH-REF-003", "REFUNDED:sender_user:30000.00");
        verify(transactionRepository).save(tx);
    }

    @Test
    void testResolveActiveTransactionFails() {
        tx.setStatus("ACTIVE"); // Not disputed
        when(transactionRepository.findById(789L)).thenReturn(Optional.of(tx));

        assertThrows(IllegalStateException.class, () -> useCase.execute(789L, "RELEASE"));
        verifyNoInteractions(accountServiceClient, kafkaProducer);
    }

    @Test
    void testResolveInvalidResolutionFails() {
        when(transactionRepository.findById(789L)).thenReturn(Optional.of(tx));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(789L, "CANCEL"));
        verifyNoInteractions(accountServiceClient, kafkaProducer);
    }
}
