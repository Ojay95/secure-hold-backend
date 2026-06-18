package com.prymo.accountservice.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PaystackClient {

    private static final Logger log = LoggerFactory.getLogger(PaystackClient.class);

    @Value("${paystack.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String resolveAccountNumber(String accountNumber, String bankCode) {
        if (secretKey == null || secretKey.isBlank()) {
            log.info("Paystack secret key is not configured. Skipping account resolution.");
            return null;
        }

        try {
            String url = "https://api.paystack.co/bank/resolve?account_number=" + accountNumber + "&bank_code=" + bankCode;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Resolving account number {} with bank code {} via Paystack", accountNumber, bankCode);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("status"))) {
                Map data = (Map) response.getBody().get("data");
                if (data != null && data.containsKey("account_name")) {
                    String resolvedName = (String) data.get("account_name");
                    log.info("Successfully resolved account name: {}", resolvedName);
                    return resolvedName;
                }
            }
        } catch (Exception e) {
            log.error("Failed to resolve bank account number via Paystack: {}", e.getMessage());
        }

        return null;
    }
}
