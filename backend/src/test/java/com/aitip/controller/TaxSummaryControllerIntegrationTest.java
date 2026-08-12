package com.aitip.controller;

import com.aitip.dto.TaxPeriod;
import com.aitip.dto.TaxSummaryRequest;
import com.aitip.dto.UserResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxSummaryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() throws Exception {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        // User A
        User userA = new User();
        userA.setEmail("usera@example.com");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setName("User A");
        userRepository.save(userA);

        // User B
        User userB = new User();
        userB.setEmail("userb@example.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setName("User B");
        userRepository.save(userB);

        // Create tips for User A
        Tip tip = new Tip();
        tip.setUser(userA);
        tip.setBillAmount(new BigDecimal("100.00"));
        tip.setTipPercentage(new BigDecimal("20.00"));
        tip.setTipAmount(new BigDecimal("20.00"));
        tip.setTotalAmount(new BigDecimal("120.00"));
        tip.setCurrency("USD");
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        tipRepository.save(tip);

        // Need JWT for User A & User B
        // Here we mock the token by sending it as we would in tests or we can call login if available
        // Usually, in these integration tests, there's a util to get the token. 
        // We will call POST /api/auth/login to get it.
        
        userAToken = getJwtToken("usera@example.com", "password");
        userBToken = getJwtToken("userb@example.com", "password");
    }
    
    private String getJwtToken(String email, String password) throws Exception {
        String loginJson = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andReturn().getResponse().getContentAsString();
        
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void getSummary_withoutJwt_returns401() throws Exception {
        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("100"));
        mockMvc.perform(post("/api/tax/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSummary_withInvalidPercentage_returns400() throws Exception {
        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("150"));
        mockMvc.perform(post("/api/tax/summary")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSummary_isolatesUserData() throws Exception {
        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("100"));
        
        // User A should see 1 tip
        mockMvc.perform(post("/api/tax/summary")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipCount").value(1))
                .andExpect(jsonPath("$.totalTips").value(20.00));
                
        // User B should see 0 tips, proving strict isolation
        mockMvc.perform(post("/api/tax/summary")
                .header("Authorization", "Bearer " + userBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipCount").value(0))
                .andExpect(jsonPath("$.totalTips").value(0));
    }
}
