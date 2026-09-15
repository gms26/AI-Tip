package com.aitip.controller;

import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipForecastControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .name("Forecast A")
                .email("forecasta@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenA = jwtTokenProvider.generateToken(userA.getEmail());

        userB = userRepository.save(User.builder()
                .name("Forecast B")
                .email("forecastb@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());
    }

    private void createTip(User user, String tipAmount, String tipPct, String currency,
                           LocalDateTime createdAt) {
        Tip tip = new Tip();
        tip.setUser(user);
        tip.setBillAmount(new BigDecimal("100.00"));
        tip.setTipAmount(new BigDecimal(tipAmount));
        tip.setTipPercentage(new BigDecimal(tipPct));
        tip.setTotalAmount(new BigDecimal("100.00").add(new BigDecimal(tipAmount)));
        tip.setCurrency(currency);
        tip.setRestaurantName("Test Restaurant");
        tip.setCreatedAt(createdAt);
        tipRepository.save(tip);
    }

    // =============================================
    // Authentication
    // =============================================

    @Test
    void testJwtRequired() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isUnauthorized());
    }

    // =============================================
    // Empty state
    // =============================================

    @Test
    void testEmptyState() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(0)))
                .andExpect(jsonPath("$.confidence", is("LOW")))
                .andExpect(jsonPath("$.estimatedTipCount", is(0)));
    }

    // =============================================
    // User isolation
    // =============================================

    @Test
    void testUserIsolation() throws Exception {
        // User A has tips, User B does not
        createTip(userA, "20", "20", "USD", LocalDateTime.now().minusDays(1));
        createTip(userA, "30", "30", "USD", LocalDateTime.now().minusDays(2));

        // User A sees their tips
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(2)));

        // User B sees zero
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(0)));
    }

    // =============================================
    // Currency isolation
    // =============================================

    @Test
    void testCurrencyIsolation() throws Exception {
        createTip(userA, "20", "20", "USD", LocalDateTime.now().minusDays(1));
        createTip(userA, "50", "50", "INR", LocalDateTime.now().minusDays(1));

        // USD forecast should only include 1 tip
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(1)))
                .andExpect(jsonPath("$.currency", is("USD")));

        // INR forecast should only include 1 tip
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "INR")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(1)))
                .andExpect(jsonPath("$.currency", is("INR")));
    }

    // =============================================
    // Validation errors
    // =============================================

    @Test
    void testInvalidCurrency() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "XYZ")
                        .param("period", "NEXT_30_DAYS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidPeriod() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidLookback_tooLow() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS")
                        .param("lookbackDays", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidLookback_tooHigh() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS")
                        .param("lookbackDays", "400"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidBudget_negative() throws Exception {
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS")
                        .param("monthlyBudget", "-1"))
                .andExpect(status().isBadRequest());
    }

    // =============================================
    // Valid forecast response structure
    // =============================================

    @Test
    void testValidForecast_responseStructure() throws Exception {
        createTip(userA, "20", "20", "USD", LocalDateTime.now().minusDays(5));
        createTip(userA, "30", "30", "USD", LocalDateTime.now().minusDays(3));
        createTip(userA, "25", "25", "USD", LocalDateTime.now().minusDays(1));

        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS")
                        .param("monthlyBudget", "500")
                        .param("lookbackDays", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.forecastPeriod", is("NEXT_30_DAYS")))
                .andExpect(jsonPath("$.forecastDays", is(30)))
                .andExpect(jsonPath("$.lookbackDays", is(90)))
                .andExpect(jsonPath("$.historicalTipCount", is(3)))
                .andExpect(jsonPath("$.historicalAveragePercentage").isNumber())
                .andExpect(jsonPath("$.historicalMedianPercentage").isNumber())
                .andExpect(jsonPath("$.historicalAverageTipAmount").isNumber())
                .andExpect(jsonPath("$.estimatedMonthlyTipAmount").isNumber())
                .andExpect(jsonPath("$.estimatedTipCount").isNumber())
                .andExpect(jsonPath("$.projectedTipPercentage").isNumber())
                .andExpect(jsonPath("$.confidence").isString())
                .andExpect(jsonPath("$.monthlyBudget").isNumber())
                .andExpect(jsonPath("$.projectedBudgetUsage").isNumber())
                .andExpect(jsonPath("$.budgetStatus").isString())
                .andExpect(jsonPath("$.message").isString());
    }

    // =============================================
    // Budget projection via API
    // =============================================

    @Test
    void testBudgetProjection_viaApi() throws Exception {
        // 3 tips averaging 100 in last 30 days
        createTip(userA, "100", "20", "USD", LocalDateTime.now().minusDays(10));
        createTip(userA, "100", "20", "USD", LocalDateTime.now().minusDays(5));
        createTip(userA, "100", "20", "USD", LocalDateTime.now().minusDays(1));

        // Budget = 200, projected = 300 → OVER_BUDGET
        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS")
                        .param("monthlyBudget", "200")
                        .param("lookbackDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetStatus", is("OVER_BUDGET")));
    }

    // =============================================
    // Lookback filtering
    // =============================================

    @Test
    void testLookbackFiltering() throws Exception {
        // Tip within lookback window
        createTip(userA, "20", "20", "USD", LocalDateTime.now().minusDays(5));
        // Tip outside lookback window (100 days ago, but lookback is 30)
        createTip(userA, "50", "50", "USD", LocalDateTime.now().minusDays(100));

        mockMvc.perform(get("/api/tip-forecast")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("currency", "USD")
                        .param("period", "NEXT_30_DAYS")
                        .param("lookbackDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(1)))
                .andExpect(jsonPath("$.historicalAverageTipAmount", is(20.0)));
    }
}
