package com.prymo.accountservice.infrastructure.persistence;

import com.prymo.accountservice.domain.model.UserProfile;
import com.prymo.accountservice.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserProfileRepositoryAdapter implements UserProfileRepository {

    private final JpaUserProfileRepository jpaRepository;

    public UserProfileRepositoryAdapter(JpaUserProfileRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserProfile> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileEntity entity = toEntity(profile);
        UserProfileEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private UserProfile toDomain(UserProfileEntity entity) {
        return UserProfile.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .kycLevel(entity.getKycLevel())
                .dailyLimit(entity.getDailyLimit())
                .balance(entity.getBalance())
                .secureHoldBalance(entity.getSecureHoldBalance())
                .build();
    }

    private UserProfileEntity toEntity(UserProfile domain) {
        return UserProfileEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .phoneNumber(domain.getPhoneNumber())
                .kycLevel(domain.getKycLevel())
                .dailyLimit(domain.getDailyLimit())
                .balance(domain.getBalance())
                .secureHoldBalance(domain.getSecureHoldBalance())
                .build();
    }
}
