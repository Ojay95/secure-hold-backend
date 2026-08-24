package com.prymo.accountservice.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prymo.accountservice.application.usecase.GetProfileUseCase;
import com.prymo.accountservice.application.usecase.InitializeProfileUseCase;
import com.prymo.accountservice.application.usecase.LedgerOperationUseCase;
import com.prymo.accountservice.application.usecase.LinkBankAccountUseCase;
import com.prymo.accountservice.domain.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetProfileUseCase getProfileUseCase;

    @MockBean
    private LinkBankAccountUseCase linkBankAccountUseCase;

    @MockBean
    private LedgerOperationUseCase ledgerOperationUseCase;

    @MockBean
    private InitializeProfileUseCase initializeProfileUseCase;

    @Test
    void testGetProfileSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .username("john_doe")
                .balance(new BigDecimal("1000.00"))
                .secureHoldBalance(BigDecimal.ZERO)
                .build();

        when(getProfileUseCase.execute("john_doe")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/accounts/profile")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void testGetProfileUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDeductBalanceSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .username("john_doe")
                .balance(new BigDecimal("900.00"))
                .secureHoldBalance(new BigDecimal("100.00"))
                .build();

        when(ledgerOperationUseCase.deduct(eq("john_doe"), eq(new BigDecimal("100.00")), eq(true)))
                .thenReturn(profile);

        Map<String, Object> request = new HashMap<>();
        request.put("username", "john_doe");
        request.put("amount", 100.00);
        request.put("isSecureHold", true);

        mockMvc.perform(post("/api/v1/accounts/deduct")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deducted successfully"))
                .andExpect(jsonPath("$.balance").value(900.00))
                .andExpect(jsonPath("$.secureHoldBalance").value(100.00));
    }

    @Test
    void testCreditBalanceSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .username("recipient_user")
                .balance(new BigDecimal("1100.00"))
                .build();

        when(ledgerOperationUseCase.credit(eq("recipient_user"), eq("sender_user"), eq(new BigDecimal("100.00")), eq(true)))
                .thenReturn(profile);

        Map<String, Object> request = new HashMap<>();
        request.put("username", "recipient_user");
        request.put("senderUsername", "sender_user");
        request.put("amount", 100.00);
        request.put("isFromSecureHold", true);

        mockMvc.perform(post("/api/v1/accounts/credit")
                        .header("X-User-Username", "recipient_user")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credited successfully"))
                .andExpect(jsonPath("$.balance").value(1100.00));
    }

    @Test
    void testRefundBalanceSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .username("john_doe")
                .balance(new BigDecimal("1000.00"))
                .secureHoldBalance(BigDecimal.ZERO)
                .build();

        when(ledgerOperationUseCase.refund(eq("john_doe"), eq(new BigDecimal("100.00"))))
                .thenReturn(profile);

        Map<String, Object> request = new HashMap<>();
        request.put("username", "john_doe");
        request.put("amount", 100.00);

        mockMvc.perform(post("/api/v1/accounts/refund")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Refunded successfully"))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.secureHoldBalance").value(0.00));
    }

    @Test
    void testGetSupportedBanks() throws Exception {
        mockMvc.perform(get("/api/v1/banks")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("044"))
                .andExpect(jsonPath("$[0].name").value("Access Bank"));
    }

    @Test
    void testInitializeProfileSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .username("john_doe")
                .phoneNumber("+2348031234567")
                .balance(new BigDecimal("100000.00"))
                .build();

        when(initializeProfileUseCase.execute(eq("john_doe"), eq("+2348031234567")))
                .thenReturn(profile);

        Map<String, String> request = new HashMap<>();
        request.put("username", "john_doe");
        request.put("phoneNumber", "+2348031234567");

        mockMvc.perform(post("/api/v1/accounts/profile/initialize")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.balance").value(100000.00));
    }
}
