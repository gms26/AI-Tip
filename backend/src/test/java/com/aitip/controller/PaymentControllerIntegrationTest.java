package com.aitip.controller;

import com.aitip.dto.PaymentSessionRequest;
import com.aitip.entity.PaymentSession;
import com.aitip.entity.PaymentSessionStatus;
import com.aitip.entity.User;
import com.aitip.repository.PaymentSessionRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentSessionRepository paymentSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private PaymentSession userASession;

    @BeforeEach
    void setUp() {
        paymentSessionRepository.deleteAll();
        userRepository.deleteAll();

        // Create User A
        userA = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userA);
        tokenA = "Bearer " + jwtTokenProvider.generateToken(userA.getEmail());

        // Create User B
        userB = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userB);
        tokenB = "Bearer " + jwtTokenProvider.generateToken(userB.getEmail());

        // Create a session for User A
        userASession = new PaymentSession();
        userASession.setUserId(userA.getId());
        userASession.setAmount(new BigDecimal("100.00"));
        userASession.setCurrency("USD");
        userASession.setRestaurantName("Alice's Rest");
        userASession.setStatus(PaymentSessionStatus.PENDING);
        userASession.setCreatedAt(LocalDateTime.now());
        userASession.setUpdatedAt(LocalDateTime.now());
        paymentSessionRepository.save(userASession);
    }

    @Test
    void testCreateSession_ValidRequest_ReturnsCreated() throws Exception {
        PaymentSessionRequest request = new PaymentSessionRequest();
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setRestaurantName("New Rest");

        mockMvc.perform(post("/api/payments/session")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").value(PaymentSessionStatus.PENDING.name()));
    }

    @Test
    void testCreateSession_InvalidRequest_Returns400() throws Exception {
        PaymentSessionRequest request = new PaymentSessionRequest();
        request.setAmount(new BigDecimal("-10.00")); // invalid amount
        request.setCurrency("USD");
        request.setRestaurantName("New Rest");

        mockMvc.perform(post("/api/payments/session")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSession_UserBCannotAccessUserASession() throws Exception {
        mockMvc.perform(get("/api/payments/" + userASession.getId())
                .header("Authorization", tokenB))
                .andExpect(status().isNotFound()); // Expect 404 for wrong user access
    }

    @Test
    void testGetSession_WithoutJWT_Returns401() throws Exception {
        mockMvc.perform(get("/api/payments/" + userASession.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCancelSession_ValidSession_ReturnsCancelled() throws Exception {
        mockMvc.perform(post("/api/payments/" + userASession.getId() + "/cancel")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PaymentSessionStatus.CANCELLED.name()));
    }

    @Test
    void testCancelSession_UserBCannotCancelUserASession() throws Exception {
        mockMvc.perform(post("/api/payments/" + userASession.getId() + "/cancel")
                .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }
}
