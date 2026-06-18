package com.prymo.accountservice.presentation.controller;

import com.prymo.accountservice.application.usecase.GetProfileUseCase;
import com.prymo.accountservice.application.usecase.InitializeProfileUseCase;
import com.prymo.accountservice.application.usecase.LedgerOperationUseCase;
import com.prymo.accountservice.application.usecase.LinkBankAccountUseCase;
import com.prymo.accountservice.domain.model.LinkedAccount;
import com.prymo.accountservice.domain.model.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h2>AccountController</h2>
 * <p>Manages user profiles, ledger balance operations, and linking third-party bank accounts.</p>
 * <p><strong>Developer Guide:</strong></p>
 * <ul>
 *   <li>Internally invoked by the `transaction-service` to credit, deduct, and refund user balances.</li>
 *   <li>Uses `HeaderAuthFilter` to extract user identity from gateway-relayed `X-User-Username` headers.</li>
 *   <li>Exposes standard bank listing endpoints for linking flows.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Account Management", description = "Endpoints for profiles, ledger balance manipulation, and linked bank accounts")
public class AccountController {

    private final GetProfileUseCase getProfileUseCase;
    private final LinkBankAccountUseCase linkBankAccountUseCase;
    private final LedgerOperationUseCase ledgerOperationUseCase;
    private final InitializeProfileUseCase initializeProfileUseCase;

    public AccountController(GetProfileUseCase getProfileUseCase, 
                             LinkBankAccountUseCase linkBankAccountUseCase, 
                             LedgerOperationUseCase ledgerOperationUseCase,
                             InitializeProfileUseCase initializeProfileUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.linkBankAccountUseCase = linkBankAccountUseCase;
        this.ledgerOperationUseCase = ledgerOperationUseCase;
        this.initializeProfileUseCase = initializeProfileUseCase;
    }

    /**
     * Retrieves the profile associated with the authenticated user.
     */
    @GetMapping("/accounts/profile")
    @Operation(
        summary = "Get User Profile",
        description = "Fetches the detailed ledger profile of the currently authenticated user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
        }
    )
    public ResponseEntity<?> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfile profile = getProfileUseCase.execute(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * Retrieves the list of third-party bank accounts linked by the user.
     */
    @GetMapping("/accounts/linked")
    @Operation(
        summary = "Get Linked Bank Accounts",
        description = "Returns a list of all bank accounts that have been linked by the authenticated user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of linked accounts"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
        }
    )
    public ResponseEntity<?> getLinkedAccounts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<LinkedAccount> accounts = linkBankAccountUseCase.getLinkedAccounts(username);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Links a third-party bank account to the user profile.
     */
    @PostMapping("/accounts/link")
    @Operation(
        summary = "Link Bank Account",
        description = "Associates a verified external bank account with the user's ledger.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Bank account linked successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid payload parameters")
        }
    )
    public ResponseEntity<?> linkAccount(@RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String bankName = body.get("bankName");
        String accountNumber = body.get("accountNumber");

        if (bankName == null || accountNumber == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "bankName and accountNumber are required"));
        }

        LinkedAccount account = linkBankAccountUseCase.link(username, bankName, accountNumber);
        return ResponseEntity.ok(account);
    }

    /**
     * Deducts funds from the available balance. Can lock funds into secure hold balance.
     */
    @PostMapping("/accounts/deduct")
    @Operation(
        summary = "Deduct Balance (Internal)",
        description = "Deducts funds from the user's available balance. Can optionally place the funds into the secure hold escrow bucket.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Deducted successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient funds or invalid request")
        }
    )
    public ResponseEntity<?> deductBalance(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        boolean isSecureHold = (Boolean) body.getOrDefault("isSecureHold", false);

        try {
            UserProfile profile = ledgerOperationUseCase.deduct(username, amount, isSecureHold);
            return ResponseEntity.ok(Map.of(
                    "message", "Deducted successfully", 
                    "balance", profile.getBalance(), 
                    "secureHoldBalance", profile.getSecureHoldBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Credits funds to a user profile balance.
     */
    @PostMapping("/accounts/credit")
    @Operation(
        summary = "Credit Balance (Internal)",
        description = "Credits funds to the user profile's available balance. Can specify if the transfer is being pulled out of secure hold.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Credited successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload parameters")
        }
    )
    public ResponseEntity<?> creditBalance(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        boolean isFromSecureHold = (Boolean) body.getOrDefault("isFromSecureHold", false);
        String senderUsername = (String) body.get("senderUsername");

        try {
            UserProfile profile = ledgerOperationUseCase.credit(username, senderUsername, amount, isFromSecureHold);
            return ResponseEntity.ok(Map.of("message", "Credited successfully", "balance", profile.getBalance()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refunds funds out of secure hold back to the available balance.
     */
    @PostMapping("/accounts/refund")
    @Operation(
        summary = "Refund Held Balance (Internal)",
        description = "Refunds escrowed secure hold balance back to the user's available balance.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Refunded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or transaction reference")
        }
    )
    public ResponseEntity<?> refundBalance(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        try {
            UserProfile profile = ledgerOperationUseCase.refund(username, amount);
            return ResponseEntity.ok(Map.of(
                    "message", "Refunded successfully", 
                    "balance", profile.getBalance(), 
                    "secureHoldBalance", profile.getSecureHoldBalance()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Returns the complete list of supported banks in Nigeria.
     */
    @GetMapping("/banks")
    @Operation(
        summary = "Get Supported Banks List",
        description = "Fetches the full metadata list of supported Nigerian banks for linking accounts.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of banks retrieved successfully")
        }
    )
    public ResponseEntity<?> getSupportedBanks() {
        List<Map<String, String>> banks = List.of(
                Map.of("code", "044", "name", "Access Bank", "color", "bg-purple-500"),
                Map.of("code", "014", "name", "Afribank Nigeria Plc", "color", "bg-red-500"),
                Map.of("code", "023", "name", "Citibank Nigeria Limited", "color", "bg-blue-500"),
                Map.of("code", "050", "name", "Ecobank Nigeria Plc", "color", "bg-green-500"),
                Map.of("code", "084", "name", "Enterprise Bank Limited", "color", "bg-orange-500"),
                Map.of("code", "070", "name", "Fidelity Bank Plc", "color", "bg-indigo-500"),
                Map.of("code", "011", "name", "First Bank of Nigeria", "color", "bg-blue-600"),
                Map.of("code", "214", "name", "First City Monument Bank", "color", "bg-yellow-500"),
                Map.of("code", "058", "name", "Guaranty Trust Bank", "color", "bg-orange-600"),
                Map.of("code", "030", "name", "Heritage Bank", "color", "bg-green-600"),
                Map.of("code", "082", "name", "Keystone Bank", "color", "bg-purple-600"),
                Map.of("code", "076", "name", "Polaris Bank", "color", "bg-blue-700"),
                Map.of("code", "221", "name", "Stanbic IBTC Bank", "color", "bg-blue-800"),
                Map.of("code", "068", "name", "Standard Chartered Bank", "color", "bg-teal-500"),
                Map.of("code", "232", "name", "Sterling Bank", "color", "bg-green-700"),
                Map.of("code", "032", "name", "Union Bank of Nigeria", "color", "bg-red-600"),
                Map.of("code", "033", "name", "United Bank For Africa", "color", "bg-red-700"),
                Map.of("code", "215", "name", "Unity Bank", "color", "bg-orange-700"),
                Map.of("code", "035", "name", "Wema Bank", "color", "bg-purple-700"),
                Map.of("code", "057", "name", "Zenith Bank", "color", "bg-red-800")
        );
        return ResponseEntity.ok(banks);
    }

    /**
     * Initializes the user profile.
     */
    @PostMapping("/accounts/profile/initialize")
    @Operation(
        summary = "Initialize User Profile (Internal)",
        description = "Creates a new UserProfile ledger record upon successful user signup in the auth-service.",
        responses = {
            @ApiResponse(responseCode = "200", description = "User profile initialized successfully"),
            @ApiResponse(responseCode = "400", description = "Username not provided")
        }
    )
    public ResponseEntity<?> initializeProfile(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String phoneNumber = body.get("phoneNumber");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        UserProfile profile = initializeProfileUseCase.execute(username, phoneNumber);
        return ResponseEntity.ok(profile);
    }
}
