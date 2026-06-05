package com.prymo.authservice.domain.repository;

import com.prymo.authservice.domain.model.UserCredential;

import java.util.Optional;

public interface UserCredentialRepository {
    Optional<UserCredential> findByUsername(String username);
    Optional<UserCredential> findByPhoneNumber(String phoneNumber);
    UserCredential save(UserCredential user);
}
