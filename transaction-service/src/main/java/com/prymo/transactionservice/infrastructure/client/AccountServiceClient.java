package com.prymo.transactionservice.infrastructure.client;

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

        restTemplate.postForEntity(url, request, Map.class);
    }

    public void credit(String recipientUsername, String senderUsername, BigDecimal amount, boolean isFromSecureHold) {
        String url = "http://account-service/api/v1/accounts/credit";
        Map<String, Object> request = new HashMap<>();
        request.put("username", recipientUsername);
        request.put("senderUsername", senderUsername);
        request.put("amount", amount);
        request.put("isFromSecureHold", isFromSecureHold);

        restTemplate.postForEntity(url, request, Map.class);
    }

    public void refund(String username, BigDecimal amount) {
        String url = "http://account-service/api/v1/accounts/refund";
        Map<String, Object> request = new HashMap<>();
        request.put("username", username);
        request.put("amount", amount);

        restTemplate.postForEntity(url, request, Map.class);
    }
}
