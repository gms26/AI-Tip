package com.aitip.controller;

import com.aitip.dto.ReceiptReconciliationRequest;
import com.aitip.dto.RegisterRequest;
import com.aitip.dto.CreateTipRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.service.AuthService;
import com.aitip.service.TipService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReceiptReconciliationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private TipService tipService;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        // Register User A
        RegisterRequest registerReqA = new RegisterRequest("User A", "usera@example.com", "password123");
        tokenA = authService.register(registerReqA).token();

        // Register User B
        RegisterRequest registerReqB = new RegisterRequest("User B", "userb@example.com", "password123");
        tokenB = authService.register(registerReqB).token();
    }

    @Test
    void reconcile_Unauthenticated_Returns401() throws Exception {
        ReceiptReconciliationRequest req = new ReceiptReconciliationRequest(
                new BigDecimal("50.00"), "Cafe", "USD", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reconcile_ValidRequestEmptyHistory_ReturnsNoMatch() throws Exception {
        ReceiptReconciliationRequest req = new ReceiptReconciliationRequest(
                new BigDecimal("50.00"), "Cafe", "USD", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.matchCount").value(0))
                .andExpect(jsonPath("$.matches").isEmpty());
    }

    @Test
    void reconcile_ValidationFailures_Returns400() throws Exception {
        // Negative bill
        ReceiptReconciliationRequest req1 = new ReceiptReconciliationRequest(
                new BigDecimal("-50.00"), "Cafe", "USD", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.billAmount").exists());

        // Blank restaurant
        ReceiptReconciliationRequest req2 = new ReceiptReconciliationRequest(
                new BigDecimal("50.00"), " ", "USD", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.restaurantName").exists());

        // Invalid currency handled by CurrencyValidationUtil
        ReceiptReconciliationRequest req3 = new ReceiptReconciliationRequest(
                new BigDecimal("50.00"), "Cafe", "XYZ", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("currency code")));
    }

    @Test
    void reconcile_UserIsolation_CannotSeeOtherUserTips() throws Exception {
        // User A saves a tip
        CreateTipRequest saveReq = new CreateTipRequest(
                new BigDecimal("45.67"), new BigDecimal("15.00"), "Italian Place", "USD", ServiceQuality.GOOD
        );
        tipService.calculateAndSave(saveReq, "usera@example.com");

        // User B attempts to reconcile same exact receipt
        ReceiptReconciliationRequest recReq = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "USD", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .header("Authorization", "Bearer " + tokenB) // Using User B's token
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.matchCount").value(0));
    }

    @Test
    void reconcile_CurrencyIsolation_NoCrossCurrencyMatches() throws Exception {
        // User A saves USD tip
        CreateTipRequest saveReq = new CreateTipRequest(
                new BigDecimal("45.67"), new BigDecimal("15.00"), "Italian Place", "USD", ServiceQuality.GOOD
        );
        tipService.calculateAndSave(saveReq, "usera@example.com");

        // User A reconciles INR receipt with exact same amount/name
        ReceiptReconciliationRequest recReq = new ReceiptReconciliationRequest(
                new BigDecimal("45.67"), "Italian Place", "INR", null, null);

        mockMvc.perform(post("/api/receipts/reconcile")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.matchCount").value(0));
    }
}
