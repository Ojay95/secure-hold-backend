package com.prymo.disputeservice.application.usecase;

import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import com.prymo.disputeservice.infrastructure.client.TransactionServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveDisputeUseCaseTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private DisputeMessageRepository messageRepository;

    @Mock
    private TransactionServiceClient transactionServiceClient;

    private ResolveDisputeUseCase useCase;
    private Dispute dispute;

    @BeforeEach
    void setUp() {
        useCase = new ResolveDisputeUseCase(disputeRepository, messageRepository, transactionServiceClient);

        dispute = Dispute.builder()
                .id(10L)
                .transactionId(123L)
                .filerUsername("buyer_user")
                .status("OPENED")
                .build();
    }

    @Test
    void testResolveReleaseSuccess() {
        when(disputeRepository.findById(10L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dispute result = useCase.execute(10L, "RELEASE");

        assertNotNull(result);
        assertEquals("RESOLVED_SELLER", result.getStatus());

        // Verify clients/repositories are invoked
        verify(transactionServiceClient).resolveDispute(123L, "RELEASE");
        verify(disputeRepository).save(dispute);
        verify(messageRepository).save(argThat(msg -> 
                msg.getDisputeId().equals(10L) &&
                "SYSTEM".equals(msg.getSenderUsername()) &&
                msg.getMessage().contains("Resolution outcome: RESOLVED_SELLER")
        ));
    }

    @Test
    void testResolveRefundSuccess() {
        when(disputeRepository.findById(10L)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dispute result = useCase.execute(10L, "REFUND");

        assertNotNull(result);
        assertEquals("RESOLVED_BUYER", result.getStatus());

        verify(transactionServiceClient).resolveDispute(123L, "REFUND");
        verify(disputeRepository).save(dispute);
        verify(messageRepository).save(argThat(msg -> 
                msg.getDisputeId().equals(10L) &&
                "SYSTEM".equals(msg.getSenderUsername()) &&
                msg.getMessage().contains("Resolution outcome: RESOLVED_BUYER")
        ));
    }

    @Test
    void testResolveClosedDisputeFails() {
        dispute.setStatus("RESOLVED_BUYER"); // already resolved
        when(disputeRepository.findById(10L)).thenReturn(Optional.of(dispute));

        assertThrows(IllegalStateException.class, () -> useCase.execute(10L, "RELEASE"));
        verifyNoInteractions(transactionServiceClient, messageRepository);
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    @Test
    void testResolveInvalidResolutionFails() {
        when(disputeRepository.findById(10L)).thenReturn(Optional.of(dispute));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(10L, "CANCEL"));
        verifyNoInteractions(transactionServiceClient, messageRepository);
    }
}
