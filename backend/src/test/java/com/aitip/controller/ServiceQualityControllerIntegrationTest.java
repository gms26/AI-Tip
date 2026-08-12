package com.aitip.controller;

import com.aitip.dto.ServiceQuality;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for service quality endpoints:
 *   - GET /api/service-quality/summary
 *   - PATCH /api/tips/{id}/service-quality
 *
 * Verifies: JWT required, user isolation, invalid enum, empty history,
 * update and statistics flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ServiceQualityControllerIntegrationTest {

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

        userA = User.builder()
                .name("Alice")
                .email("alice@sq.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userA);
        tokenA = "Bearer " + jwtTokenProvider.generateToken(userA.getEmail());

        userB = User.builder()
                .name("Bob")
                .email("bob@sq.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userB);
        tokenB = "Bearer " + jwtTokenProvider.generateToken(userB.getEmail());

        // User A has one rated EXCELLENT tip and one GOOD tip
        userATip = Tip.builder()
                .user(userA)
                .restaurantName("Italian Place")
                .billAmount(new BigDecimal("85.00"))
                .tipPercentage(new BigDecimal("20.00"))
                .tipAmount(new BigDecimal("17.00"))
                .totalAmount(new BigDecimal("102.00"))
                .currency("USD")
                .serviceQuality(ServiceQuality.EXCELLENT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        tipRepository.save(userATip);

        Tip goodTip = Tip.builder()
                .user(userA)
                .restaurantName("Burger Joint")
                .billAmount(new BigDecimal("50.00"))
                .tipPercentage(new BigDecimal("17.00"))
                .tipAmount(new BigDecimal("8.50"))
                .totalAmount(new BigDecimal("58.50"))
                .currency("USD")
                .serviceQuality(ServiceQuality.GOOD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        tipRepository.save(goodTip);

        // Historical tip with no rating (simulates pre-Day-6 data)
        Tip historicalTip = Tip.builder()
                .user(userA)
                .restaurantName("Old Place")
                .billAmount(new BigDecimal("40.00"))
                .tipPercentage(new BigDecimal("15.00"))
                .tipAmount(new BigDecimal("6.00"))
                .totalAmount(new BigDecimal("46.00"))
                .currency("USD")
                .serviceQuality(null)  // historical — no rating
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        tipRepository.save(historicalTip);
    }

    // =============================================
    // GET /api/service-quality/summary
    // =============================================

    @Test
    void testSummaryRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/service-quality/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticatedUserCanViewSummary() throws Exception {
        mockMvc.perform(get("/api/service-quality/summary")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRatedTips").value(2))
                .andExpect(jsonPath("$.excellentCount").value(1))
                .andExpect(jsonPath("$.goodCount").value(1))
                .andExpect(jsonPath("$.averageCount").value(0))
                .andExpect(jsonPath("$.poorCount").value(0))
                .andExpect(jsonPath("$.mostCommon").isString())
                .andExpect(jsonPath("$.averageTipPercentageByQuality").isMap());
    }

    @Test
    void testHistoricalNullTipsExcludedFromSummary() throws Exception {
        // User A has 3 tips total but only 2 are rated — historical null is excluded
        mockMvc.perform(get("/api/service-quality/summary")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRatedTips").value(2)); // NOT 3
    }

    @Test
    void testUserACannotSeeUserBStatistics() throws Exception {
        // User B has no tips → should get empty state, NOT User A's data
        mockMvc.perform(get("/api/service-quality/summary")
                .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRatedTips").value(0))
                .andExpect(jsonPath("$.mostCommon").doesNotExist());
    }

    @Test
    void testEmptyHistoryReturnsValidResponse() throws Exception {
        // User B has no tips at all
        mockMvc.perform(get("/api/service-quality/summary")
                .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRatedTips").value(0));
    }

    // =============================================
    // PATCH /api/tips/{id}/service-quality
    // =============================================

    @Test
    void testUpdateServiceQualityRequiresJwt() throws Exception {
        mockMvc.perform(patch("/api/tips/" + userATip.getId() + "/service-quality")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("serviceQuality", "GOOD"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUserCanUpdateOwnServiceQuality() throws Exception {
        mockMvc.perform(patch("/api/tips/" + userATip.getId() + "/service-quality")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("serviceQuality", "GOOD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceQuality").value("GOOD"));
    }

    @Test
    void testUserBCannotUpdateUserATip() throws Exception {
        // User B attempting to PATCH User A's tip → 404 (no enumeration leak)
        mockMvc.perform(patch("/api/tips/" + userATip.getId() + "/service-quality")
                .header("Authorization", tokenB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("serviceQuality", "GOOD"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testInvalidEnumReturns400() throws Exception {
        // "SUPERB" is not a valid ServiceQuality value
        mockMvc.perform(patch("/api/tips/" + userATip.getId() + "/service-quality")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceQuality\": \"SUPERB\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingServiceQualityFieldReturns400() throws Exception {
        mockMvc.perform(patch("/api/tips/" + userATip.getId() + "/service-quality")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serviceQuality\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateAndVerifyStatisticsUpdate() throws Exception {
        // Update User A's EXCELLENT tip to POOR
        mockMvc.perform(patch("/api/tips/" + userATip.getId() + "/service-quality")
                .header("Authorization", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("serviceQuality", "POOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceQuality").value("POOR"));

        // Summary should now reflect: 1 POOR, 1 GOOD, 0 EXCELLENT
        mockMvc.perform(get("/api/service-quality/summary")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poorCount").value(1))
                .andExpect(jsonPath("$.goodCount").value(1))
                .andExpect(jsonPath("$.excellentCount").value(0));
    }
}
