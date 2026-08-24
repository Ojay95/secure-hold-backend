package com.prymo.transactionservice.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prymo.transactionservice.application.usecase.*;
import com.prymo.transactionservice.domain.model.Transaction;
import com.prymo.transactionservice.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private SendMoneyUseCase sendMoneyUseCase;

    @MockBean
    private CreateSecureHoldUseCase createSecureHoldUseCase;

    @MockBean
    private ReleaseSecureHoldUseCase releaseSecureHoldUseCase;

    @MockBean
    private DisputeSecureHoldUseCase disputeSecureHoldUseCase;

    @MockBean
    private ResolveDisputedSecureHoldUseCase resolveDisputedSecureHoldUseCase;

    @Test
    void testGetMyTransactionsSuccess() throws Exception {
        Transaction tx1 = Transaction.builder()
                .id(1L)
                .senderUsername("john_doe")
                .recipientUsername("jane_doe")
                .amount(new BigDecimal("100.00"))
                .type("TRANSFER")
                .status("COMPLETED")
                .build();

        when(transactionRepository.findBySenderUsernameOrRecipientUsername("john_doe", "john_doe"))
                .thenReturn(Collections.singletonList(tx1));

        mockMvc.perform(get("/api/v1/transactions/my-transactions")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderUsername").value("john_doe"))
                .andExpect(jsonPath("$[0].recipientUsername").value("jane_doe"))
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

    @Test
    void testSendMoneySuccess() throws Exception {
        Transaction tx = Transaction.builder()
                .id(2L)
                .senderUsername("john_doe")
                .recipientUsername("jane_doe")
                .amount(new BigDecimal("50.00"))
                .status("COMPLETED")
                .build();

        when(sendMoneyUseCase.execute(eq("john_doe"), eq("jane_doe"), eq(new BigDecimal("50.00")), eq("Lunch")))
                .thenReturn(tx);

        TransactionController.TransferRequest request = new TransactionController.TransferRequest();
        request.setRecipientUsername("jane_doe");
        request.setAmount(new BigDecimal("50.00"));
        request.setNote("Lunch");

        mockMvc.perform(post("/api/v1/transfer")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUsername").value("john_doe"))
                .andExpect(jsonPath("$.recipientUsername").value("jane_doe"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testCreateSecureHoldSuccess() throws Exception {
        Transaction tx = Transaction.builder()
                .id(3L)
                .senderUsername("john_doe")
                .recipientUsername("jane_doe")
                .amount(new BigDecimal("500.00"))
                .type("SECUREHOLD")
                .status("ACTIVE")
                .build();

        when(createSecureHoldUseCase.execute(eq("john_doe"), eq("jane_doe"), eq(new BigDecimal("500.00")), eq("Escrow"), eq(48)))
                .thenReturn(tx);

        TransactionController.TransferRequest request = new TransactionController.TransferRequest();
        request.setRecipientUsername("jane_doe");
        request.setAmount(new BigDecimal("500.00"));
        request.setNote("Escrow");
        request.setHoldDuration(48);

        mockMvc.perform(post("/api/v1/securehold/create")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderUsername").value("john_doe"))
                .andExpect(jsonPath("$.recipientUsername").value("jane_doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.type").value("SECUREHOLD"));
    }

    @Test
    void testReleaseSecureHoldSuccess() throws Exception {
        Transaction tx = Transaction.builder()
                .id(4L)
                .status("COMPLETED")
                .build();

        when(releaseSecureHoldUseCase.execute(4L, "john_doe")).thenReturn(tx);

        mockMvc.perform(post("/api/v1/securehold/4/release")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testDisputeSecureHoldSuccess() throws Exception {
        Transaction tx = Transaction.builder()
                .id(5L)
                .status("DISPUTED")
                .disputeReason("Defective goods")
                .build();

        when(disputeSecureHoldUseCase.execute(5L, "john_doe", "Defective goods")).thenReturn(tx);

        Map<String, String> request = Map.of("reason", "Defective goods");

        mockMvc.perform(post("/api/v1/securehold/5/dispute")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"))
                .andExpect(jsonPath("$.disputeReason").value("Defective goods"));
    }

    @Test
    void testResolveSecureHoldSuccess() throws Exception {
        Transaction tx = Transaction.builder()
                .id(6L)
                .status("REFUNDED")
                .build();

        when(resolveDisputedSecureHoldUseCase.execute(6L, "REFUND")).thenReturn(tx);

        Map<String, String> request = Map.of("resolution", "REFUND");

        mockMvc.perform(post("/api/v1/securehold/6/resolve")
                        .header("X-User-Username", "admin_user")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
