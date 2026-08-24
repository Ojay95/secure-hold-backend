package com.prymo.disputeservice.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prymo.disputeservice.application.usecase.AddDisputeMessageUseCase;
import com.prymo.disputeservice.application.usecase.FileDisputeUseCase;
import com.prymo.disputeservice.application.usecase.ResolveDisputeUseCase;
import com.prymo.disputeservice.domain.model.Dispute;
import com.prymo.disputeservice.domain.model.DisputeMessage;
import com.prymo.disputeservice.domain.repository.DisputeMessageRepository;
import com.prymo.disputeservice.domain.repository.DisputeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DisputeController.class)
class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DisputeRepository disputeRepository;

    @MockBean
    private DisputeMessageRepository messageRepository;

    @MockBean
    private FileDisputeUseCase fileDisputeUseCase;

    @MockBean
    private AddDisputeMessageUseCase addDisputeMessageUseCase;

    @MockBean
    private ResolveDisputeUseCase resolveDisputeUseCase;

    @Test
    void testGetMyDisputesSuccess() throws Exception {
        Dispute dispute = Dispute.builder()
                .id(1L)
                .filerUsername("john_doe")
                .reason("Damaged item")
                .status("OPENED")
                .build();

        when(disputeRepository.findByFilerUsername("john_doe"))
                .thenReturn(Collections.singletonList(dispute));

        mockMvc.perform(get("/api/v1/disputes/my-disputes")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filerUsername").value("john_doe"))
                .andExpect(jsonPath("$[0].reason").value("Damaged item"));
    }

    @Test
    void testFileDisputeSuccess() throws Exception {
        Dispute dispute = Dispute.builder()
                .id(2L)
                .filerUsername("john_doe")
                .transactionId(123L)
                .transactionReference("TX-123")
                .reason("No delivery")
                .status("OPENED")
                .build();

        when(fileDisputeUseCase.execute(eq(123L), eq("TX-123"), eq("john_doe"), eq("No delivery")))
                .thenReturn(dispute);

        DisputeController.FileDisputeRequest request = new DisputeController.FileDisputeRequest();
        request.setTransactionId(123L);
        request.setTransactionReference("TX-123");
        request.setReason("No delivery");

        mockMvc.perform(post("/api/v1/disputes")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filerUsername").value("john_doe"))
                .andExpect(jsonPath("$.reason").value("No delivery"));
    }

    @Test
    void testGetDisputeMessagesSuccess() throws Exception {
        Dispute dispute = Dispute.builder().id(10L).build();
        when(disputeRepository.findById(10L)).thenReturn(Optional.of(dispute));

        DisputeMessage message = DisputeMessage.builder()
                .id(1L)
                .disputeId(10L)
                .senderUsername("john_doe")
                .message("Hello")
                .build();

        when(messageRepository.findByDisputeIdOrderBySentAtAsc(10L))
                .thenReturn(Collections.singletonList(message));

        mockMvc.perform(get("/api/v1/disputes/10/messages")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderUsername").value("john_doe"))
                .andExpect(jsonPath("$[0].message").value("Hello"));
    }

    @Test
    void testSendDisputeMessageSuccess() throws Exception {
        DisputeMessage message = DisputeMessage.builder()
                .id(5L)
                .disputeId(10L)
                .senderUsername("john_doe")
                .message("Test response")
                .build();

        when(addDisputeMessageUseCase.execute(eq(10L), eq("john_doe"), eq("Test response")))
                .thenReturn(message);

        Map<String, String> body = Map.of("message", "Test response");

        mockMvc.perform(post("/api/v1/disputes/10/messages")
                        .header("X-User-Username", "john_doe")
                        .header("X-User-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Test response"));
    }

    @Test
    void testResolveDisputeSuccess() throws Exception {
        Dispute dispute = Dispute.builder()
                .id(10L)
                .status("RESOLVED_BUYER")
                .build();

        when(resolveDisputeUseCase.execute(eq(10L), eq("REFUND")))
                .thenReturn(dispute);

        Map<String, String> body = Map.of("resolution", "REFUND");

        mockMvc.perform(post("/api/v1/disputes/10/resolve")
                        .header("X-User-Username", "admin_user")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED_BUYER"));
    }
}
