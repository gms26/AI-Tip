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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipScenarioControllerIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .name("Scenario A")
                .email("scenarioa@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenA = jwtTokenProvider.generateToken(userA.getEmail());

        userB = userRepository.save(User.builder()
                .name("Scenario B")
                .email("scenariob@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());
    }

    private void createTip(User user, String tipAmount, String tipPct, String currency) {
        Tip tip = new Tip();
        tip.setUser(user);
        tip.setBillAmount(new BigDecimal("100.00"));
        tip.setTipAmount(new BigDecimal(tipAmount));
        tip.setTipPercentage(new BigDecimal(tipPct));
        tip.setTotalAmount(new BigDecimal("100.00").add(new BigDecimal(tipAmount)));
        tip.setCurrency(currency);
        tip.setRestaurantName("Test Restaurant");
        tip.setCreatedAt(LocalDateTime.now().minusDays(5));
        tipRepository.save(tip);
    }

    // =============================================
    // Authentication
    // =============================================

    @Test
    void testJwtRequired() throws Exception {
        String body = """
                {"currency":"USD","billAmount":100,"scenarioPercentages":[10]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // =============================================
    // User isolation
    // =============================================

    @Test
    void testUserIsolation() throws Exception {
        createTip(userA, "20", "20", "USD");
        createTip(userA, "30", "30", "USD");

        String body = """
                {"currency":"USD","billAmount":1000,"scenarioPercentages":[15]}
                """;

        // User A sees their tips
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(2)));

        // User B sees zero
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(0)));
    }

    // =============================================
    // Currency isolation
    // =============================================

    @Test
    void testCurrencyIsolation() throws Exception {
        createTip(userA, "20", "20", "USD");
        createTip(userA, "50", "50", "INR");

        String usdBody = """
                {"currency":"USD","billAmount":1000,"scenarioPercentages":[15]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usdBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(1)))
                .andExpect(jsonPath("$.currency", is("USD")));

        String inrBody = """
                {"currency":"INR","billAmount":1000,"scenarioPercentages":[15]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inrBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(1)))
                .andExpect(jsonPath("$.currency", is("INR")));
    }

    // =============================================
    // Validation errors
    // =============================================

    @Test
    void testValidation_noCurrency() throws Exception {
        String body = """
                {"billAmount":100,"scenarioPercentages":[10]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidation_zeroBillAmount() throws Exception {
        String body = """
                {"currency":"USD","billAmount":0,"scenarioPercentages":[10]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidation_emptyScenarios() throws Exception {
        String body = """
                {"currency":"USD","billAmount":100,"scenarioPercentages":[]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidation_tooManyScenarios() throws Exception {
        String body = """
                {"currency":"USD","billAmount":100,"scenarioPercentages":[5,10,15,20,25,30]}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidation_negativeBudget() throws Exception {
        String body = """
                {"currency":"USD","billAmount":100,"scenarioPercentages":[10],"monthlyBudget":-1}
                """;
        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // =============================================
    // Happy path — response structure
    // =============================================

    @Test
    void testHappyPath_responseStructure() throws Exception {
        createTip(userA, "15", "15", "USD");
        createTip(userA, "20", "20", "USD");
        createTip(userA, "25", "25", "USD");

        String body = """
                {"currency":"USD","billAmount":1000,"scenarioPercentages":[10,15,20],"monthlyBudget":5000}
                """;

        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.billAmount", is(1000)))
                .andExpect(jsonPath("$.historicalTipCount", is(3)))
                .andExpect(jsonPath("$.historicalMedianTipPercentage").isNumber())
                .andExpect(jsonPath("$.historicalAverageTipPercentage").isNumber())
                .andExpect(jsonPath("$.historicalAverageTipAmount").isNumber())
                .andExpect(jsonPath("$.scenarioResults", hasSize(3)))
                .andExpect(jsonPath("$.scenarioResults[0].tipPercentage", is(10)))
                .andExpect(jsonPath("$.scenarioResults[0].tipAmount").isNumber())
                .andExpect(jsonPath("$.scenarioResults[0].totalAmount").isNumber())
                .andExpect(jsonPath("$.scenarioResults[0].differenceFromHistorical").isNumber())
                .andExpect(jsonPath("$.scenarioResults[0].monetaryDifference").isNumber())
                .andExpect(jsonPath("$.scenarioResults[0].monthlyProjectedTipAmount").isNumber())
                .andExpect(jsonPath("$.scenarioResults[0].budgetStatus").isString())
                .andExpect(jsonPath("$.monthlyBudget", is(5000)))
                .andExpect(jsonPath("$.historicalMonthlyTipAmount").isNumber())
                .andExpect(jsonPath("$.message").isString());
    }

    // =============================================
    // No history — scenarios still work
    // =============================================

    @Test
    void testNoHistory_scenariosStillCompute() throws Exception {
        String body = """
                {"currency":"GBP","billAmount":500,"scenarioPercentages":[10,20]}
                """;

        mockMvc.perform(post("/api/tip-scenarios")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalTipCount", is(0)))
                .andExpect(jsonPath("$.historicalMedianTipPercentage").doesNotExist())
                .andExpect(jsonPath("$.scenarioResults", hasSize(2)))
                .andExpect(jsonPath("$.scenarioResults[0].tipAmount", is(50.0)))
                .andExpect(jsonPath("$.scenarioResults[0].totalAmount", is(550.0)))
                .andExpect(jsonPath("$.scenarioResults[0].differenceFromHistorical").doesNotExist())
                .andExpect(jsonPath("$.scenarioResults[1].tipAmount", is(100.0)))
                .andExpect(jsonPath("$.scenarioResults[1].totalAmount", is(600.0)));
    }
}
