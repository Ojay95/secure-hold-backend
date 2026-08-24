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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendMoneyUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private KafkaEventProducer kafkaProducer;

    @Mock
    private PaystackPayoutClient paystackPayoutClient;

    private SendMoneyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendMoneyUseCase(transactionRepository, accountServiceClient, kafkaProducer, paystackPayoutClient);
    }

    @Test
    void testExecuteInternalTransferSuccess() {
        String sender = "sender_user";
        String recipient = "recipient_user";
        BigDecimal amount = new BigDecimal("8000.00");
        String note = "Dinner bill";

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(10L);
            return tx;
        });

        Transaction result = useCase.execute(sender, recipient, amount, note);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("TRANSFER", result.getType());
        assertEquals("COMPLETED", result.getStatus());

        verify(accountServiceClient).deduct(sender, amount, false);
        verify(accountServiceClient).credit(recipient, sender, amount, false);
        verify(kafkaProducer).sendEvent(eq("transaction-events"), eq(result.getReference()), startsWith("COMPLETED:sender_user:recipient_user:8000.00"));
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoInteractions(paystackPayoutClient);
    }

    @Test
    void testExecuteExternalPayoutSuccess() {
        String sender = "sender_user";
        String recipient = "058:0123456789"; // GTBank code + account number
        BigDecimal amount = new BigDecimal("25000.00");
        String note = "Vendor payout";

        when(paystackPayoutClient.initiatePayout(
                eq("058"), eq("0123456789"), eq("External Recipient"), eq(amount), anyString(), eq(note)
        )).thenReturn("PAYSTACK-REF-123");

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(11L);
            return tx;
        });

        Transaction result = useCase.execute(sender, recipient, amount, note);

        assertNotNull(result);
        assertEquals(11L, result.getId());
        assertEquals("TRANSFER", result.getType());
        assertEquals("COMPLETED", result.getStatus());

        verify(accountServiceClient).deduct(sender, amount, false);
        verify(paystackPayoutClient).initiatePayout("058", "0123456789", "External Recipient", amount, result.getReference(), note);
        verify(kafkaProducer).sendEvent(eq("transaction-events"), eq(result.getReference()), startsWith("COMPLETED:sender_user:058:0123456789:25000.00"));
        verify(transactionRepository).save(any(Transaction.class));
        verifyNoMoreInteractions(accountServiceClient);
    }

    @Test
    void testExecuteExternalPayoutFailure() {
        String sender = "sender_user";
        String recipient = "058:0123456789";
        BigDecimal amount = new BigDecimal("25000.00");

        when(paystackPayoutClient.initiatePayout(
                anyString(), anyString(), anyString(), any(BigDecimal.class), anyString(), anyString()
        )).thenReturn(null); // Failure simulation

        assertThrows(IllegalStateException.class, () -> useCase.execute(sender, recipient, amount, "Fail test"));
        verify(accountServiceClient).deduct(sender, amount, false);
        verifyNoInteractions(kafkaProducer, transactionRepository);
    }

    @Test
    void testExecuteSelfTransferFails() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("john_doe", "john_doe", BigDecimal.TEN, "self"));
    }
}
