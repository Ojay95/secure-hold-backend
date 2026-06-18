package com.prymo.authservice.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);
    private final RestTemplate restTemplate;

    public AccountServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void initializeProfile(String username, String phoneNumber) {
        try {
            String url = "http://account-service/api/v1/accounts/profile/initialize";
            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("phoneNumber", phoneNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Username", username);
            headers.set("X-User-Roles", "ROLE_USER");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, Map.class);
            log.info("Successfully initialized ledger profile for user {} in account-service", username);
        } catch (Exception e) {
            log.error("Failed to initialize profile in account-service for user {}: {}", username, e.getMessage());
        }
    }
}
