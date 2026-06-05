package com.prymo.accountservice.domain.repository;

import com.prymo.accountservice.domain.model.LinkedAccount;

import java.util.List;

public interface LinkedAccountRepository {
    List<LinkedAccount> findByUsername(String username);
    LinkedAccount save(LinkedAccount account);
}
