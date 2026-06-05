package com.prymo.authservice.application.usecase;

import com.prymo.authservice.domain.model.UserCredential;
import com.prymo.authservice.domain.repository.UserCredentialRepository;
import com.prymo.authservice.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserUseCase {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthenticateUserUseCase(UserCredentialRepository repository, 
                                    PasswordEncoder passwordEncoder, 
                                    JwtTokenProvider tokenProvider) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public String execute(String username, String password) {
        UserCredential user = repository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return tokenProvider.generateToken(user.getUsername(), user.getRoles());
    }
}
