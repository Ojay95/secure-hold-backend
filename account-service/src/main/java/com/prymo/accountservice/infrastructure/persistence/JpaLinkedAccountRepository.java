package com.prymo.accountservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaLinkedAccountRepository extends JpaRepository<LinkedAccountEntity, Long> {
    List<LinkedAccountEntity> findByUsername(String username);
}
