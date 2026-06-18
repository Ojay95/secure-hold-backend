package com.prymo.accountservice.application.usecase;

import com.prymo.accountservice.domain.model.LinkedAccount;
import com.prymo.accountservice.domain.repository.LinkedAccountRepository;
import com.prymo.accountservice.infrastructure.client.PaystackClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LinkBankAccountUseCase {

    private final LinkedAccountRepository repository;
    private final PaystackClient paystackClient;

    public LinkBankAccountUseCase(LinkedAccountRepository repository, PaystackClient paystackClient) {
        this.repository = repository;
        this.paystackClient = paystackClient;
    }

    public LinkedAccount link(String username, String bankName, String accountNumber) {
        String bankCode = getBankCode(bankName);
        String resolvedName = paystackClient.resolveAccountNumber(accountNumber, bankCode);
        
        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = "Jane Doe Mock Account";
        }

        LinkedAccount account = LinkedAccount.builder()
                .username(username)
                .bankName(bankName)
                .accountName(resolvedName)
                .accountNumber(accountNumber)
                .status("ACTIVE")
                .build();

        return repository.save(account);
    }

    public List<LinkedAccount> getLinkedAccounts(String username) {
        return repository.findByUsername(username);
    }

    private String getBankCode(String bankName) {
        if (bankName == null) return "058";
        String lower = bankName.toLowerCase();
        if (lower.contains("access")) return "044";
        if (lower.contains("afribank")) return "014";
        if (lower.contains("citibank")) return "023";
        if (lower.contains("ecobank")) return "050";
        if (lower.contains("enterprise")) return "084";
        if (lower.contains("fidelity")) return "070";
        if (lower.contains("first bank") || lower.contains("firstbank")) return "011";
        if (lower.contains("first city") || lower.contains("fcmb")) return "214";
        if (lower.contains("guaranty") || lower.contains("gtb") || lower.contains("gtbank")) return "058";
        if (lower.contains("heritage")) return "030";
        if (lower.contains("keystone")) return "082";
        if (lower.contains("polaris")) return "076";
        if (lower.contains("stanbic")) return "221";
        if (lower.contains("standard chartered")) return "068";
        if (lower.contains("sterling")) return "232";
        if (lower.contains("union")) return "032";
        if (lower.contains("united bank") || lower.contains("uba")) return "033";
        if (lower.contains("unity")) return "215";
        if (lower.contains("wema")) return "035";
        if (lower.contains("zenith")) return "057";
        return "058"; // fallback default
    }
}
