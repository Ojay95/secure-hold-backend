package com.prymo.accountservice.application.usecase;

import com.prymo.accountservice.domain.model.LinkedAccount;
import com.prymo.accountservice.domain.repository.LinkedAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LinkBankAccountUseCase {

    private final LinkedAccountRepository repository;

    public LinkBankAccountUseCase(LinkedAccountRepository repository) {
        this.repository = repository;
    }

    public LinkedAccount link(String username, String bankName, String accountNumber) {
        LinkedAccount account = LinkedAccount.builder()
                .username(username)
                .bankName(bankName)
                .accountName("Jane Doe Mock Account") // Mocked verification name
                .accountNumber(accountNumber)
                .status("ACTIVE")
                .build();

        return repository.save(account);
    }

    public List<LinkedAccount> getLinkedAccounts(String username) {
        return repository.findByUsername(username);
    }
}
