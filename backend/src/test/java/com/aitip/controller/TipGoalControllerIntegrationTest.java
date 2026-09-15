package com.aitip.controller;

import com.aitip.dto.CreateTipGoalRequest;
import com.aitip.entity.*;
import com.aitip.repository.TipGoalRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipGoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipGoalRepository tipGoalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        tipGoalRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .name("User A")
                .email("goala@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenA = jwtTokenProvider.generateToken(userA.getEmail());

        userB = userRepository.save(User.builder()
                .name("User B")
                .email("goalb@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());
    }

    @Test
    void testJwtRequired() throws Exception {
        mockMvc.perform(get("/api/tip-goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateGoal() throws Exception {
        CreateTipGoalRequest request = new CreateTipGoalRequest(
                TipGoalType.TOTAL_TIP_AMOUNT,
                new BigDecimal("500.00"),
                "USD",
                TipGoalPeriod.CURRENT_MONTH,
                null, null, null, null
        );

        mockMvc.perform(post("/api/tip-goals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetValue", is(500.00)));
    }

    @Test
    void testDuplicatePrevention() throws Exception {
        CreateTipGoalRequest request = new CreateTipGoalRequest(
                TipGoalType.TOTAL_TIP_AMOUNT,
                new BigDecimal("500.00"),
                "USD",
                TipGoalPeriod.CURRENT_MONTH,
                null, null, null, null
        );

        mockMvc.perform(post("/api/tip-goals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tip-goals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void testUserIsolation() throws Exception {
        CreateTipGoalRequest request = new CreateTipGoalRequest(
                TipGoalType.TOTAL_TIP_AMOUNT,
                new BigDecimal("500.00"),
                "USD",
                TipGoalPeriod.CURRENT_MONTH,
                null, null, null, null
        );

        String createRes = mockMvc.perform(post("/api/tip-goals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String goalId = objectMapper.readTree(createRes).get("id").asText();

        // Access via User B should be NOT FOUND
        mockMvc.perform(get("/api/tip-goals/" + goalId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void testValidationFails() throws Exception {
        // Missing currency for monetary goal
        String badJson = """
            {
              "goalType": "TOTAL_TIP_AMOUNT",
              "targetValue": 100.00,
              "period": "CURRENT_MONTH"
            }
            """;
            
        mockMvc.perform(post("/api/tip-goals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }
}
