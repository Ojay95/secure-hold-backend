package com.prymo.authservice.presentation.controller;

import com.prymo.authservice.application.usecase.AuthenticateUserUseCase;
import com.prymo.authservice.application.usecase.ManageOtpUseCase;
import com.prymo.authservice.application.usecase.RegisterUserUseCase;
import com.prymo.authservice.domain.model.UserCredential;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final ManageOtpUseCase manageOtpUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, 
                          AuthenticateUserUseCase authenticateUserUseCase, 
                          ManageOtpUseCase manageOtpUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.manageOtpUseCase = manageOtpUseCase;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegisterRequest request) {
        try {
            UserCredential user = registerUserUseCase.execute(
                    request.getUsername(),
                    request.getPassword(),
                    request.getPhoneNumber()
            );
            return ResponseEntity.ok(Map.of("message", "User registered successfully", "userId", user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authenticateUserUseCase.execute(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(Map.of("token", token, "username", request.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }
        
        manageOtpUseCase.sendOtp(phoneNumber);
        
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully (Logged to console in dev mode)"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        String otpCode = body.get("otpCode");
        
        if (phoneNumber == null || otpCode == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number and OTP code are required"));
        }

        boolean isVerified = manageOtpUseCase.verifyOtp(phoneNumber, otpCode);
        if (isVerified) {
            return ResponseEntity.ok(Map.of("verified", true, "message", "OTP verified successfully"));
        } else {
            return ResponseEntity.status(400).body(Map.of("verified", false, "error", "Invalid or expired OTP"));
        }
    }

    // DTO Inner Classes
    @lombok.Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String phoneNumber;
    }

    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
