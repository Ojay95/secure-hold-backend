package com.prymo.accountservice.domain.repository;

import com.prymo.accountservice.domain.model.UserProfile;

import java.util.Optional;

public interface UserProfileRepository {
    Optional<UserProfile> findByUsername(String username);
    UserProfile save(UserProfile profile);
}
