package com.aitip.controller;

import com.aitip.dto.*;
import com.aitip.service.SmartTipLearningInsightsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link SmartTipLearningInsightsController} (Day 35).
 *
 * Tests JWT auth enforcement, currency parameter, response structure, user isolation,
 * and empty-state handling through MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmartTipLearningInsightsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SmartTipLearningInsightsService insightsService;

    private SmartTipLearningInsightsResponse buildEmptyResponse() {
        return new SmartTipLearningInsightsResponse(
                0, 0, 0, 0, 0, null,
                SmartTipFeedbackDirection.INSUFFICIENT_DATA,
                LearningStrength.INSUFFICIENT_DATA,
                PersonalizationEffect.INSUFFICIENT_DATA,
                "Not enough decisions yet",
                false, null,
                Collections.emptyList(), null, true
        );
    }

    private SmartTipLearningInsightsResponse buildPopulatedResponse() {
        RecentDecisionDto recent = new RecentDecisionDto(
                LocalDateTime.of(2026, 6, 15, 14, 30),
                "Test Restaurant",
                ServiceQuality.GOOD,
                new BigDecimal("15.00"),
                new BigDecimal("18.00"),
                new BigDecimal("3.00"),
                SmartTipFeedbackType.MODIFIED,
                "SMART"
        );
        return new SmartTipLearningInsightsResponse(
                14, 8, 4, 2, 12,
                new BigDecimal("2.40"),
                SmartTipFeedbackDirection.PREFERS_HIGHER,
                LearningStrength.ESTABLISHED,
                PersonalizationEffect.IMPROVING,
                "Often tips slightly higher than recommended",
                true, new BigDecimal("2.40"),
                List.of(recent),
                "You tend to tip a bit higher than suggested, and your choices are becoming more aligned.",
                true
        );
    }

    // ==================== Authentication Tests ====================

    @Test
    @DisplayName("GET /api/smart-tip/learning-insights without auth returns 401")
    void noAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Empty State ====================

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Empty state returns valid response with INSUFFICIENT_DATA")
    void emptyState_shouldReturnValidResponse() throws Exception {
        when(insightsService.getInsights(eq("test@example.com"), eq("USD")))
                .thenReturn(buildEmptyResponse());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedback").value(0))
                .andExpect(jsonPath("$.learningStrength").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.personalizationEffect").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.preferenceDirection").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.preferenceSummary").value("Not enough decisions yet"))
                .andExpect(jsonPath("$.recentDecisions").isEmpty())
                .andExpect(jsonPath("$.aiExplanation").doesNotExist());
    }

    // ==================== Populated State ====================

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Populated state returns complete response with all fields")
    void populatedState_shouldReturnFullResponse() throws Exception {
        when(insightsService.getInsights(eq("test@example.com"), eq("USD")))
                .thenReturn(buildPopulatedResponse());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedback").value(14))
                .andExpect(jsonPath("$.acceptedCount").value(8))
                .andExpect(jsonPath("$.modifiedCount").value(4))
                .andExpect(jsonPath("$.customCount").value(2))
                .andExpect(jsonPath("$.usableDecisionCount").value(12))
                .andExpect(jsonPath("$.averageDifferencePercentagePoints").value(2.40))
                .andExpect(jsonPath("$.preferenceDirection").value("PREFERS_HIGHER"))
                .andExpect(jsonPath("$.learningStrength").value("ESTABLISHED"))
                .andExpect(jsonPath("$.personalizationEffect").value("IMPROVING"))
                .andExpect(jsonPath("$.preferenceSummary").value("Often tips slightly higher than recommended"))
                .andExpect(jsonPath("$.adaptationApplied").value(true))
                .andExpect(jsonPath("$.recentDecisions").isArray())
                .andExpect(jsonPath("$.recentDecisions[0].restaurantName").value("Test Restaurant"))
                .andExpect(jsonPath("$.aiExplanation").exists());
    }

    // ==================== Missing currency parameter ====================

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Missing currency parameter returns error status")
    void missingCurrency_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/smart-tip/learning-insights"))
                .andExpect(status().is5xxServerError());
    }

    // ==================== User Isolation ====================

    @Test
    @WithMockUser(username = "userA@example.com")
    @DisplayName("User A queries learning insights — service called with User A's email")
    void userIsolation_shouldCallServiceWithCorrectEmail() throws Exception {
        when(insightsService.getInsights(eq("userA@example.com"), eq("USD")))
                .thenReturn(buildEmptyResponse());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isOk());

        verify(insightsService).getInsights("userA@example.com", "USD");
        verify(insightsService, never()).getInsights(eq("userB@example.com"), any());
    }

    @Test
    @WithMockUser(username = "userB@example.com")
    @DisplayName("User B queries learning insights — service called with User B's email, never User A's")
    void userIsolation_userB_shouldNotSeeUserA() throws Exception {
        when(insightsService.getInsights(eq("userB@example.com"), eq("USD")))
                .thenReturn(buildEmptyResponse());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isOk());

        verify(insightsService).getInsights("userB@example.com", "USD");
        verify(insightsService, never()).getInsights(eq("userA@example.com"), any());
    }

    // ==================== Currency Isolation via controller ====================

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("USD and INR requests invoke service with different currencies")
    void currencyIsolation_shouldPassCorrectCurrency() throws Exception {
        when(insightsService.getInsights(eq("test@example.com"), eq("USD")))
                .thenReturn(buildEmptyResponse());
        when(insightsService.getInsights(eq("test@example.com"), eq("INR")))
                .thenReturn(buildEmptyResponse());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "INR"))
                .andExpect(status().isOk());

        verify(insightsService).getInsights("test@example.com", "USD");
        verify(insightsService).getInsights("test@example.com", "INR");
    }

    // ==================== Response structure includes recent decisions ====================

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("Recent decisions include all expected fields")
    void recentDecisions_shouldHaveCorrectFields() throws Exception {
        when(insightsService.getInsights(eq("test@example.com"), eq("USD")))
                .thenReturn(buildPopulatedResponse());

        mockMvc.perform(get("/api/smart-tip/learning-insights")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentDecisions[0].createdAt").exists())
                .andExpect(jsonPath("$.recentDecisions[0].suggestedTipPercentage").value(15.00))
                .andExpect(jsonPath("$.recentDecisions[0].chosenTipPercentage").value(18.00))
                .andExpect(jsonPath("$.recentDecisions[0].differencePercentagePoints").value(3.00))
                .andExpect(jsonPath("$.recentDecisions[0].feedbackType").value("MODIFIED"))
                .andExpect(jsonPath("$.recentDecisions[0].serviceQuality").value("GOOD"));
    }
}
