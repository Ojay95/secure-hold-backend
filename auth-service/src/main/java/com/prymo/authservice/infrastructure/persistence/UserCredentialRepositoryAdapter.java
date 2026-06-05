package com.prymo.authservice.infrastructure.persistence;

import com.prymo.authservice.domain.model.UserCredential;
import com.prymo.authservice.domain.repository.UserCredentialRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserCredentialRepositoryAdapter implements UserCredentialRepository {

    private final JpaUserCredentialRepository jpaRepository;

    public UserCredentialRepositoryAdapter(JpaUserCredentialRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        return jpaRepository.findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public Optional<UserCredential> findByPhoneNumber(String phoneNumber) {
        return jpaRepository.findByPhoneNumber(phoneNumber)
                .map(this::toDomain);
    }

    @Override
    public UserCredential save(UserCredential user) {
        UserCredentialEntity entity = toEntity(user);
        UserCredentialEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private UserCredential toDomain(UserCredentialEntity entity) {
        return new UserCredential(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getPhoneNumber(),
                entity.getRoles()
        );
    }

    private UserCredentialEntity toEntity(UserCredential domain) {
        return UserCredentialEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .password(domain.getPassword())
                .phoneNumber(domain.getPhoneNumber())
                .roles(domain.getRoles())
                .build();
    }
}
