package com.prymo.accountservice.infrastructure.persistence;

import com.prymo.accountservice.domain.model.LinkedAccount;
import com.prymo.accountservice.domain.repository.LinkedAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LinkedAccountRepositoryAdapter implements LinkedAccountRepository {

    private final JpaLinkedAccountRepository jpaRepository;

    public LinkedAccountRepositoryAdapter(JpaLinkedAccountRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<LinkedAccount> findByUsername(String username) {
        return jpaRepository.findByUsername(username).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public LinkedAccount save(LinkedAccount account) {
        LinkedAccountEntity entity = toEntity(account);
        LinkedAccountEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private LinkedAccount toDomain(LinkedAccountEntity entity) {
        return LinkedAccount.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .bankName(entity.getBankName())
                .accountName(entity.getAccountName())
                .accountNumber(entity.getAccountNumber())
                .status(entity.getStatus())
                .build();
    }

    private LinkedAccountEntity toEntity(LinkedAccount domain) {
        return LinkedAccountEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .bankName(domain.getBankName())
                .accountName(domain.getAccountName())
                .accountNumber(domain.getAccountNumber())
                .status(domain.getStatus())
                .build();
    }
}
