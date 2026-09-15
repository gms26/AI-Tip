package com.aitip.service;

import com.aitip.dto.SmartTipAdaptationResult;
import com.aitip.dto.SmartTipFeedbackDirection;
import com.aitip.dto.SmartTipFeedbackType;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartTipAdaptationServiceTest {

    @Mock
    private TipRecommendationFeedbackRepository feedbackRepository;

    private SmartTipAdaptationService service;
    private User testUser;

    @BeforeEach
    void setUp() {
        service = new SmartTipAdaptationService(feedbackRepository);
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@example.com");
    }

    private TipRecommendationFeedback createFeedback(String currency, Double diff, SmartTipFeedbackType type) {
        return TipRecommendationFeedback.builder()
                .user(testUser)
                .currency(currency)
                .billAmount(new BigDecimal("50.00"))
                .suggestedTipPercentage(diff != null ? new BigDecimal("15.00") : null)
                .chosenTipPercentage(diff != null ? new BigDecimal(String.valueOf(15.0 + diff)) : new BigDecimal("18.00"))
                .differencePercentagePoints(diff != null ? new BigDecimal(String.valueOf(diff)) : null)
                .feedbackType(type)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. Zero feedback: INSUFFICIENT_DATA, adaptation not applied")
    void analyze_ZeroFeedback() {
        SmartTipAdaptationResult result = service.analyze(List.of());

        assertEquals(0, result.feedbackCount());
        assertEquals(SmartTipFeedbackDirection.INSUFFICIENT_DATA, result.direction());
        assertFalse(result.adaptationApplied());
        assertNull(result.adaptationAdjustment());
        assertNull(result.averageDifferencePercentagePoints());
        assertTrue(result.adaptationMessage().contains("Not enough feedback"));
    }

    @Test
    @DisplayName("2. One feedback record: INSUFFICIENT_DATA")
    void analyze_OneFeedback() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 3.0, SmartTipFeedbackType.MODIFIED)
        );

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(1, result.feedbackCount());
        assertEquals(1, result.recommendationDecisionCount());
        assertEquals(SmartTipFeedbackDirection.INSUFFICIENT_DATA, result.direction());
        assertFalse(result.adaptationApplied());
        assertNull(result.adaptationAdjustment());
    }

    @Test
    @DisplayName("3. Four feedback records (+4.0 pp): INSUFFICIENT_DATA (strict threshold)")
    void analyze_FourFeedback_InsufficientData() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 4.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 4.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 4.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 4.0, SmartTipFeedbackType.MODIFIED)
        );

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(4, result.feedbackCount());
        assertEquals(SmartTipFeedbackDirection.INSUFFICIENT_DATA, result.direction());
        assertFalse(result.adaptationApplied());
        assertNull(result.adaptationAdjustment());
        assertEquals(new BigDecimal("4.00"), result.averageDifferencePercentagePoints());
    }

    @Test
    @DisplayName("4. Exactly five records with avg diff >= +2.0 pp: PREFERS_HIGHER")
    void analyze_FiveFeedback_PrefersHigher() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.5, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 3.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.5, SmartTipFeedbackType.MODIFIED)
        ); // Average = 2.40 pp

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(5, result.feedbackCount());
        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, result.direction());
        assertTrue(result.adaptationApplied());
        assertEquals(new BigDecimal("2.40"), result.adaptationAdjustment());
        assertEquals(new BigDecimal("2.40"), result.averageDifferencePercentagePoints());
        assertTrue(result.adaptationMessage().contains("above the suggested tip"));
    }

    @Test
    @DisplayName("5. Exactly five records with avg diff <= -2.0 pp: PREFERS_LOWER")
    void analyze_FiveFeedback_PrefersLower() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.5, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -3.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.5, SmartTipFeedbackType.MODIFIED)
        ); // Average = -2.40 pp

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(5, result.feedbackCount());
        assertEquals(SmartTipFeedbackDirection.PREFERS_LOWER, result.direction());
        assertTrue(result.adaptationApplied());
        assertEquals(new BigDecimal("-2.40"), result.adaptationAdjustment());
        assertTrue(result.adaptationMessage().contains("below the suggested tip"));
    }

    @Test
    @DisplayName("6. Five records with avg diff between -2.0 and +2.0 pp: ALIGNED")
    void analyze_FiveFeedback_Aligned() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 0.0, SmartTipFeedbackType.ACCEPTED),
                createFeedback("USD", 0.0, SmartTipFeedbackType.ACCEPTED),
                createFeedback("USD", 1.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 0.0, SmartTipFeedbackType.ACCEPTED),
                createFeedback("USD", 0.5, SmartTipFeedbackType.MODIFIED)
        ); // Average = 0.30 pp

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(5, result.feedbackCount());
        assertEquals(SmartTipFeedbackDirection.ALIGNED, result.direction());
        assertFalse(result.adaptationApplied());
        assertEquals(new BigDecimal("0.00"), result.adaptationAdjustment());
        assertTrue(result.adaptationMessage().contains("generally close"));
    }

    @Test
    @DisplayName("7. Threshold test: exactly +2.00 pp triggers adaptation")
    void analyze_Threshold_ExactlyTwoPpPositive() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED)
        );

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, result.direction());
        assertTrue(result.adaptationApplied());
        assertEquals(new BigDecimal("2.00"), result.adaptationAdjustment());
    }

    @Test
    @DisplayName("8. Threshold test: +1.99 pp does NOT trigger adaptation")
    void analyze_Threshold_JustUnderTwoPp() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 1.99, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 1.99, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 1.99, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 1.99, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 1.99, SmartTipFeedbackType.MODIFIED)
        );

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(SmartTipFeedbackDirection.ALIGNED, result.direction());
        assertFalse(result.adaptationApplied());
        assertEquals(new BigDecimal("0.00"), result.adaptationAdjustment());
    }

    @Test
    @DisplayName("9. Threshold test: exactly -2.00 pp triggers negative adaptation")
    void analyze_Threshold_ExactlyTwoPpNegative() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -2.0, SmartTipFeedbackType.MODIFIED)
        );

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(SmartTipFeedbackDirection.PREFERS_LOWER, result.direction());
        assertTrue(result.adaptationApplied());
        assertEquals(new BigDecimal("-2.00"), result.adaptationAdjustment());
    }

    @Test
    @DisplayName("10. Bounded adjustment: +4.50 pp is clamped to +3.00 pp maximum")
    void analyze_BoundedAdjustment_PositiveClamped() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 4.5, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 5.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 4.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 4.5, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 4.5, SmartTipFeedbackType.MODIFIED)
        ); // Average = 4.50 pp

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, result.direction());
        assertTrue(result.adaptationApplied());
        assertEquals(new BigDecimal("4.50"), result.averageDifferencePercentagePoints());
        assertEquals(new BigDecimal("3.00"), result.adaptationAdjustment()); // Clamped to 3.00!
    }

    @Test
    @DisplayName("11. Bounded adjustment: -5.00 pp is clamped to -3.00 pp minimum")
    void analyze_BoundedAdjustment_NegativeClamped() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", -5.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -5.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -5.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -5.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", -5.0, SmartTipFeedbackType.MODIFIED)
        );

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(SmartTipFeedbackDirection.PREFERS_LOWER, result.direction());
        assertTrue(result.adaptationApplied());
        assertEquals(new BigDecimal("-3.00"), result.adaptationAdjustment()); // Clamped to -3.00!
    }

    @Test
    @DisplayName("12. Mixed feedback: custom tips (no suggestion) do not distort difference calculation")
    void analyze_MixedWithCustomTips() {
        List<TipRecommendationFeedback> list = List.of(
                createFeedback("USD", 0.0, SmartTipFeedbackType.ACCEPTED),
                createFeedback("USD", null, SmartTipFeedbackType.CUSTOM),
                createFeedback("USD", 2.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", null, SmartTipFeedbackType.CUSTOM),
                createFeedback("USD", 0.0, SmartTipFeedbackType.ACCEPTED),
                createFeedback("USD", 3.0, SmartTipFeedbackType.MODIFIED),
                createFeedback("USD", 0.0, SmartTipFeedbackType.ACCEPTED)
        ); // 7 total, 2 custom, 5 recommendation decisions: diffs = [0, 2, 0, 3, 0] -> sum = 5, avg = 1.00 pp

        SmartTipAdaptationResult result = service.analyze(list);

        assertEquals(7, result.feedbackCount());
        assertEquals(3, result.acceptedCount());
        assertEquals(2, result.modifiedCount());
        assertEquals(2, result.customCount());
        assertEquals(5, result.recommendationDecisionCount());
        assertEquals(new BigDecimal("1.00"), result.averageDifferencePercentagePoints());
        assertEquals(SmartTipFeedbackDirection.ALIGNED, result.direction());
        assertFalse(result.adaptationApplied());
    }

    @Test
    @DisplayName("13. Scoped query: getAdaptation fetches strictly by user and normalized currency")
    void getAdaptation_ScopedIsolation() {
        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD"))
                .thenReturn(List.of());

        SmartTipAdaptationResult res = service.getAdaptation(testUser, "usd");

        verify(feedbackRepository).findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD");
        assertEquals(0, res.feedbackCount());
    }

    @Test
    @DisplayName("14. Null user or currency returns empty result safely")
    void getAdaptation_NullParameters_Safe() {
        SmartTipAdaptationResult res1 = service.getAdaptation(null, "USD");
        assertEquals(0, res1.feedbackCount());

        SmartTipAdaptationResult res2 = service.getAdaptation(testUser, null);
        assertEquals(0, res2.feedbackCount());
    }
}
