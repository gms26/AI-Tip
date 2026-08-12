package com.aitip.controller;

import com.aitip.dto.CreateTipRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipControllerIntegrationTest {

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
    private String tokenA;
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
        
        // Add more tips for user A to test pagination
        for (int i = 0; i < 25; i++) {
            tipRepository.save(Tip.builder()
                .user(userA)
                .billAmount(new BigDecimal("10.00"))
                .tipPercentage(new BigDecimal("10.00"))
                .tipAmount(new BigDecimal("1.00"))
                .totalAmount(new BigDecimal("11.00"))
                .currency("USD")
                .serviceQuality(com.aitip.dto.ServiceQuality.AVERAGE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        }
    }

    @Test
    void testUserBCannotAccessUserATip() throws Exception {
        // User B trying to GET User A's tip
        mockMvc.perform(get("/api/tips/" + userATip.getId())
                .header("Authorization", tokenB))
                .andExpect(status().isNotFound()); // 404 ensures we don't leak existence
    }

    @Test
    void testUserBCannotDeleteUserATip() throws Exception {
        // User B trying to DELETE User A's tip
        mockMvc.perform(delete("/api/tips/" + userATip.getId())
                .header("Authorization", tokenB))
                .andExpect(status().isNotFound());
                
        // Ensure tip still exists
        assert(tipRepository.existsById(userATip.getId()));
    }

    @Test
    void testPaginationBehavesCorrectly() throws Exception {
        // User A has 26 tips total. 
        // Request page 0, size 10
        mockMvc.perform(get("/api/tips?page=0&size=10")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(26))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0)) // page 0
                .andExpect(jsonPath("$.size").value(10)); // size 10
                
        // Request page 2 (the last page)
        mockMvc.perform(get("/api/tips?page=2&size=10")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(6))) // 26 total: p0=10, p1=10, p2=6
                .andExpect(jsonPath("$.last").value(true)); // this is the last page
    }
}
