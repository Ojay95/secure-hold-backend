package com.prymo.authservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCredential {
    private Long id;
    private String username;
    private String password;
    private String phoneNumber;
    private List<String> roles;

    public void validate() {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        if (!phoneNumber.startsWith("+")) {
            throw new IllegalArgumentException("Phone number must start with country code (+)");
        }
    }
}
