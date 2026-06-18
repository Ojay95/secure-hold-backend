package com.prymo.transactionservice.infrastructure.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    public AccountServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void deduct(String username, BigDecimal amount, boolean isSecureHold) {
        String url = "http://account-service/api/v1/accounts/deduct";
        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("amount", amount);
        request.put("isSecureHold", isSecureHold);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Username", username);
        headers.set("X-User-Roles", "ROLE_USER");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(url, entity, Map.class);
    }

    public void credit(String recipientUsername, String senderUsername, BigDecimal amount, boolean isFromSecureHold) {
        String url = "http://account-service/api/v1/accounts/credit";
        Map<String, Object> request = new HashMap<>();
        request.put("username", recipientUsername);
        request.put("senderUsername", senderUsername);
        request.put("amount", amount);
        request.put("isFromSecureHold", isFromSecureHold);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Username", recipientUsername);
        headers.set("X-User-Roles", "ROLE_USER");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(url, entity, Map.class);
    }

    public void refund(String username, BigDecimal amount) {
        String url = "http://account-service/api/v1/accounts/refund";
        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("amount", amount);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Username", username);
        headers.set("X-User-Roles", "ROLE_USER");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForEntity(url, entity, Map.class);
    }
}
