package com.prymo.authservice.application.usecase;

import com.prymo.authservice.domain.model.UserCredential;
import com.prymo.authservice.domain.repository.UserCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegisterUserUseCase {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(UserCredentialRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserCredential execute(String username, String password, String phoneNumber) {
        if (repository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (repository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        UserCredential user = new UserCredential(
                null,
                username,
                passwordEncoder.encode(password),
                phoneNumber,
                List.of("ROLE_USER")
        );

        user.validate();
        return repository.save(user);
    }
}
