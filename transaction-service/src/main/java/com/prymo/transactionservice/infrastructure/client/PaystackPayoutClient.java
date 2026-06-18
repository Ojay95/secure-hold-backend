package com.prymo.transactionservice.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class PaystackPayoutClient {

    private static final Logger log = LoggerFactory.getLogger(PaystackPayoutClient.class);

    @Value("${paystack.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String initiatePayout(String bankCode, String accountNumber, String accountName, BigDecimal amount, String reference, String reason) {
        if (secretKey == null || secretKey.isBlank()) {
            log.info("Paystack secret key is not configured. Simulating payout/transfer to account {} with mock recipient.", accountNumber);
            return "MOCK_TRANSFER_SUCCESS_ID";
        }

        try {
            // 1. Create Transfer Recipient
            String recipientUrl = "https://api.paystack.co/transferrecipient";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + secretKey);

            Map<String, Object> recipientBody = new HashMap<>();
            recipientBody.put("type", "nuban");
            recipientBody.put("name", accountName);
            recipientBody.put("account_number", accountNumber);
            recipientBody.put("bank_code", bankCode);
            recipientBody.put("currency", "NGN");

            HttpEntity<Map<String, Object>> recipientRequest = new HttpEntity<>(recipientBody, headers);
            log.info("Creating Paystack transfer recipient for {} (number: {})", accountName, accountNumber);
            Map recipientResponse = restTemplate.postForObject(recipientUrl, recipientRequest, Map.class);

            if (recipientResponse == null || !Boolean.TRUE.equals(recipientResponse.get("status"))) {
                log.error("Paystack recipient creation failed: {}", recipientResponse);
                return null;
            }

            Map data = (Map) recipientResponse.get("data");
            String recipientCode = (String) data.get("recipient_code");
            log.info("Paystack transfer recipient created: {}", recipientCode);

            // 2. Initiate Transfer
            String transferUrl = "https://api.paystack.co/transfer";
            Map<String, Object> transferBody = new HashMap<>();
            transferBody.put("source", "balance");
            // Paystack amount is in kobo (NGN * 100)
            BigDecimal koboAmount = amount.multiply(new BigDecimal("100"));
            transferBody.put("amount", koboAmount.intValue());
            transferBody.put("recipient", recipientCode);
            transferBody.put("reference", reference);
            transferBody.put("reason", reason != null ? reason : "Prymo Payout");

            HttpEntity<Map<String, Object>> transferRequest = new HttpEntity<>(transferBody, headers);
            log.info("Initiating Paystack transfer to recipient {} for amount {}", recipientCode, amount);
            Map transferResponse = restTemplate.postForObject(transferUrl, transferRequest, Map.class);

            if (transferResponse != null && Boolean.TRUE.equals(transferResponse.get("status"))) {
                Map transferData = (Map) transferResponse.get("data");
                String transferReference = (String) transferData.get("reference");
                log.info("Paystack transfer initiated successfully. Ref: {}", transferReference);
                return transferReference;
            } else {
                log.error("Paystack transfer initiation failed: {}", transferResponse);
            }
        } catch (Exception e) {
            log.error("Exception during Paystack transfer execution: {}", e.getMessage());
        }

        return null;
    }
}
