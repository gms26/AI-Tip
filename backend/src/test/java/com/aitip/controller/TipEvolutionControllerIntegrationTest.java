package com.aitip.controller;

import com.aitip.dto.TipEvolutionDirection;
import com.aitip.dto.TipEvolutionPeriod;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.service.AiProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TipEvolutionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AiProvider AiProvider;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Evolution User");
        testUser.setEmail("evolution@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);
    }

    @AfterEach
    void tearDown() {
        tipRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void createTip(User user, String currency, String restaurant, double amount, double percentage, LocalDateTime createdAt) {
        Tip tip = new Tip();
        tip.setUser(user);
        tip.setCurrency(currency);
        tip.setRestaurantName(restaurant);
        tip.setBillAmount(BigDecimal.valueOf(50));
        tip.setTipAmount(BigDecimal.valueOf(amount));
        tip.setTipPercentage(BigDecimal.valueOf(percentage));
        tip.setTotalAmount(BigDecimal.valueOf(50 + amount));
        tip.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        tipRepository.save(tip);
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/tip-evolution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Authenticated valid request returns 200 OK with default period")
    void testAuthenticatedValidRequestReturns200() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 15.0, LocalDateTime.now().minusDays(10));

        mockMvc.perform(get("/api/tip-evolution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period", is("LAST_6_MONTHS")))
                .andExpect(jsonPath("$.totalTipCount", is(1)))
                .andExpect(jsonPath("$.currencyTimelines", hasSize(1)))
                .andExpect(jsonPath("$.currencyTimelines[0].currency", is("USD")));
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Period parameter LAST_12_MONTHS correctly parsed")
    void testPeriodParameter() throws Exception {
        mockMvc.perform(get("/api/tip-evolution")
                .param("period", "LAST_12_MONTHS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period", is("LAST_12_MONTHS")));
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Currency parameter filters tips and isolates currency")
    void testCurrencyParameter() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 15.0, LocalDateTime.now());
        createTip(testUser, "EUR", "Bistro", 12.0, 18.0, LocalDateTime.now());

        mockMvc.perform(get("/api/tip-evolution")
                .param("currency", "USD")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.totalTipCount", is(1)))
                .andExpect(jsonPath("$.currencyTimelines", hasSize(1)))
                .andExpect(jsonPath("$.currencyTimelines[0].currency", is("USD")));
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Invalid period returns 400 Bad Request")
    void testInvalidPeriodReturns400() throws Exception {
        mockMvc.perform(get("/api/tip-evolution")
                .param("period", "INVALID_PERIOD")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Invalid currency returns 400 Bad Request")
    void testInvalidCurrencyReturns400() throws Exception {
        mockMvc.perform(get("/api/tip-evolution")
                .param("currency", "INVALID_CURRENCY")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Empty state returns 200 OK with zero tips and INSUFFICIENT_DATA")
    void testEmptyState() throws Exception {
        mockMvc.perform(get("/api/tip-evolution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(0)))
                .andExpect(jsonPath("$.activeMonths", is(0)))
                .andExpect(jsonPath("$.overallDirection", is(TipEvolutionDirection.INSUFFICIENT_DATA.name())))
                .andExpect(jsonPath("$.currencyTimelines", hasSize(0)))
                .andExpect(jsonPath("$.message", containsString("Add some tips to start seeing how your tipping behavior changes over time.")));
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Multi-currency state returns separate timelines without mixing")
    void testMultiCurrencyState() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 15.0, LocalDateTime.now());
        createTip(testUser, "INR", "Dhaba", 100.0, 10.0, LocalDateTime.now());

        mockMvc.perform(get("/api/tip-evolution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(2)))
                .andExpect(jsonPath("$.currencyTimelines", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("User isolation: User never sees other user's tips")
    void testUserIsolation() throws Exception {
        createTip(otherUser, "USD", "Private Restaurant", 50.0, 25.0, LocalDateTime.now());

        mockMvc.perform(get("/api/tip-evolution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(0)))
                .andExpect(jsonPath("$.currencyTimelines", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "evolution@example.com")
    @DisplayName("Groq failure leaves deterministic response intact with null aiExplanation")
    void testGroqFailureHandledGracefully() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 15.0, LocalDateTime.now());
        when(AiProvider.getRecommendation(anyString())).thenThrow(new RuntimeException("Groq timeout"));

        mockMvc.perform(get("/api/tip-evolution")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(1)))
                .andExpect(jsonPath("$.aiExplanation", nullValue()));
    }
}
