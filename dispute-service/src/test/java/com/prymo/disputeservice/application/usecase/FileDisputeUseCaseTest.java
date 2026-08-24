package com.prymo.disputeservice.application.usecase;

import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDisputeUseCaseTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private DisputeMessageRepository messageRepository;

    private FileDisputeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FileDisputeUseCase(disputeRepository, messageRepository);
    }

    @Test
    void testExecuteSuccess() {
        Long txId = 123L;
        String txRef = "SH-123";
        String filer = "buyer_user";
        String reason = "Package never arrived";

        when(disputeRepository.findByTransactionId(txId)).thenReturn(Optional.empty());
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute dispute = invocation.getArgument(0);
            dispute.setId(10L);
            return dispute;
        });

        Dispute result = useCase.execute(txId, txRef, filer, reason);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(txId, result.getTransactionId());
        assertEquals(txRef, result.getTransactionReference());
        assertEquals(filer, result.getFilerUsername());
        assertEquals(reason, result.getReason());
        assertEquals("OPENED", result.getStatus());

        // Verify the collaborator repositories are updated
        verify(disputeRepository).save(any(Dispute.class));
        verify(messageRepository).save(argThat(msg -> 
                msg.getDisputeId().equals(10L) &&
                "SYSTEM".equals(msg.getSenderUsername()) &&
                msg.getMessage().contains("Dispute ticket opened by buyer_user")
        ));
    }

    @Test
    void testExecuteDuplicateFails() {
        Long txId = 123L;
        Dispute existingDispute = Dispute.builder().id(10L).build();
        when(disputeRepository.findByTransactionId(txId)).thenReturn(Optional.of(existingDispute));

        assertThrows(IllegalArgumentException.class, () -> 
                useCase.execute(txId, "SH-123", "buyer_user", "Reason")
        );
        
        verify(disputeRepository, never()).save(any(Dispute.class));
        verifyNoInteractions(messageRepository);
    }
}
