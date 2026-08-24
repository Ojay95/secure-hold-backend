package com.prymo.disputeservice.infrastructure.messaging;

import com.prymo.disputeservice.application.usecase.FileDisputeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaDisputeListenerTest {

    @Mock
    private FileDisputeUseCase fileDisputeUseCase;

    private KafkaDisputeListener listener;

    @BeforeEach
    void setUp() {
        listener = new KafkaDisputeListener(fileDisputeUseCase);
    }

    @Test
    void testConsumeDisputeEventSuccess() {
        String message = "DISPUTED:123:buyer_user:Goods were damaged";
        String reference = "SH-REF-001";

        listener.consumeTransactionEvent(message, reference);

        verify(fileDisputeUseCase).execute(123L, "SH-REF-001", "buyer_user", "Goods were damaged");
    }

    @Test
    void testConsumeDisputeEventWithColonsInReason() {
        String message = "DISPUTED:123:buyer_user:Reason: part 1: part 2";
        String reference = "SH-REF-001";

        listener.consumeTransactionEvent(message, reference);

        verify(fileDisputeUseCase).execute(123L, "SH-REF-001", "buyer_user", "Reason: part 1: part 2");
    }

    @Test
    void testConsumeDisputeEventMalformedTxId() {
        String message = "DISPUTED:not-a-number:buyer_user:Goods were damaged";
        String reference = "SH-REF-001";

        listener.consumeTransactionEvent(message, reference);

        verifyNoInteractions(fileDisputeUseCase);
    }

    @Test
    void testConsumeDisputeEventNotDisputedType() {
        String message = "CREATED:123:buyer_user:some_other_info";
        String reference = "SH-REF-001";

        listener.consumeTransactionEvent(message, reference);

        verifyNoInteractions(fileDisputeUseCase);
    }

    @Test
    void testConsumeDisputeEventTooFewParts() {
        String message = "DISPUTED:123";
        String reference = "SH-REF-001";

        listener.consumeTransactionEvent(message, reference);

        verifyNoInteractions(fileDisputeUseCase);
    }
}
