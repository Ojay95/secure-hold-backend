package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
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
class DisputeSecureHoldUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private KafkaEventProducer kafkaProducer;

    private DisputeSecureHoldUseCase useCase;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        useCase = new DisputeSecureHoldUseCase(transactionRepository, kafkaProducer);

        tx = Transaction.builder()
                .id(456L)
                .senderUsername("sender_user")
                .recipientUsername("recipient_user")
                .amount(new BigDecimal("20000.00"))
                .type("SECUREHOLD")
                .status("ACTIVE")
                .reference("SH-REF-002")
                .build();
    }

    @Test
    void testExecuteSenderDisputeSuccess() {
        when(transactionRepository.findById(456L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = useCase.execute(456L, "sender_user", "Items damaged on arrival");

        assertNotNull(result);
        assertEquals("DISPUTED", result.getStatus());
        assertEquals("Items damaged on arrival", result.getDisputeReason());

        verify(kafkaProducer).sendEvent("transaction-events", "SH-REF-002", "DISPUTED:456:sender_user:Items damaged on arrival");
        verify(transactionRepository).save(tx);
    }

    @Test
    void testExecuteRecipientDisputeSuccess() {
        when(transactionRepository.findById(456L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = useCase.execute(456L, "recipient_user", "Sender refuses to release funds");

        assertNotNull(result);
        assertEquals("DISPUTED", result.getStatus());

        verify(kafkaProducer).sendEvent("transaction-events", "SH-REF-002", "DISPUTED:456:recipient_user:Sender refuses to release funds");
        verify(transactionRepository).save(tx);
    }

    @Test
    void testExecuteNonParticipantFails() {
        when(transactionRepository.findById(456L)).thenReturn(Optional.of(tx));

        assertThrows(SecurityException.class, () -> useCase.execute(456L, "attacker_user", "Steal funds"));
        verifyNoInteractions(kafkaProducer);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
