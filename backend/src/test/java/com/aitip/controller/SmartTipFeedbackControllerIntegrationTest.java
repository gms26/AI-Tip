package com.aitip.controller;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.SmartTipFeedbackRequest;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SmartTipFeedbackControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRecommendationFeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProvider AiProvider;

    private User testUserA;
    private User testUserB;

    private static final String USER_A_EMAIL = "feedback_userA@example.com";
    private static final String USER_B_EMAIL = "feedback_userB@example.com";

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();

        testUserA = new User();
        testUserA.setName("User A");
        testUserA.setEmail(USER_A_EMAIL);
        testUserA.setPassword(passwordEncoder.encode("password123"));
        testUserA.setCreatedAt(LocalDateTime.now());
        testUserA.setUpdatedAt(LocalDateTime.now());
        testUserA = userRepository.save(testUserA);

        testUserB = new User();
        testUserB.setName("User B");
        testUserB.setEmail(USER_B_EMAIL);
        testUserB.setPassword(passwordEncoder.encode("password123"));
        testUserB.setCreatedAt(LocalDateTime.now());
        testUserB.setUpdatedAt(LocalDateTime.now());
        testUserB = userRepository.save(testUserB);
    }

    @AfterEach
    void tearDown() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. POST /api/smart-tip/feedback without token returns 401 Unauthorized")
    void recordFeedback_Unauthorized() throws Exception {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Bistro", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );

        mockMvc.perform(post("/api/smart-tip/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("2. POST /api/smart-tip/feedback successfully records ACCEPTED decision")
    void recordFeedback_Accepted_Success() throws Exception {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Bistro", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );

        mockMvc.perform(post("/api/smart-tip/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackType", is("ACCEPTED")))
                .andExpect(jsonPath("$.suggestedTipPercentage", is(15.00)))
                .andExpect(jsonPath("$.chosenTipPercentage", is(15.00)))
                .andExpect(jsonPath("$.differencePercentagePoints", is(0.00)));

        assertEquals(1, feedbackRepository.count());
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("3. POST /api/smart-tip/feedback successfully records MODIFIED decision")
    void recordFeedback_Modified_Success() throws Exception {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Sushi Zen", ServiceQuality.EXCELLENT,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("18.00")
        );

        mockMvc.perform(post("/api/smart-tip/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackType", is("MODIFIED")))
                .andExpect(jsonPath("$.suggestedTipPercentage", is(15.00)))
                .andExpect(jsonPath("$.chosenTipPercentage", is(18.00)))
                .andExpect(jsonPath("$.differencePercentagePoints", is(3.00)));
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("4. POST /api/smart-tip/feedback successfully records CUSTOM decision")
    void recordFeedback_Custom_Success() throws Exception {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), null, ServiceQuality.GOOD,
                null, null, new BigDecimal("20.00")
        );

        mockMvc.perform(post("/api/smart-tip/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackType", is("CUSTOM")))
                .andExpect(jsonPath("$.suggestedTipPercentage").doesNotExist())
                .andExpect(jsonPath("$.chosenTipPercentage", is(20.00)))
                .andExpect(jsonPath("$.differencePercentagePoints").doesNotExist());
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("5. POST /api/smart-tip/feedback rejects billAmount <= 0.01 with 400")
    void recordFeedback_InvalidBill_BadRequest() throws Exception {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("0.01"), "Bistro", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );

        mockMvc.perform(post("/api/smart-tip/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("6. POST /api/smart-tip/feedback rejects tip percentage > 100 with 400")
    void recordFeedback_Over100Tip_BadRequest() throws Exception {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Bistro", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("105.00")
        );

        mockMvc.perform(post("/api/smart-tip/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("7. GET /api/smart-tip/feedback returns zero counts for new user")
    void getFeedbackSummary_NewUser_Empty() throws Exception {
        mockMvc.perform(get("/api/smart-tip/feedback")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.feedbackCount", is(0)))
                .andExpect(jsonPath("$.acceptedCount", is(0)))
                .andExpect(jsonPath("$.modifiedCount", is(0)))
                .andExpect(jsonPath("$.customCount", is(0)))
                .andExpect(jsonPath("$.direction", is("INSUFFICIENT_DATA")))
                .andExpect(jsonPath("$.adaptationApplied", is(false)));
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("8. Strict User Isolation: User A cannot see User B's feedback")
    void userIsolation_Verified() throws Exception {
        // Seed feedback for User B
        feedbackRepository.save(TipRecommendationFeedback.builder()
                .user(testUserB)
                .currency("USD")
                .billAmount(new BigDecimal("50.00"))
                .suggestedTipPercentage(new BigDecimal("15.00"))
                .chosenTipPercentage(new BigDecimal("18.00"))
                .differencePercentagePoints(new BigDecimal("3.00"))
                .feedbackType(com.aitip.dto.SmartTipFeedbackType.MODIFIED)
                .createdAt(LocalDateTime.now())
                .build());

        // Authenticated as User A: summary must show 0 feedback
        mockMvc.perform(get("/api/smart-tip/feedback")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackCount", is(0)));
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("9. Strict Currency Isolation: USD query ignores EUR feedback")
    void currencyIsolation_Verified() throws Exception {
        // Seed EUR feedback for User A
        feedbackRepository.save(TipRecommendationFeedback.builder()
                .user(testUserA)
                .currency("EUR")
                .billAmount(new BigDecimal("50.00"))
                .suggestedTipPercentage(new BigDecimal("10.00"))
                .chosenTipPercentage(new BigDecimal("15.00"))
                .differencePercentagePoints(new BigDecimal("5.00"))
                .feedbackType(com.aitip.dto.SmartTipFeedbackType.MODIFIED)
                .createdAt(LocalDateTime.now())
                .build());

        // Query USD: must be 0
        mockMvc.perform(get("/api/smart-tip/feedback")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.feedbackCount", is(0)));

        // Query EUR: must be 1
        mockMvc.perform(get("/api/smart-tip/feedback")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.feedbackCount", is(1)))
                .andExpect(jsonPath("$.modifiedCount", is(1)));
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("10. Bounded adaptation reflected in GET /api/smart-tip/feedback after 5 higher decisions")
    void adaptationSummary_PrefersHigher_Verified() throws Exception {
        // Seed 5 feedback records with +3.00 pp difference
        for (int i = 0; i < 5; i++) {
            feedbackRepository.save(TipRecommendationFeedback.builder()
                    .user(testUserA)
                    .currency("USD")
                    .billAmount(new BigDecimal("50.00"))
                    .suggestedTipPercentage(new BigDecimal("15.00"))
                    .chosenTipPercentage(new BigDecimal("18.00"))
                    .differencePercentagePoints(new BigDecimal("3.00"))
                    .feedbackType(com.aitip.dto.SmartTipFeedbackType.MODIFIED)
                    .createdAt(LocalDateTime.now().minusMinutes(10 - i))
                    .build());
        }

        mockMvc.perform(get("/api/smart-tip/feedback")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackCount", is(5)))
                .andExpect(jsonPath("$.modifiedCount", is(5)))
                .andExpect(jsonPath("$.averageDifferencePercentagePoints", is(3.00)))
                .andExpect(jsonPath("$.direction", is("PREFERS_HIGHER")))
                .andExpect(jsonPath("$.adaptationApplied", is(true)))
                .andExpect(jsonPath("$.adaptationAdjustment", is(3.00)));
    }

    @Test
    @WithMockUser(username = USER_A_EMAIL)
    @DisplayName("11. POST /api/smart-tip response includes Day 32 adaptation metadata")
    void getSmartTip_IncludesDay32Fields() throws Exception {
        String payload = """
                {
                    "currency": "USD",
                    "billAmount": 50.00
                }
                """;

        mockMvc.perform(post("/api/smart-tip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackCount", is(0)))
                .andExpect(jsonPath("$.feedbackDirection", is("INSUFFICIENT_DATA")))
                .andExpect(jsonPath("$.adaptationApplied", is(false)))
                .andExpect(jsonPath("$.baselinePrimarySuggestion").exists())
                .andExpect(jsonPath("$.primarySuggestion").exists());
    }
}
