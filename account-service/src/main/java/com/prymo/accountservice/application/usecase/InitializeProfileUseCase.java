package com.prymo.accountservice.application.usecase;

import com.prymo.accountservice.domain.model.UserProfile;
import com.prymo.accountservice.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class InitializeProfileUseCase {

    private final UserProfileRepository repository;

    public InitializeProfileUseCase(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile execute(String username, String phoneNumber) {
        return repository.findByUsername(username)
                .orElseGet(() -> {
                    UserProfile profile = UserProfile.builder()
                            .username(username)
                            .phoneNumber(phoneNumber != null ? phoneNumber : "+2348031234567")
                            .email(username + "@prymo.com")
                            .kycLevel("Tier 1")
                            .dailyLimit(new BigDecimal("50000.00"))
                            .balance(new BigDecimal("100000.00"))
                            .secureHoldBalance(BigDecimal.ZERO)
                            .build();
                    return repository.save(profile);
                });
    }
}
