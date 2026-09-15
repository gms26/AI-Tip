package com.aitip.controller;

import com.aitip.dto.AnalyticsPeriod;
import com.aitip.dto.AnalyticsRequest;
import com.aitip.dto.ServiceQuality;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TipAnalyticsControllerIntegrationTest {

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
        userA.setEmail("analytics-a@example.com");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setName("User A");
        userRepository.save(userA);

        // User B
        User userB = new User();
        userB.setEmail("analytics-b@example.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setName("User B");
        userRepository.save(userB);

        // Tips for User A
        createAndSaveTip(userA, "USD", "20.00", "100.00", "Italian Place", ServiceQuality.GOOD);
        createAndSaveTip(userA, "USD", "18.00", "100.00", "Italian Place", ServiceQuality.EXCELLENT);
        createAndSaveTip(userA, "INR", "15.00", "500.00", "Indian Bistro", ServiceQuality.GOOD);

        // Tips for User B
        createAndSaveTip(userB, "USD", "10.00", "80.00", "Some Place", ServiceQuality.AVERAGE);

        userAToken = getJwtToken("analytics-a@example.com", "password");
        userBToken = getJwtToken("analytics-b@example.com", "password");
    }

    private String getJwtToken(String email, String password) throws Exception {
        String loginJson = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private void createAndSaveTip(User user, String currency, String tipPct, String billAmount, String restaurant, ServiceQuality sq) {
        BigDecimal bill = new BigDecimal(billAmount);
        BigDecimal pct = new BigDecimal(tipPct);
        BigDecimal tipAmt = bill.multiply(pct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal total = bill.add(tipAmt);

        Tip tip = new Tip();
        tip.setUser(user);
        tip.setCurrency(currency);
        tip.setBillAmount(bill);
        tip.setTipPercentage(pct);
        tip.setTipAmount(tipAmt);
        tip.setTotalAmount(total);
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        tipRepository.save(tip);
    }

    // =============================================
    // Authentication
    // =============================================

    @Test
    void getAnalytics_withoutJwt_returns401() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_MONTH, null, null, null, null);
        mockMvc.perform(post("/api/analytics/tips")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // =============================================
    // User isolation
    // =============================================

    @Test
    void getAnalytics_userACannotSeeUserBData() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_MONTH, null, null, null, null);

        // User A should see 3 tips
        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(3));

        // User B should see only 1 tip
        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(1));
    }

    // =============================================
    // Valid request
    // =============================================

    @Test
    void getAnalytics_validRequest_returnsFullResponse() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_MONTH, null, null, null, null);

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(3))
                .andExpect(jsonPath("$.averageTipPercentage").isNumber())
                .andExpect(jsonPath("$.medianTipPercentage").isNumber())
                .andExpect(jsonPath("$.highestTipPercentage").isNumber())
                .andExpect(jsonPath("$.lowestTipPercentage").isNumber())
                .andExpect(jsonPath("$.currencyBreakdown").isArray())
                .andExpect(jsonPath("$.restaurantInsights").isArray())
                .andExpect(jsonPath("$.serviceQualityInsights").isArray())
                .andExpect(jsonPath("$.monthlyTrend").isArray());
    }

    // =============================================
    // Invalid custom dates
    // =============================================

    @Test
    void getAnalytics_customWithMissingDates_returns400() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CUSTOM, null, null, null, null);

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAnalytics_customWithInvertedDates_returns400() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(
                AnalyticsPeriod.CUSTOM,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 1, 1),
                null, null
        );

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =============================================
    // Filters
    // =============================================

    @Test
    void getAnalytics_withRestaurantFilter_onlyReturnsMatchingTips() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(
                AnalyticsPeriod.CURRENT_MONTH, null, null, "Italian Place", null
        );

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(2));
    }

    @Test
    void getAnalytics_withServiceQualityFilter_onlyReturnsMatchingTips() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(
                AnalyticsPeriod.CURRENT_MONTH, null, null, null, ServiceQuality.GOOD
        );

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(2));
    }

    // =============================================
    // Empty history
    // =============================================

    @Test
    void getAnalytics_emptyHistory_returnsEmptyState() throws Exception {
        tipRepository.deleteAll();

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_MONTH, null, null, null, null);

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(0))
                .andExpect(jsonPath("$.currencyBreakdown").isEmpty())
                .andExpect(jsonPath("$.restaurantInsights").isEmpty())
                .andExpect(jsonPath("$.monthlyTrend").isEmpty());
    }

    // =============================================
    // ALL_TIME period
    // =============================================

    @Test
    void getAnalytics_allTime_returnsAllUserTips() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.ALL_TIME, null, null, null, null);

        mockMvc.perform(post("/api/analytics/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount").value(3))
                .andExpect(jsonPath("$.period").value("ALL_TIME"));
    }
}
