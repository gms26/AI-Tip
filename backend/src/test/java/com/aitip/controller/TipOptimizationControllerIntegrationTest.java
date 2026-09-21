package com.aitip.controller;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TipOptimizationController.
 *
 * Verifies: authentication, validation, user isolation,
 * currency isolation, and empty-state handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TipOptimizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        userA = User.builder().email("optA@example.com").password("hash").name("Opt A").build();
        userB = User.builder().email("optB@example.com").password("hash").name("Opt B").build();
        userRepository.save(userA);
        userRepository.save(userB);

        tokenA = jwtTokenProvider.generateToken(userA.getEmail());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());
    }

    // === Auth ===

    @Test
    void unauthenticated_returns401() throws Exception {
        String body = """
                {"currency":"USD","currentTipPercentage":20}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // === Valid request ===

    @Test
    void validRequest_returnsOk() throws Exception {
        String body = """
                {"currency":"USD","currentTipPercentage":20}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.currentTipPercentage").value(20))
                .andExpect(jsonPath("$.sampleSize").value(0))
                .andExpect(jsonPath("$.confidence").value("LOW"));
    }

    // === Validation errors ===

    @Test
    void invalidCurrency_returns400() throws Exception {
        String body = """
                {"currency":"XYZ","currentTipPercentage":20}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePercentage_returns400() throws Exception {
        String body = """
                {"currency":"USD","currentTipPercentage":-5}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void percentageOver100_returns400() throws Exception {
        String body = """
                {"currency":"USD","currentTipPercentage":101}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBillAmount_zeroBill_returns400() throws Exception {
        String body = """
                {"currency":"USD","currentTipPercentage":20,"billAmount":0}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBillAmount_negativeBill_returns400() throws Exception {
        String body = """
                {"currency":"USD","currentTipPercentage":20,"billAmount":-50}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // === Empty currency history ===

    @Test
    void emptyCurrencyHistory_returnsEmptyState() throws Exception {
        // User A has USD tips but requests INR
        addTip(userA, "USD", "20.00");

        String body = """
                {"currency":"INR","currentTipPercentage":15}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(0))
                .andExpect(jsonPath("$.historicalMedianPercentage").isEmpty())
                .andExpect(jsonPath("$.message", containsString("No tipping history")));
    }

    // === User isolation ===

    @Test
    void userA_cannotSeeUserB_statistics() throws Exception {
        // Only User B has USD tips
        addTip(userB, "USD", "25.00");
        addTip(userB, "USD", "30.00");

        // User A requests USD optimization â†’ should see empty state
        String body = """
                {"currency":"USD","currentTipPercentage":20}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(0))
                .andExpect(jsonPath("$.historicalMedianPercentage").isEmpty());
    }

    // === Currency isolation ===

    @Test
    void currencyIsolation_usdTips_doNotAppearInInr() throws Exception {
        // User A has both USD and INR tips
        addTip(userA, "USD", "20.00");
        addTip(userA, "USD", "22.00");
        addTip(userA, "INR", "10.00");

        // Request INR optimization
        String body = """
                {"currency":"INR","currentTipPercentage":12}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(1))
                .andExpect(jsonPath("$.historicalMedianPercentage").value(10.00));
    }

    // === Valid request with tips ===

    @Test
    void validRequest_withTips_returnsCalculatedStats() throws Exception {
        addTip(userA, "USD", "15.00");
        addTip(userA, "USD", "20.00");
        addTip(userA, "USD", "25.00");

        String body = """
                {"currency":"USD","currentTipPercentage":25,"billAmount":1000}
                """;
        mockMvc.perform(post("/api/tip-optimization")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(3))
                .andExpect(jsonPath("$.confidence").value("MEDIUM"))
                .andExpect(jsonPath("$.historicalMedianPercentage").value(20.00))
                .andExpect(jsonPath("$.recommendedMinimumPercentage").value(18.00))
                .andExpect(jsonPath("$.recommendedMaximumPercentage").value(22.00));
    }

    // === Helper ===

    private void addTip(User user, String currency, String tipPercentage) {
        Tip tip = Tip.builder()
                .user(user)
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal(tipPercentage))
                .tipAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("120.00"))
                .currency(currency)
                .build();
        tipRepository.save(tip);
    }
}
