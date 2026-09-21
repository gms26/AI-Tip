package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SmartTipLearningInsightsService} (Day 35).
 *
 * Covers all 12 deterministic test cases specified in the Day 35 master prompt:
 * learning strength thresholds, preference direction, personalization effect,
 * currency isolation, JWT isolation, and Groq unavailability.
 */
@ExtendWith(MockitoExtension.class)
class SmartTipLearningInsightsServiceTest {

    @Mock private TipRecommendationFeedbackRepository feedbackRepository;
    @Mock private SmartTipAdaptationService adaptationService;
    @Mock private UserRepository userRepository;
    @Mock private AiProvider AiProvider;

    @InjectMocks
    private SmartTipLearningInsightsService insightsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");
    }

    // ==================== Helper Methods ====================

    private TipRecommendationFeedback buildFeedback(BigDecimal suggested, BigDecimal chosen,
                                                     SmartTipFeedbackType type, LocalDateTime createdAt) {
        BigDecimal diff = (suggested != null && chosen != null) ? chosen.subtract(suggested) : null;
        return TipRecommendationFeedback.builder()
                .user(testUser)
                .currency("USD")
                .billAmount(new BigDecimal("100.00"))
                .suggestedTipPercentage(suggested)
                .chosenTipPercentage(chosen)
                .feedbackType(type)
                .differencePercentagePoints(diff)
                .createdAt(createdAt)
                .build();
    }

    private List<TipRecommendationFeedback> buildFeedbackList(int count, BigDecimal suggested, BigDecimal chosen,
                                                                SmartTipFeedbackType type) {
        List<TipRecommendationFeedback> list = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        for (int i = 0; i < count; i++) {
            list.add(buildFeedback(suggested, chosen, type, base.plusDays(i)));
        }
        return list;
    }

    private SmartTipAdaptationResult emptyAdaptation() {
        return new SmartTipAdaptationResult(0, 0, 0, 0, 0, null,
                SmartTipFeedbackDirection.INSUFFICIENT_DATA, false, null,
                "Not enough feedback yet to identify a consistent preference.");
    }

    private void mockUserAndAdaptation() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(adaptationService.getAdaptation(eq(testUser), eq("USD"))).thenReturn(emptyAdaptation());
    }

    // ==================== Case 1: No Feedback ====================

    @Test
    @DisplayName("Case 1: No feedback -> INSUFFICIENT_DATA for all classifications")
    void noFeedback_shouldReturnInsufficientData() {
        mockUserAndAdaptation();
        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(Collections.emptyList());
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(Collections.emptyList());

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(0, response.totalFeedback());
        assertEquals(0, response.usableDecisionCount());
        assertEquals(LearningStrength.INSUFFICIENT_DATA, response.learningStrength());
        assertEquals(PersonalizationEffect.INSUFFICIENT_DATA, response.personalizationEffect());
        assertEquals(SmartTipFeedbackDirection.INSUFFICIENT_DATA, response.preferenceDirection());
        assertEquals("Not enough decisions yet", response.preferenceSummary());
        assertNull(response.averageDifferencePercentagePoints());
        assertTrue(response.recentDecisions().isEmpty());
    }

    // ==================== Case 2: 5 usable decisions -> EARLY_LEARNING ====================

    @Test
    @DisplayName("Case 2: 5 usable decisions -> EARLY_LEARNING")
    void fiveDecisions_shouldReturnEarlyLearning() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(5,
                new BigDecimal("15.00"), new BigDecimal("15.00"), SmartTipFeedbackType.ACCEPTED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList);
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(LearningStrength.EARLY_LEARNING, response.learningStrength());
        assertEquals(5, response.usableDecisionCount());
    }

    // ==================== Case 3: 10 usable decisions -> ESTABLISHED ====================

    @Test
    @DisplayName("Case 3: 10 usable decisions -> ESTABLISHED")
    void tenDecisions_shouldReturnEstablished() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(10,
                new BigDecimal("15.00"), new BigDecimal("15.00"), SmartTipFeedbackType.ACCEPTED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(5, 10));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(LearningStrength.ESTABLISHED, response.learningStrength());
        assertEquals(10, response.usableDecisionCount());
    }

    // ==================== Case 4: 20 usable decisions -> STRONG ====================

    @Test
    @DisplayName("Case 4: 20 usable decisions -> STRONG")
    void twentyDecisions_shouldReturnStrong() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(20,
                new BigDecimal("15.00"), new BigDecimal("15.00"), SmartTipFeedbackType.ACCEPTED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(15, 20));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(LearningStrength.STRONG, response.learningStrength());
        assertEquals(20, response.usableDecisionCount());
    }

    // ==================== Case 5: Consistently higher choices -> PREFERS_HIGHER ====================

    @Test
    @DisplayName("Case 5: Consistently higher choices -> PREFERS_HIGHER")
    void consistentlyHigher_shouldReturnPrefersHigher() {
        mockUserAndAdaptation();
        // suggested=15, chosen=18 => diff=+3
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(6,
                new BigDecimal("15.00"), new BigDecimal("18.00"), SmartTipFeedbackType.MODIFIED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(1, 6));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, response.preferenceDirection());
        assertEquals(new BigDecimal("3.00"), response.averageDifferencePercentagePoints());
        assertEquals("Often tips slightly higher than recommended", response.preferenceSummary());
    }

    // ==================== Case 6: Consistently lower choices -> PREFERS_LOWER ====================

    @Test
    @DisplayName("Case 6: Consistently lower choices -> PREFERS_LOWER")
    void consistentlyLower_shouldReturnPrefersLower() {
        mockUserAndAdaptation();
        // suggested=20, chosen=16 => diff=-4
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(6,
                new BigDecimal("20.00"), new BigDecimal("16.00"), SmartTipFeedbackType.MODIFIED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(1, 6));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(SmartTipFeedbackDirection.PREFERS_LOWER, response.preferenceDirection());
        assertEquals(new BigDecimal("-4.00"), response.averageDifferencePercentagePoints());
        assertEquals("Often tips slightly lower than recommended", response.preferenceSummary());
    }

    // ==================== Case 7: Aligned choices ====================

    @Test
    @DisplayName("Case 7: Aligned choices -> ALIGNED")
    void alignedChoices_shouldReturnAligned() {
        mockUserAndAdaptation();
        // suggested=15, chosen=15 => diff=0
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(6,
                new BigDecimal("15.00"), new BigDecimal("15.00"), SmartTipFeedbackType.ACCEPTED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(1, 6));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(SmartTipFeedbackDirection.ALIGNED, response.preferenceDirection());
        assertEquals(new BigDecimal("0.00"), response.averageDifferencePercentagePoints());
        assertEquals("Usually follows recommendations", response.preferenceSummary());
    }

    // ==================== Case 8: Improving personalization ====================

    @Test
    @DisplayName("Case 8: Improving personalization -> IMPROVING")
    void improvingPersonalization_shouldReturnImproving() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);

        // Earlier half: large deviations (diff = +5.00)
        for (int i = 0; i < 4; i++) {
            feedbackList.add(buildFeedback(new BigDecimal("15.00"), new BigDecimal("20.00"),
                    SmartTipFeedbackType.MODIFIED, base.plusDays(i)));
        }
        // Recent half: small deviations (diff = +0.50)
        for (int i = 4; i < 8; i++) {
            feedbackList.add(buildFeedback(new BigDecimal("15.00"), new BigDecimal("15.50"),
                    SmartTipFeedbackType.MODIFIED, base.plusDays(i)));
        }

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(3, 8));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(PersonalizationEffect.IMPROVING, response.personalizationEffect());
    }

    // ==================== Case 9: Diverging personalization ====================

    @Test
    @DisplayName("Case 9: Diverging personalization -> DIVERGING")
    void divergingPersonalization_shouldReturnDiverging() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);

        // Earlier half: small deviations (diff = +0.50)
        for (int i = 0; i < 4; i++) {
            feedbackList.add(buildFeedback(new BigDecimal("15.00"), new BigDecimal("15.50"),
                    SmartTipFeedbackType.MODIFIED, base.plusDays(i)));
        }
        // Recent half: large deviations (diff = +5.00)
        for (int i = 4; i < 8; i++) {
            feedbackList.add(buildFeedback(new BigDecimal("15.00"), new BigDecimal("20.00"),
                    SmartTipFeedbackType.MODIFIED, base.plusDays(i)));
        }

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(3, 8));
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(PersonalizationEffect.DIVERGING, response.personalizationEffect());
    }

    // ==================== Case 10: Currency isolation ====================

    @Test
    @DisplayName("Case 10: Currency isolation Ã¢â‚¬â€ USD query only returns USD data")
    void currencyIsolation_shouldOnlyReturnRequestedCurrency() {
        mockUserAndAdaptation();
        // Even though INR records exist, the USD query should not include them
        // because the repository queries enforce currency filtering.
        List<TipRecommendationFeedback> usdFeedback = buildFeedbackList(5,
                new BigDecimal("15.00"), new BigDecimal("15.00"), SmartTipFeedbackType.ACCEPTED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(usdFeedback);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(usdFeedback);
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(5, response.totalFeedback());

        // Verify repository was queried with exactly "USD"
        verify(feedbackRepository).findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD");
        verify(feedbackRepository, never()).findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "INR");
    }

    // ==================== Case 11: JWT isolation ====================

    @Test
    @DisplayName("Case 11: JWT isolation Ã¢â‚¬â€ different email triggers different user lookup")
    void jwtIsolation_shouldUseDifferentUsers() {
        User otherUser = new User();
        otherUser.setEmail("other@example.com");

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(adaptationService.getAdaptation(eq(otherUser), eq("USD"))).thenReturn(emptyAdaptation());
        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(otherUser, "USD"))
                .thenReturn(Collections.emptyList());
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(otherUser, "USD"))
                .thenReturn(Collections.emptyList());

        SmartTipLearningInsightsResponse response = insightsService.getInsights("other@example.com", "USD");

        assertEquals(0, response.totalFeedback());
        // Verify the correct user entity was used
        verify(feedbackRepository).findAllByUserAndCurrencyOrderByCreatedAtAsc(otherUser, "USD");
        verify(feedbackRepository, never()).findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD");
    }

    // ==================== Case 12: Groq unavailable ====================

    @Test
    @DisplayName("Case 12: Groq unavailable Ã¢â‚¬â€ endpoint still succeeds with null aiExplanation")
    void groqUnavailable_shouldStillSucceed() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = buildFeedbackList(6,
                new BigDecimal("15.00"), new BigDecimal("18.00"), SmartTipFeedbackType.MODIFIED);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList.subList(1, 6));
        when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Groq API unavailable"));

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        // Core insights still work despite Groq failure
        assertEquals(6, response.totalFeedback());
        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, response.preferenceDirection());
        assertNull(response.aiExplanation());
    }

    // ==================== Additional: Feedback counts ====================

    @Test
    @DisplayName("Feedback counts should correctly tally ACCEPTED, MODIFIED, CUSTOM")
    void feedbackCounts_shouldBeCorrect() {
        mockUserAndAdaptation();
        List<TipRecommendationFeedback> feedbackList = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);

        // 3 ACCEPTED
        for (int i = 0; i < 3; i++) {
            feedbackList.add(buildFeedback(new BigDecimal("15.00"), new BigDecimal("15.00"),
                    SmartTipFeedbackType.ACCEPTED, base.plusDays(i)));
        }
        // 2 MODIFIED
        for (int i = 3; i < 5; i++) {
            feedbackList.add(buildFeedback(new BigDecimal("15.00"), new BigDecimal("18.00"),
                    SmartTipFeedbackType.MODIFIED, base.plusDays(i)));
        }
        // 1 CUSTOM (null suggested, null diff)
        TipRecommendationFeedback customFeedback = TipRecommendationFeedback.builder()
                .user(testUser).currency("USD").billAmount(new BigDecimal("100.00"))
                .suggestedTipPercentage(null).chosenTipPercentage(new BigDecimal("20.00"))
                .feedbackType(SmartTipFeedbackType.CUSTOM).differencePercentagePoints(null)
                .createdAt(base.plusDays(5)).build();
        feedbackList.add(customFeedback);

        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(feedbackList);
        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                .thenReturn(feedbackList);
        lenient().when(AiProvider.generateLearningInsightsExplanation(
                anyInt(), any(), any(), any(), any())).thenReturn("AI summary");

        SmartTipLearningInsightsResponse response = insightsService.getInsights("test@example.com", "USD");

        assertEquals(6, response.totalFeedback());
        assertEquals(3, response.acceptedCount());
        assertEquals(2, response.modifiedCount());
        assertEquals(1, response.customCount());
        // Only 5 records have non-null differencePercentagePoints
        assertEquals(5, response.usableDecisionCount());
    }

    // ==================== Additional: LearningStrength boundary tests ====================

    @Test
    @DisplayName("LearningStrength.fromCount boundary: 4 -> INSUFFICIENT_DATA, 5 -> EARLY_LEARNING")
    void learningStrengthBoundary_fourVsFive() {
        assertEquals(LearningStrength.INSUFFICIENT_DATA, LearningStrength.fromCount(4));
        assertEquals(LearningStrength.EARLY_LEARNING, LearningStrength.fromCount(5));
    }

    @Test
    @DisplayName("LearningStrength.fromCount boundary: 9 -> EARLY_LEARNING, 10 -> ESTABLISHED")
    void learningStrengthBoundary_nineVsTen() {
        assertEquals(LearningStrength.EARLY_LEARNING, LearningStrength.fromCount(9));
        assertEquals(LearningStrength.ESTABLISHED, LearningStrength.fromCount(10));
    }

    @Test
    @DisplayName("LearningStrength.fromCount boundary: 19 -> ESTABLISHED, 20 -> STRONG")
    void learningStrengthBoundary_nineteenVsTwenty() {
        assertEquals(LearningStrength.ESTABLISHED, LearningStrength.fromCount(19));
        assertEquals(LearningStrength.STRONG, LearningStrength.fromCount(20));
    }

    // ==================== Additional: Personalization Effect edge case ====================

    @Test
    @DisplayName("PersonalizationEffect with fewer than 4 usable decisions -> INSUFFICIENT_DATA")
    void personalizationEffect_tooFewDecisions() {
        List<TipRecommendationFeedback> shortList = buildFeedbackList(3,
                new BigDecimal("15.00"), new BigDecimal("18.00"), SmartTipFeedbackType.MODIFIED);

        PersonalizationEffect effect = insightsService.determinePersonalizationEffect(shortList);

        assertEquals(PersonalizationEffect.INSUFFICIENT_DATA, effect);
    }

    @Test
    @DisplayName("PersonalizationEffect STABLE when deviations are similar in both halves")
    void personalizationEffect_stableWhenSimilar() {
        // All same deviation (+1.00)
        List<TipRecommendationFeedback> list = buildFeedbackList(8,
                new BigDecimal("15.00"), new BigDecimal("16.00"), SmartTipFeedbackType.MODIFIED);

        PersonalizationEffect effect = insightsService.determinePersonalizationEffect(list);

        assertEquals(PersonalizationEffect.STABLE, effect);
    }
}
