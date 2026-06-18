package com.prymo.accountservice.application.usecase;

import com.prymo.accountservice.domain.model.UserProfile;
import com.prymo.accountservice.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class LedgerOperationUseCase {

    private final UserProfileRepository repository;

    public LedgerOperationUseCase(UserProfileRepository repository) {
        this.repository = repository;
    }

    private UserProfile getProfile(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found for " + username));
    }

    public UserProfile deduct(String username, BigDecimal amount, boolean isSecureHold) {
        UserProfile profile = getProfile(username);
        profile.deduct(amount, isSecureHold);
        return repository.save(profile);
    }

    public UserProfile credit(String username, String senderUsername, BigDecimal amount, boolean isFromSecureHold) {
        if (username != null && username.contains(":")) {
            // It is an external transfer payout destination.
            if (isFromSecureHold) {
                UserProfile sender = getProfile(senderUsername);
                sender.releaseHeldEscrow(amount);
                return repository.save(sender);
            }
            return getProfile(senderUsername);
        }

        UserProfile profile = getProfile(username);
        
        if (isFromSecureHold) {
            UserProfile sender = getProfile(senderUsername);
            sender.releaseHeldEscrow(amount);
            repository.save(sender);
        }

        profile.credit(amount);
        return repository.save(profile);
    }

    public UserProfile refund(String username, BigDecimal amount) {
        UserProfile profile = getProfile(username);
        profile.refundHeldEscrow(amount);
        return repository.save(profile);
    }
}
