package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartTipDecisionMemoryServiceTest {

    @Mock
    private TipRecommendationFeedbackRepository feedbackRepository;
    @Mock
    private SmartTipAdaptationService adaptationService;
    @Mock
    private UserService userService;

    @InjectMocks
    private SmartTipDecisionMemoryService decisionMemoryService;

    private User testUser;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String CURRENCY = "USD";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(TEST_EMAIL);
    }

    @Test
    void getDecisionMemory_success() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        
        SmartTipAdaptationResult adaptation = new SmartTipAdaptationResult(
                10L, 5L, 3L, 2L, 5L, new BigDecimal("1.50"),
                SmartTipFeedbackDirection.PREFERS_HIGHER, true, new BigDecimal("1.00"), "Adapted"
        );
        when(adaptationService.getAdaptation(testUser, CURRENCY)).thenReturn(adaptation);

        when(feedbackRepository.findMinChosenPercentageByUserAndCurrency(testUser, CURRENCY))
                .thenReturn(new BigDecimal("15.00"));
        when(feedbackRepository.findMaxChosenPercentageByUserAndCurrency(testUser, CURRENCY))
                .thenReturn(new BigDecimal("25.00"));
        when(feedbackRepository.findAverageChosenPercentageByUserAndCurrency(testUser, CURRENCY))
                .thenReturn(new BigDecimal("18.50"));

        TipRecommendationFeedback f1 = new TipRecommendationFeedback();
        f1.setChosenTipPercentage(new BigDecimal("15.00"));
        TipRecommendationFeedback f2 = new TipRecommendationFeedback();
        f2.setChosenTipPercentage(new BigDecimal("20.00"));
        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, CURRENCY))
                .thenReturn(List.of(f1, f2));

        TipRecommendationFeedback fRecent = new TipRecommendationFeedback();
        fRecent.setCreatedAt(LocalDateTime.now());
        fRecent.setCurrency(CURRENCY);
        fRecent.setRestaurantName("Test Rest");
        fRecent.setSuggestedTipPercentage(new BigDecimal("18.00"));
        fRecent.setChosenTipPercentage(new BigDecimal("20.00"));
        fRecent.setDifferencePercentagePoints(new BigDecimal("2.00"));
        fRecent.setFeedbackType(SmartTipFeedbackType.MODIFIED);

        when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, CURRENCY))
                .thenReturn(List.of(fRecent));

        SmartTipDecisionMemoryResponse response = decisionMemoryService.getDecisionMemory(TEST_EMAIL, CURRENCY);

        assertThat(response.currency()).isEqualTo(CURRENCY);
        assertThat(response.totalFeedback()).isEqualTo(10);
        assertThat(response.confidence()).isEqualTo("HIGH");
        assertThat(response.commonChosenRange().min()).isEqualTo(new BigDecimal("15.00"));
        assertThat(response.commonChosenRange().max()).isEqualTo(new BigDecimal("25.00"));
        assertThat(response.commonChosenRange().average()).isEqualTo(new BigDecimal("18.50"));
        assertThat(response.commonChosenRange().median()).isEqualTo(new BigDecimal("17.50"));
        assertThat(response.recentDecisions()).hasSize(1);
        assertThat(response.recentDecisions().get(0).restaurantName()).isEqualTo("Test Rest");
    }

    @Test
    void buildDecisionExplanation_allFactors() {
        SmartTipResponse response = new SmartTipResponse(
                CURRENCY, new BigDecimal("100.00"),
                new BigDecimal("18.00"), new BigDecimal("18.50"),
                new BigDecimal("15.00"), new BigDecimal("20.00"),
                TipBudgetStatus.UNDER_BUDGET, new BigDecimal("45.00"), true,
                TipEvolutionDirection.MORE_GENEROUS,
                5, new BigDecimal("19.00"), new BigDecimal("19.50"),
                3, new BigDecimal("20.00"),
                List.of(), null, "Test", null,
                0L, SmartTipFeedbackDirection.INSUFFICIENT_DATA, null, false, null, null, null, null, true
        );

        SmartTipAdaptationResult adaptation = new SmartTipAdaptationResult(
                10L, 5L, 3L, 2L, 5L, new BigDecimal("2.50"),
                SmartTipFeedbackDirection.PREFERS_HIGHER, true, new BigDecimal("1.50"), "Adapted"
        );

        SmartTipDecisionExplanation explanation = decisionMemoryService.buildDecisionExplanation(response, adaptation, true);

        assertThat(explanation).isNotNull();
        assertThat(explanation.confidence()).isEqualTo("HIGH");
        assertThat(explanation.adaptationApplied()).isTrue();
        assertThat(explanation.adaptationAdjustment()).isEqualTo(new BigDecimal("1.50"));
        // Check ordered factors
        assertThat(explanation.factors()).hasSize(7);
        assertThat(explanation.factors().get(0).type()).isEqualTo(SmartTipDecisionFactorType.USER_FEEDBACK);
        assertThat(explanation.factors().get(1).type()).isEqualTo(SmartTipDecisionFactorType.RESTAURANT_HISTORY);
        assertThat(explanation.factors().get(2).type()).isEqualTo(SmartTipDecisionFactorType.OPTIMIZATION_RANGE);
        assertThat(explanation.factors().get(3).type()).isEqualTo(SmartTipDecisionFactorType.HISTORICAL_BEHAVIOR);
        assertThat(explanation.factors().get(4).type()).isEqualTo(SmartTipDecisionFactorType.RECENT_TREND);
        assertThat(explanation.factors().get(5).type()).isEqualTo(SmartTipDecisionFactorType.BUDGET);
        assertThat(explanation.factors().get(6).type()).isEqualTo(SmartTipDecisionFactorType.SERVICE_QUALITY);
    }
}
