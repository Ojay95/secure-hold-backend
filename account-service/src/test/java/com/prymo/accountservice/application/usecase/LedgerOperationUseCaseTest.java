package com.prymo.accountservice.application.usecase;

import com.prymo.accountservice.domain.model.UserProfile;
import com.prymo.accountservice.domain.repository.UserProfileRepository;
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
class LedgerOperationUseCaseTest {

    @Mock
    private UserProfileRepository repository;

    private LedgerOperationUseCase useCase;

    private UserProfile sender;
    private UserProfile recipient;

    @BeforeEach
    void setUp() {
        useCase = new LedgerOperationUseCase(repository);

        sender = UserProfile.builder()
                .id(1L)
                .username("sender_user")
                .balance(new BigDecimal("100000.00"))
                .secureHoldBalance(new BigDecimal("50000.00"))
                .build();

        recipient = UserProfile.builder()
                .id(2L)
                .username("recipient_user")
                .balance(new BigDecimal("20000.00"))
                .secureHoldBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testDeductSuccess() {
        when(repository.findByUsername("sender_user")).thenReturn(Optional.of(sender));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.deduct("sender_user", new BigDecimal("10000.00"), false);

        assertEquals(new BigDecimal("90000.00"), result.getBalance());
        assertEquals(new BigDecimal("50000.00"), result.getSecureHoldBalance());
        verify(repository).save(sender);
    }

    @Test
    void testDeductSecureHoldSuccess() {
        when(repository.findByUsername("sender_user")).thenReturn(Optional.of(sender));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.deduct("sender_user", new BigDecimal("10000.00"), true);

        assertEquals(new BigDecimal("90000.00"), result.getBalance());
        assertEquals(new BigDecimal("60000.00"), result.getSecureHoldBalance());
        verify(repository).save(sender);
    }

    @Test
    void testCreditInternalWithoutSecureHold() {
        when(repository.findByUsername("recipient_user")).thenReturn(Optional.of(recipient));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.credit("recipient_user", "sender_user", new BigDecimal("15000.00"), false);

        assertEquals(new BigDecimal("35000.00"), result.getBalance());
        verify(repository, never()).findByUsername("sender_user");
        verify(repository).save(recipient);
    }

    @Test
    void testCreditInternalWithSecureHold() {
        when(repository.findByUsername("recipient_user")).thenReturn(Optional.of(recipient));
        when(repository.findByUsername("sender_user")).thenReturn(Optional.of(sender));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.credit("recipient_user", "sender_user", new BigDecimal("15000.00"), true);

        // Recipient balance increases
        assertEquals(new BigDecimal("35000.00"), result.getBalance());
        // Sender secureHoldBalance decreases
        assertEquals(new BigDecimal("35000.00"), sender.getSecureHoldBalance());
        
        verify(repository).save(sender);
        verify(repository).save(recipient);
    }

    @Test
    void testCreditExternalWithoutSecureHold() {
        when(repository.findByUsername("sender_user")).thenReturn(Optional.of(sender));

        // Recipient name contains bank code prefix
        UserProfile result = useCase.credit("044:1234567890", "sender_user", new BigDecimal("15000.00"), false);

        assertEquals(sender, result);
        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void testCreditExternalWithSecureHold() {
        when(repository.findByUsername("sender_user")).thenReturn(Optional.of(sender));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.credit("044:1234567890", "sender_user", new BigDecimal("15000.00"), true);

        // Sender secureHold balance is reduced and saved
        assertEquals(new BigDecimal("35000.00"), sender.getSecureHoldBalance());
        verify(repository).save(sender);
        assertEquals(sender, result);
    }

    @Test
    void testRefundSuccess() {
        when(repository.findByUsername("sender_user")).thenReturn(Optional.of(sender));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.refund("sender_user", new BigDecimal("10000.00"));

        assertEquals(new BigDecimal("40000.00"), result.getSecureHoldBalance());
        assertEquals(new BigDecimal("110000.00"), result.getBalance());
        verify(repository).save(sender);
    }

    @Test
    void testGetProfileNotFound() {
        when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.deduct("unknown", BigDecimal.TEN, false));
    }
}
