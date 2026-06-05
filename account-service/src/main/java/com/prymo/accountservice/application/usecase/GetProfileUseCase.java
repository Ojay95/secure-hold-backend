package com.prymo.accountservice.application.usecase;

import com.prymo.accountservice.domain.model.UserProfile;
import com.prymo.accountservice.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GetProfileUseCase {

    private final UserProfileRepository repository;

    public GetProfileUseCase(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile execute(String username) {
        return repository.findByUsername(username)
                .orElseGet(() -> {
                    UserProfile profile = UserProfile.builder()
                            .username(username)
                            .phoneNumber("+2348031234567")
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
