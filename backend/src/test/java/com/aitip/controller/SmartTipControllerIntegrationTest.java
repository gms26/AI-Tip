package com.aitip.controller;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.SmartTipRequest;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.service.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SmartTipControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProvider AiProvider;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Smart User");
        testUser.setEmail("smartuser@example.com");
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

    private Tip saveTip(User user, String currency, double amount, double percentage, String restaurant, ServiceQuality sq) {
        Tip tip = new Tip();
        tip.setUser(user);
        tip.setCurrency(currency);
        tip.setBillAmount(BigDecimal.valueOf(100.00));
        tip.setTipAmount(BigDecimal.valueOf(amount));
        tip.setTipPercentage(BigDecimal.valueOf(percentage));
        tip.setTotalAmount(BigDecimal.valueOf(100.00 + amount));
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(LocalDateTime.now().minusDays(2));
        tip.setUpdatedAt(LocalDateTime.now().minusDays(2));
        return tipRepository.save(tip);
    }

    // =========================================================================
    // 1. SECURITY & AUTHENTICATION
    // =========================================================================

    @Test
    @DisplayName("Security: Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Should get decision memory summary successfully")
    void getDecisionMemory_success() throws Exception {
        mockMvc.perform(get("/api/smart-tip/decision-memory")
                        .param("currency", "USD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.totalFeedback", is(0)))
                .andExpect(jsonPath("$.confidence", is("LOW")))
                .andExpect(jsonPath("$.recentDecisions", hasSize(0)));
    }

    // =========================================================================
    // 2. VALIDATION
    // =========================================================================

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Validation: Bill amount 0.01 returns 400 Bad Request")
    void testBillAmountMinBoundaryReturns400() throws Exception {
        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("0.01"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Validation: Missing bill amount returns 400 Bad Request")
    void testMissingBillAmountReturns400() throws Exception {
        SmartTipRequest request = new SmartTipRequest("USD", null, null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Validation: Invalid currency returns 400 Bad Request")
    void testInvalidCurrencyReturns400() throws Exception {
        SmartTipRequest request = new SmartTipRequest("XYZ123", new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 3. NO HISTORY / FIRST TIME USER
    // =========================================================================

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("No History: Returns standard neutral suggestions and 200 OK")
    void testNoHistoryReturnsStandardSuggestions() throws Exception {
        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("100.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.billAmount").value(100.00))
                .andExpect(jsonPath("$.historicalMedianTipPercentage").doesNotExist())
                .andExpect(jsonPath("$.suggestions", hasSize(4)))
                .andExpect(jsonPath("$.suggestions[0].tipPercentage").value(10.00))
                .andExpect(jsonPath("$.suggestions[1].tipPercentage").value(15.00))
                .andExpect(jsonPath("$.suggestions[2].tipPercentage").value(18.00))
                .andExpect(jsonPath("$.suggestions[3].tipPercentage").value(20.00))
                .andExpect(jsonPath("$.primarySuggestion.isRecommended").value(true))
                .andExpect(jsonPath("$.message", containsString("General tip options")));
    }

    // =========================================================================
    // 4. PERSONALIZED SUGGESTIONS WITH HISTORY
    // =========================================================================

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Personalized: Returns computed median, suggestions, and primary suggestion")
    void testPersonalizedSuggestions() throws Exception {
        saveTip(testUser, "USD", 15.00, 15.00, "Cafe Uno", ServiceQuality.GOOD);
        saveTip(testUser, "USD", 18.00, 18.00, "Cafe Dos", ServiceQuality.GOOD);
        saveTip(testUser, "USD", 20.00, 20.00, "Cafe Tres", ServiceQuality.EXCELLENT);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, new BigDecimal("15.00"));

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.billAmount").value(50.00))
                .andExpect(jsonPath("$.historicalMedianTipPercentage").value(18.00))
                .andExpect(jsonPath("$.suggestions", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.primarySuggestion").exists())
                .andExpect(jsonPath("$.primarySuggestion.isRecommended").value(true));
    }

    // =========================================================================
    // 5. CURRENCY & USER ISOLATION
    // =========================================================================

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Currency Isolation: Requesting EUR ignores USD tips")
    void testCurrencyIsolation() throws Exception {
        saveTip(testUser, "USD", 25.00, 25.00, "NYC Diner", ServiceQuality.GOOD);

        SmartTipRequest request = new SmartTipRequest("EUR", new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.historicalMedianTipPercentage").doesNotExist())
                .andExpect(jsonPath("$.message", containsString("General tip options")));
    }

    @Test
    @WithMockUser(username = "other@example.com")
    @DisplayName("User Isolation: Other user cannot see smartuser's tipping history")
    void testUserIsolation() throws Exception {
        saveTip(testUser, "USD", 25.00, 25.00, "NYC Diner", ServiceQuality.GOOD);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historicalMedianTipPercentage").doesNotExist())
                .andExpect(jsonPath("$.message", containsString("General tip options")));
    }

    // =========================================================================
    // 6. GROQ INTEGRATION & FALLBACK
    // =========================================================================

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Groq Success: Populates aiExplanation when Groq succeeds")
    void testGroqExplanationPopulated() throws Exception {
        saveTip(testUser, "USD", 18.00, 18.00, "Cafe Uno", ServiceQuality.GOOD);
        when(AiProvider.getRecommendation(anyString())).thenReturn("This recommendation fits your typical dining habits.");

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiExplanation").value("This recommendation fits your typical dining habits."));
    }

    @Test
    @WithMockUser(username = "smartuser@example.com")
    @DisplayName("Groq Failure: Endpoint still succeeds 200 OK when Groq times out or fails")
    void testGroqFailureFallback() throws Exception {
        saveTip(testUser, "USD", 18.00, 18.00, "Cafe Uno", ServiceQuality.GOOD);
        when(AiProvider.getRecommendation(anyString())).thenThrow(new RuntimeException("Groq quota exceeded"));

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiExplanation").doesNotExist())
                .andExpect(jsonPath("$.primarySuggestion").exists());
    }
}
