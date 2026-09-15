package com.aitip.controller;

import com.aitip.dto.TipHistorySearchRequest;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
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

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private String tokenB;
    private Tip userATip;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
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

        // Create a tip belonging to User A
        userATip = Tip.builder()
                .user(userA)
                .restaurantName("Alice's Diner")
                .billAmount(new BigDecimal("50.00"))
                .tipPercentage(new BigDecimal("15.00"))
                .tipAmount(new BigDecimal("7.50"))
                .totalAmount(new BigDecimal("57.50"))
                .currency("USD")
                .serviceQuality(com.aitip.dto.ServiceQuality.GOOD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        tipRepository.save(userATip);
    }

    @Test
    void testUserBCannotSeeUserAsHistoryInSearch() throws Exception {
        // User B searches history
        TipHistorySearchRequest request = new TipHistorySearchRequest(
                null, null, null, null, null, null, null, null, null, null, null, 0, 10
        );
        
        mockMvc.perform(post("/api/tips/history/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", empty())); // Should not contain Alice's Diner tip
    }
}
