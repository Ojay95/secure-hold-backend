package com.prymo.disputeservice.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionServiceClient {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceClient.class);
    private final RestTemplate restTemplate;

    public TransactionServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "transactionService", fallbackMethod = "resolveDisputeFallback")
    public void resolveDispute(Long transactionId, String resolution) {
        try {
            String url = "http://transaction-service/api/v1/securehold/" + transactionId + "/resolve";
            Map<String, String> request = new HashMap<>();
            request.put("resolution", resolution);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Username", "SYSTEM");
            headers.set("X-User-Roles", "ROLE_ADMIN");
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, Map.class);
            log.info("Dispute resolution [{}] forwarded to transaction-service for transactionId {}", resolution, transactionId);
        } catch (Exception e) {
            log.error("Failed to forward dispute resolution [{}] for transactionId {}: {}", resolution, transactionId, e.getMessage());
            throw new RuntimeException("Failed to update transaction status: " + e.getMessage());
        }
    }

    public void resolveDisputeFallback(Long transactionId, String resolution, Throwable t) {
        log.error("Circuit breaker 'transactionService' triggered. Failed to resolve dispute for transaction {}. Error: {}", transactionId, t.getMessage());
        throw new RuntimeException("Transaction service is currently unavailable. Dispute resolution could not be processed.");
    }
}
