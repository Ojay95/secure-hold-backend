package com.prymo.authservice.presentation.controller;

import com.prymo.authservice.application.usecase.AuthenticateUserUseCase;
import com.prymo.authservice.application.usecase.ManageOtpUseCase;
import com.prymo.authservice.application.usecase.RegisterUserUseCase;
import com.prymo.authservice.domain.model.UserCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <h2>AuthController</h2>
 * <p>Exposes authentication operations, user signup, and Multi-Factor (OTP) verification flows.</p>
 * <p><strong>Developer Guide:</strong></p>
 * <ul>
 *   <li>The signup flow automatically triggers a REST call downstream to `account-service` to provision the user's ledger profile.</li>
 *   <li>OTP codes are stored in Redis with a 5-minute TTL. If Redis is down, it fails over to a local JVM map cache.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, and Multi-Factor OTP verification")
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

    /**
     * Registers a new user credentials profile in the database and requests ledger initialization downstream.
     */
    @PostMapping("/signup")
    @Operation(
        summary = "User Sign Up",
        description = "Registers a new user credentials profile and triggers downstream ledger setup.",
        responses = {
            @ApiResponse(responseCode = "200", description = "User registered and initialized successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g. username already exists)")
        }
    )
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

    /**
     * Authenticates credentials and returns a signed stateful JWT token.
     */
    @PostMapping("/login")
    @Operation(
        summary = "User Log In",
        description = "Validates credentials and returns a secure JWT access token.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
        }
    )
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authenticateUserUseCase.execute(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(Map.of("token", token, "username", request.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generates a random 6-digit numeric OTP, stores it with TTL, and prints/sends it.
     */
    @PostMapping("/send-otp")
    @Operation(
        summary = "Send OTP Code",
        description = "Generates and sends a 6-digit Multi-Factor Authentication OTP to the user's phone number.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OTP generated successfully"),
            @ApiResponse(responseCode = "400", description = "Phone number missing or invalid")
        }
    )
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }
        
        manageOtpUseCase.sendOtp(phoneNumber);
        
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully (Logged to console in dev mode)"));
    }

    /**
     * Compares user-supplied OTP code against the cached code.
     */
    @PostMapping("/verify-otp")
    @Operation(
        summary = "Verify OTP Code",
        description = "Validates the user-submitted OTP code against the cache.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
        }
    )
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
    @Schema(description = "Payload representing user registration details")
    public static class RegisterRequest {
        @Schema(description = "Unique username for authentication", example = "jane_doe")
        private String username;
        @Schema(description = "Raw password", example = "P@ssword123")
        private String password;
        @Schema(description = "Nigerian phone number for OTP verification", example = "+2348031234567")
        private String phoneNumber;
    }

    @lombok.Data
    @Schema(description = "Payload representing user credentials validation details")
    public static class LoginRequest {
        @Schema(description = "Registered username", example = "jane_doe")
        private String username;
        @Schema(description = "Account password", example = "P@ssword123")
        private String password;
    }
}
