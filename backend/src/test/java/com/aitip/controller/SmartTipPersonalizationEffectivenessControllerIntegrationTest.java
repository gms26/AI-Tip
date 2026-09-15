package com.aitip.controller;

import com.aitip.dto.SmartTipFeedbackType;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SmartTipPersonalizationEffectivenessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRecommendationFeedbackRepository feedbackRepository;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("effectiveness@example.com")
                .password("password")
                .name("Effectiveness User")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other_eff@example.com")
                .password("password")
                .name("Other User")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "effectiveness@example.com")
    @DisplayName("GET effectiveness - empty state")
    void getEffectiveness_emptyState() throws Exception {
        mockMvc.perform(get("/api/smart-tip/personalization/effectiveness")
                        .param("currency", "USD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.totalFeedback").value(0))
                .andExpect(jsonPath("$.effectiveness").value("INSUFFICIENT_DATA"));
    }

    @Test
    @WithMockUser(username = "effectiveness@example.com")
    @DisplayName("GET effectiveness - populated state")
    void getEffectiveness_populatedState() throws Exception {
        for (int i = 0; i < 10; i++) {
            TipRecommendationFeedback fb = TipRecommendationFeedback.builder()
                    .user(testUser).currency("USD")
                    .billAmount(new BigDecimal("50.00"))
                    .chosenTipPercentage(new BigDecimal("15.00"))
                    .differencePercentagePoints(BigDecimal.ZERO)
                    .feedbackType(SmartTipFeedbackType.ACCEPTED)
                    .createdAt(LocalDateTime.now()).build();
            feedbackRepository.save(fb);
        }

        mockMvc.perform(get("/api/smart-tip/personalization/effectiveness")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedback").value(10))
                .andExpect(jsonPath("$.effectiveness").value("STRONG_ALIGNMENT"))
                .andExpect(jsonPath("$.calibrationStatus").value("WELL_CALIBRATED"))
                .andExpect(jsonPath("$.alignmentRate").value(100.0));
    }

    @Test
    @WithMockUser(username = "effectiveness@example.com")
    @DisplayName("GET effectiveness - currency isolation")
    void getEffectiveness_currencyIsolation() throws Exception {
        TipRecommendationFeedback fb = TipRecommendationFeedback.builder()
                .user(testUser).currency("EUR")
                .billAmount(new BigDecimal("50.00"))
                .chosenTipPercentage(new BigDecimal("15.00"))
                .differencePercentagePoints(BigDecimal.ZERO)
                .feedbackType(SmartTipFeedbackType.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();
        feedbackRepository.save(fb);

        mockMvc.perform(get("/api/smart-tip/personalization/effectiveness")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedback").value(0));
    }

    @Test
    @WithMockUser(username = "other_eff@example.com")
    @DisplayName("GET effectiveness - user isolation")
    void getEffectiveness_userIsolation() throws Exception {
        TipRecommendationFeedback fb = TipRecommendationFeedback.builder()
                .user(testUser).currency("USD")
                .billAmount(new BigDecimal("50.00"))
                .chosenTipPercentage(new BigDecimal("15.00"))
                .differencePercentagePoints(BigDecimal.ZERO)
                .feedbackType(SmartTipFeedbackType.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();
        feedbackRepository.save(fb);

        mockMvc.perform(get("/api/smart-tip/personalization/effectiveness")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedback").value(0));
    }
}
