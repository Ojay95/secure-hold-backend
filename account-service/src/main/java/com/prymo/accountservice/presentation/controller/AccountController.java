package com.prymo.accountservice.presentation.controller;

import com.prymo.accountservice.application.usecase.GetProfileUseCase;
import com.prymo.accountservice.application.usecase.LedgerOperationUseCase;
import com.prymo.accountservice.application.usecase.LinkBankAccountUseCase;
import com.prymo.accountservice.domain.model.LinkedAccount;
import com.prymo.accountservice.domain.model.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetProfileUseCase getProfileUseCase;
    private final LinkBankAccountUseCase linkBankAccountUseCase;
    private final LedgerOperationUseCase ledgerOperationUseCase;

    public AccountController(GetProfileUseCase getProfileUseCase, 
                             LinkBankAccountUseCase linkBankAccountUseCase, 
                             LedgerOperationUseCase ledgerOperationUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.linkBankAccountUseCase = linkBankAccountUseCase;
        this.ledgerOperationUseCase = ledgerOperationUseCase;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserProfile profile = getProfileUseCase.execute(username);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/linked")
    public ResponseEntity<?> getLinkedAccounts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<LinkedAccount> accounts = linkBankAccountUseCase.getLinkedAccounts(username);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/link")
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

    @PostMapping("/deduct")
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

    @PostMapping("/credit")
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

    @PostMapping("/refund")
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
}
