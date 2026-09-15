package com.aitip.controller;

import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipBudgetRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipBudgetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipBudgetRepository tipBudgetRepository;

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
        tipBudgetRepository.deleteAll();
        userRepository.deleteAll();

        userA = User.builder().email("budgetA@example.com").password("hash").name("Budget A").build();
        userB = User.builder().email("budgetB@example.com").password("hash").name("Budget B").build();
        userRepository.save(userA);
        userRepository.save(userB);

        tokenA = jwtTokenProvider.generateToken(userA.getEmail());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());
    }

    // === Auth ===

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/tip-budgets"))
                .andExpect(status().isUnauthorized());
    }

    // === CRUD ===

    @Test
    void createBudget_returnsOk() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;

        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.monthlyLimit").value(100.00))
                .andExpect(jsonPath("$.warningThreshold").value(80));
    }

    @Test
    void updateExistingBudget_returnsUpdatedValues() throws Exception {
        // Create
        String create = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isOk());

        // Update
        String update = """
                {"currency":"USD","monthlyLimit":200.00,"warningThreshold":90}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").value(200.00))
                .andExpect(jsonPath("$.warningThreshold").value(90));
    }

    @Test
    void getBudgets_returnsOnlyAuthenticatedUserBudgets() throws Exception {
        // User A creates a budget
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // User A can see their budget
        mockMvc.perform(get("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currency").value("USD"));

        // User B cannot see User A's budget
        mockMvc.perform(get("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteBudget_works() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/tip-budgets/USD")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // === Status ===

    @Test
    void getBudgetStatus_noTips_returnsNoHistory() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tip-budgets/USD/status")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_HISTORY"))
                .andExpect(jsonPath("$.tipCount").value(0))
                .andExpect(jsonPath("$.confidence").value("LOW"));
    }

    @Test
    void getBudgetStatus_withTips_returnsCorrectCalculation() throws Exception {
        // Create budget
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Add a USD tip
        Tip tip = Tip.builder()
                .user(userA)
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal("20"))
                .tipAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .build();
        tipRepository.save(tip);

        mockMvc.perform(get("/api/tip-budgets/USD/status")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_BUDGET"))
                .andExpect(jsonPath("$.currentMonthTips").value(20.00))
                .andExpect(jsonPath("$.remainingBudget").value(80.00))
                .andExpect(jsonPath("$.percentageUsed").value(20.00))
                .andExpect(jsonPath("$.tipCount").value(1));
    }

    // === Cross-user isolation ===

    @Test
    void userB_cannotAccessUserA_budgetStatus() throws Exception {
        // User A creates budget
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // User B tries to access User A's budget status → 404 (not 403, to avoid revealing existence)
        mockMvc.perform(get("/api/tip-budgets/USD/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // === Validation ===

    @Test
    void invalidCurrency_returns400() throws Exception {
        String body = """
                {"currency":"XYZ","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidMonthlyLimit_zero_returns400() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":0,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidMonthlyLimit_negative_returns400() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":-50,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidWarningThreshold_zero_returns400() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":100,"warningThreshold":0}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidWarningThreshold_above100_returns400() throws Exception {
        String body = """
                {"currency":"USD","monthlyLimit":100,"warningThreshold":101}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // === Currency isolation ===

    @Test
    void inrTips_doNotAffect_usdBudget() throws Exception {
        // Create USD budget
        String body = """
                {"currency":"USD","monthlyLimit":100.00,"warningThreshold":80}
                """;
        mockMvc.perform(put("/api/tip-budgets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Add INR tip (should NOT affect USD budget)
        Tip inrTip = Tip.builder()
                .user(userA)
                .billAmount(new BigDecimal("5000.00"))
                .tipPercentage(new BigDecimal("10"))
                .tipAmount(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("5500.00"))
                .currency("INR")
                .build();
        tipRepository.save(inrTip);

        mockMvc.perform(get("/api/tip-budgets/USD/status")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_HISTORY"))
                .andExpect(jsonPath("$.currentMonthTips").value(0))
                .andExpect(jsonPath("$.tipCount").value(0));
    }
}
