package com.prymo.transactionservice.application.usecase;

import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import com.prymo.transactionservice.infrastructure.client.AccountServiceClient;
import com.prymo.transactionservice.infrastructure.client.PaystackPayoutClient;
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
class ReleaseSecureHoldUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private KafkaEventProducer kafkaProducer;

    @Mock
    private PaystackPayoutClient paystackPayoutClient;

    private ReleaseSecureHoldUseCase useCase;
    private Transaction tx;

    @BeforeEach
    void setUp() {
        useCase = new ReleaseSecureHoldUseCase(transactionRepository, accountServiceClient, kafkaProducer, paystackPayoutClient);

        tx = Transaction.builder()
                .id(123L)
                .senderUsername("sender_user")
                .recipientUsername("recipient_user")
                .amount(new BigDecimal("15000.00"))
                .type("SECUREHOLD")
                .status("ACTIVE")
                .reference("SH-REF-001")
                .note("Contract work")
                .build();
    }

    @Test
    void testExecuteInternalSuccess() {
        when(transactionRepository.findById(123L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = useCase.execute(123L, "sender_user");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        // Verify balance credit & Kafka notifications
        verify(accountServiceClient).credit("recipient_user", "sender_user", new BigDecimal("15000.00"), true);
        verify(kafkaProducer).sendEvent("transaction-events", "SH-REF-001", "RELEASED:sender_user:recipient_user:15000.00");
        verify(transactionRepository).save(tx);
        verifyNoInteractions(paystackPayoutClient);
    }

    @Test
    void testExecuteExternalPaystackSuccess() {
        tx.setRecipientUsername("044:1234567890"); // BankCode:AccountNumber format
        when(transactionRepository.findById(123L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paystackPayoutClient.initiatePayout(
                eq("044"), eq("1234567890"), anyString(), eq(new BigDecimal("15000.00")), eq("SH-REF-001"), eq("Contract work")
        )).thenReturn("PAYSTACK-TX-999");

        Transaction result = useCase.execute(123L, "sender_user");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());

        // Verify external API + internal credit logic
        verify(paystackPayoutClient).initiatePayout("044", "1234567890", "External Recipient", new BigDecimal("15000.00"), "SH-REF-001", "Contract work");
        verify(accountServiceClient).credit("044:1234567890", "sender_user", new BigDecimal("15000.00"), true);
        verify(kafkaProducer).sendEvent("transaction-events", "SH-REF-001", "RELEASED:sender_user:044:1234567890:15000.00");
        verify(transactionRepository).save(tx);
    }

    @Test
    void testExecuteExternalPaystackFailure() {
        tx.setRecipientUsername("044:1234567890");
        when(transactionRepository.findById(123L)).thenReturn(Optional.of(tx));
        when(paystackPayoutClient.initiatePayout(
                anyString(), anyString(), anyString(), any(BigDecimal.class), anyString(), anyString()
        )).thenReturn(null); // Simulated failure

        assertThrows(IllegalStateException.class, () -> useCase.execute(123L, "sender_user"));
        verifyNoInteractions(accountServiceClient, kafkaProducer);
    }

    @Test
    void testExecuteNotAuthorizedFails() {
        when(transactionRepository.findById(123L)).thenReturn(Optional.of(tx));

        assertThrows(SecurityException.class, () -> useCase.execute(123L, "other_user"));
        verifyNoInteractions(accountServiceClient, kafkaProducer, paystackPayoutClient);
    }

    @Test
    void testExecuteNotFoundFails() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(999L, "sender_user"));
    }
}
