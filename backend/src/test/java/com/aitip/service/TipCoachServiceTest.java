package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.TipGoalType;
import com.aitip.entity.User;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipCoachServiceTest {

    @Mock
    private TipBudgetService tipBudgetService;
    @Mock
    private TipForecastCalculationService tipForecastService;
    @Mock
    private TipGoalService tipGoalService;
    @Mock
    private TipDataQualityService tipDataQualityService;
    @Mock
    private TipProfileService tipProfileService;
    @Mock
    private TipOptimizationService tipOptimizationService;
    @Mock
    private TipRecommendationService tipRecommendationService;
    @Mock
    private UserService userService;
    @Mock
    private TipCoachPromptBuilder promptBuilder;
    @Mock
    private AiProvider AiProvider;

    @InjectMocks
    private TipCoachService tipCoachService;

    private final String email = "test@example.com";
    private final String currency = "USD";
    private final User testUser = new User();

    @BeforeEach
    void setUp() {
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(email);
        lenient().when(userService.getUserByEmail(email)).thenReturn(testUser);

        // Set up default empty/safe responses for all mocks
        lenient().when(tipBudgetService.getBudgetStatus(anyString(), anyString())).thenReturn(null);
        lenient().when(tipForecastService.forecast(anyString(), anyString(), any(), isNull(), isNull())).thenReturn(null);
        lenient().when(tipGoalService.getGoals(anyString())).thenReturn(new ArrayList<>());
        lenient().when(tipDataQualityService.analyzeDataQuality(any(), any(), isNull(), isNull())).thenReturn(null);
        lenient().when(tipProfileService.getProfile(anyString(), anyString())).thenReturn(null);
        lenient().when(tipOptimizationService.optimize(anyString(), any())).thenReturn(null);
        lenient().when(tipRecommendationService.getRecommendations(anyString(), anyString())).thenReturn(null);
        lenient().when(promptBuilder.buildPrompt(any())).thenReturn("prompt");
        lenient().when(AiProvider.getRecommendation(anyString())).thenReturn("AI Explanation");
    }

    @Test
    void testGetCoaching_NoData_ReturnsNoAction() {
        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.NO_ACTION, response.getFocus());
        assertEquals(9, response.getFocusPriority());
        assertEquals("Keep building your tipping history.", response.getHeadline());
    }

    @Test
    void testGetCoaching_FiveTipsNoIssues_ReturnsPositiveProgress() {
        TipProfileResponse profile = new TipProfileResponse(
                LocalDateTime.now(), 5, new ArrayList<>(), TipBehaviorType.CONSISTENT, TipStyle.MODERATE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null,
                new ArrayList<>(), new ArrayList<>(), "", null
        );
        when(tipProfileService.getProfile(email, currency)).thenReturn(profile);

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.POSITIVE_PROGRESS, response.getFocus());
        assertEquals(8, response.getFocusPriority());
    }

    @Test
    void testGetCoaching_BudgetPriority() {
        when(tipBudgetService.getBudgetStatus(email, currency)).thenReturn(
                new TipBudgetStatusResponse(currency, BigDecimal.valueOf(100), BigDecimal.valueOf(120), BigDecimal.valueOf(120), BigDecimal.ZERO, BigDecimal.valueOf(120), TipBudgetStatus.OVER_BUDGET, 10, BudgetConfidence.HIGH, "")
        );

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.BUDGET, response.getFocus());
        assertEquals(1, response.getFocusPriority());
    }

    @Test
    void testGetCoaching_BudgetApproachingLimit_OverridesForecast() {
        when(tipBudgetService.getBudgetStatus(email, currency)).thenReturn(
                new TipBudgetStatusResponse(currency, BigDecimal.valueOf(100), BigDecimal.valueOf(90), BigDecimal.valueOf(90), BigDecimal.valueOf(10), BigDecimal.valueOf(90), TipBudgetStatus.APPROACHING_LIMIT, 10, BudgetConfidence.HIGH, "")
        );

        when(tipForecastService.forecast(eq(email), eq(currency), eq(TipForecastPeriod.CURRENT_MONTH), isNull(), isNull())).thenReturn(
                new TipForecastResponse(currency, TipForecastPeriod.CURRENT_MONTH, 30, 15, 15, BigDecimal.valueOf(120), BigDecimal.valueOf(50), BigDecimal.valueOf(70), BigDecimal.TEN, 10, BigDecimal.valueOf(120), BudgetConfidence.HIGH, BigDecimal.valueOf(100), BigDecimal.valueOf(20), TipBudgetStatus.OVER_BUDGET, "")
        );

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.BUDGET, response.getFocus());
    }

    @Test
    void testGetCoaching_ForecastPriority_WhenNoImmediateBudgetIssue() {
        when(tipBudgetService.getBudgetStatus(email, currency)).thenReturn(
                new TipBudgetStatusResponse(currency, BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(50), BigDecimal.valueOf(50), BigDecimal.valueOf(50), TipBudgetStatus.UNDER_BUDGET, 10, BudgetConfidence.HIGH, "")
        );

        when(tipForecastService.forecast(eq(email), eq(currency), eq(TipForecastPeriod.CURRENT_MONTH), isNull(), isNull())).thenReturn(
                new TipForecastResponse(currency, TipForecastPeriod.CURRENT_MONTH, 30, 15, 15, BigDecimal.valueOf(120), BigDecimal.valueOf(50), BigDecimal.valueOf(70), BigDecimal.TEN, 10, BigDecimal.valueOf(120), BudgetConfidence.HIGH, BigDecimal.valueOf(100), BigDecimal.valueOf(20), TipBudgetStatus.OVER_BUDGET, "")
        );

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.FORECAST, response.getFocus());
    }

    @Test
    void testGetCoaching_GoalPriority_80Percent() {
        TipGoalProgressResponse goal = new TipGoalProgressResponse(
                UUID.randomUUID(), TipGoalType.TOTAL_TIP_AMOUNT, BigDecimal.valueOf(100), BigDecimal.valueOf(85), 
                BigDecimal.valueOf(85), BigDecimal.valueOf(15), TipGoalStatus.ACTIVE, 10, "HIGH", "",
                currency, null, null, null, null, null
        );
        when(tipGoalService.getGoals(email)).thenReturn(List.of(goal));

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.GOAL, response.getFocus());
    }

    @Test
    void testGetCoaching_DataQualityPriority() {
        when(tipDataQualityService.analyzeDataQuality(any(), any(), isNull(), isNull())).thenReturn(
                new TipDataQualityResponse(10, 8, 2, 1, 1, 0, new ArrayList<>(), Collections.singleton(currency), "")
        );

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.DATA_QUALITY, response.getFocus());
    }

    @Test
    void testGetCoaching_ConsistencyPriority() {
        TipProfileResponse profile = new TipProfileResponse(
                LocalDateTime.now(), 20, new ArrayList<>(), TipBehaviorType.HIGHLY_VARIABLE, TipStyle.MODERATE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 30, BigDecimal.TEN, null,
                new ArrayList<>(), new ArrayList<>(), "", null
        );
        when(tipProfileService.getProfile(email, currency)).thenReturn(profile);

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.CONSISTENCY, response.getFocus());
    }

    @Test
    void testGetCoaching_OptimizationPriority() {
        TipProfileResponse profile = new TipProfileResponse(
                LocalDateTime.now(), 20, new ArrayList<>(), TipBehaviorType.CONSISTENT, TipStyle.MODERATE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 90, BigDecimal.TEN, null,
                new ArrayList<>(), new ArrayList<>(), "", null
        );
        when(tipProfileService.getProfile(email, currency)).thenReturn(profile);
        
        TipOptimizationResponse optimization = new TipOptimizationResponse(
                currency, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, 20, BudgetConfidence.HIGH,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.valueOf(5), ""
        );
        when(tipOptimizationService.optimize(eq(email), any())).thenReturn(optimization);

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.TIP_OPTIMIZATION, response.getFocus());
    }

    @Test
    void testGetCoaching_ExplorationPriority() {
        List<RestaurantPattern> restaurants = List.of(
                new RestaurantPattern("A", 8, BigDecimal.TEN, BigDecimal.TEN),
                new RestaurantPattern("B", 2, BigDecimal.TEN, BigDecimal.TEN)
        );
        TipProfileResponse profile = new TipProfileResponse(
                LocalDateTime.now(), 10, new ArrayList<>(), TipBehaviorType.CONSISTENT, TipStyle.MODERATE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 90, BigDecimal.TEN, null,
                restaurants, new ArrayList<>(), "", null
        );
        when(tipProfileService.getProfile(email, currency)).thenReturn(profile);

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertEquals(TipCoachFocus.EXPLORATION, response.getFocus());
    }

    @Test
    void testGetCoaching_GroqFailure_Graceful() {
        when(AiProvider.getRecommendation(anyString())).thenThrow(new RuntimeException("API Error"));

        TipCoachResponse response = tipCoachService.getCoaching(email, currency);
        
        assertNull(response.getAiExplanation());
        assertEquals(TipCoachFocus.NO_ACTION, response.getFocus());
    }
}
