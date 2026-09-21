package com.aitip.controller;

import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import com.aitip.service.AiProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Mock AiProvider so tests don't need a real API key
    @MockBean
    private AiProvider AiProvider;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        // User A with tip history
        User userA = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userA);
        tokenA = "Bearer " + jwtTokenProvider.generateToken(userA.getEmail());

        // Save tips for User A
        tipRepository.save(buildTip(userA, "Italian Place", "18.00", "9.00", "59.00"));
        tipRepository.save(buildTip(userA, "Italian Place", "20.00", "10.00", "60.00"));
        tipRepository.save(buildTip(userA, "Burger Joint", "15.00", "3.00", "23.00"));

        // User B with NO tips
        User userB = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("encoded")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userB);
        tokenB = "Bearer " + jwtTokenProvider.generateToken(userB.getEmail());
    }

    @Test
    void testSummaryRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/personalization/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRestaurantRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/personalization/restaurant")
                .param("name", "Italian Place"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSummaryReturnsUserAStatistics() throws Exception {
        mockMvc.perform(get("/api/personalization/summary")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipCount").value(3))
                .andExpect(jsonPath("$.medianTipPercentage").isNumber())
                .andExpect(jsonPath("$.averageTipPercentage").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void testUserBCannotSeeUserAStatistics() throws Exception {
        // User B has no tips
        mockMvc.perform(get("/api/personalization/summary")
                .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipCount").value(0))
                .andExpect(jsonPath("$.message").value("No tipping history yet."));
    }

    @Test
    void testRestaurantPersonalizationReturnsInsight() throws Exception {
        mockMvc.perform(get("/api/personalization/restaurant")
                .param("name", "Italian Place")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantInsight").isNotEmpty())
                .andExpect(jsonPath("$.restaurantInsight.visitCount").value(2))
                .andExpect(jsonPath("$.restaurantInsight.restaurantName").value("Italian Place"));
    }

    @Test
    void testUserBCannotSeeUserARestaurantHistory() throws Exception {
        mockMvc.perform(get("/api/personalization/restaurant")
                .param("name", "Italian Place")
                .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantInsight.visitCount").value(0));
    }

    @Test
    void testEmptyRestaurantHistoryReturnsValidResponse() throws Exception {
        mockMvc.perform(get("/api/personalization/restaurant")
                .param("name", "Unknown Restaurant")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantInsight.visitCount").value(0));
    }

    private Tip buildTip(User user, String restaurant, String percentage, String amount, String total) {
        return Tip.builder()
                .user(user)
                .restaurantName(restaurant)
                .billAmount(new BigDecimal("50.00"))
                .tipPercentage(new BigDecimal(percentage))
                .tipAmount(new BigDecimal(amount))
                .totalAmount(new BigDecimal(total))
                .currency("USD")
                .serviceQuality(com.aitip.dto.ServiceQuality.GOOD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
