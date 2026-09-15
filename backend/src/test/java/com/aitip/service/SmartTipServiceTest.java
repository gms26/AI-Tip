package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.dto.ServiceQuality;
import com.aitip.entity.Tip;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.TipGoalType;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SmartTipServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @Mock
    private TipOptimizationService tipOptimizationService;

    @Mock
    private TipBudgetService tipBudgetService;

    @Mock
    private TipGoalService tipGoalService;

    @Mock
    private TipEvolutionService tipEvolutionService;

    @Mock
    private SmartTipAdaptationService smartTipAdaptationService;

    @Mock
    private SmartTipDecisionMemoryService smartTipDecisionMemoryService;

    private Clock fixedClock;
    private SmartTipService smartTipService;
    private User testUser;
    private UUID testUserId;

    private static final String TEST_EMAIL = "smarttip@example.com";
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 6, 15, 12, 0, 0);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        smartTipService = new SmartTipService(
                tipRepository,
                userService,
                tipOptimizationService,
                tipBudgetService,
                tipGoalService,
                tipEvolutionService,
                smartTipAdaptationService,
                smartTipDecisionMemoryService,
                null, // smartTipPersonalizationService
                fixedClock
        );

        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail(TEST_EMAIL);

        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
    }

    private Tip createTip(String currency, double amount, double percentage, String restaurant, ServiceQuality sq) {
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setUser(testUser);
        tip.setCurrency(currency);
        tip.setTipAmount(BigDecimal.valueOf(amount));
        tip.setTipPercentage(BigDecimal.valueOf(percentage));
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(FIXED_NOW.minusDays(5));
        return tip;
    }

    // =========================================================================
    // 1. VALIDATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Validation: Null request throws IllegalArgumentException")
    void testNullRequestThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> smartTipService.getSmartTip(TEST_EMAIL, null));
    }

    @Test
    @DisplayName("Validation: Null bill amount throws IllegalArgumentException")
    void testNullBillAmountThrowsException() {
        SmartTipRequest request = new SmartTipRequest("USD", null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> smartTipService.getSmartTip(TEST_EMAIL, request));
    }

    @Test
    @DisplayName("Validation: Bill amount <= 0.01 throws IllegalArgumentException (boundary: 0.01)")
    void testBillAmountAtOrBelowMinThrowsException() {
        SmartTipRequest request001 = new SmartTipRequest("USD", new BigDecimal("0.01"), null, null, null);
        assertThrows(IllegalArgumentException.class, () -> smartTipService.getSmartTip(TEST_EMAIL, request001));

        SmartTipRequest request0 = new SmartTipRequest("USD", BigDecimal.ZERO, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> smartTipService.getSmartTip(TEST_EMAIL, request0));

        SmartTipRequest requestNeg = new SmartTipRequest("USD", new BigDecimal("-10.00"), null, null, null);
        assertThrows(IllegalArgumentException.class, () -> smartTipService.getSmartTip(TEST_EMAIL, requestNeg));
    }

    @Test
    @DisplayName("Validation: Bill amount 0.02 is valid (boundary: > 0.01)")
    void testBillAmountValidBoundary() {
        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("0.02"), null, null, null);
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(Collections.emptyList());

        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);
        assertNotNull(response);
        assertEquals(new BigDecimal("0.02"), response.billAmount());
    }

    @Test
    @DisplayName("Validation: Invalid currency throws IllegalArgumentException")
    void testInvalidCurrencyThrowsException() {
        SmartTipRequest request = new SmartTipRequest("INVALID_CURR", new BigDecimal("50.00"), null, null, null);
        assertThrows(IllegalArgumentException.class, () -> smartTipService.getSmartTip(TEST_EMAIL, request));
    }

    @Test
    @DisplayName("Validation: Currency is normalized to uppercase")
    void testCurrencyNormalization() {
        SmartTipRequest request = new SmartTipRequest("usd", new BigDecimal("50.00"), null, null, null);
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(Collections.emptyList());

        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);
        assertEquals("USD", response.currency());
    }

    // =========================================================================
    // 2. NO HISTORY / FIRST-TIME USER
    // =========================================================================

    @Test
    @DisplayName("No History: Returns standard neutral suggestions when user has no tips")
    void testNoHistoryReturnsStandardSuggestions() {
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(Collections.emptyList());

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("100.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertNotNull(response);
        assertEquals("USD", response.currency());
        assertEquals(new BigDecimal("100.00"), response.billAmount());
        assertNull(response.historicalMedianTipPercentage());
        assertNull(response.historicalAverageTipPercentage());
        assertNull(response.optimizedMinimumPercentage());
        assertNull(response.optimizedMaximumPercentage());
        assertNull(response.restaurantTipCount());
        assertNull(response.serviceQualityTipCount());
        assertEquals(TipEvolutionDirection.INSUFFICIENT_DATA, response.recentDirection());
        assertTrue(response.message().contains("General tip options"));

        // Exactly 4 suggestions
        assertEquals(4, response.suggestions().size());
        assertEquals(new BigDecimal("10.00"), response.suggestions().get(0).tipPercentage());
        assertEquals(new BigDecimal("15.00"), response.suggestions().get(1).tipPercentage());
        assertEquals(new BigDecimal("18.00"), response.suggestions().get(2).tipPercentage());
        assertEquals(new BigDecimal("20.00"), response.suggestions().get(3).tipPercentage());

        // Exactly one primary suggestion marked recommended
        assertNotNull(response.primarySuggestion());
        assertTrue(response.primarySuggestion().isRecommended());
        long recommendedCount = response.suggestions().stream().filter(s -> Boolean.TRUE.equals(s.isRecommended())).count();
        assertEquals(1, recommendedCount);
    }

    // =========================================================================
    // 3. HISTORICAL BASELINE
    // =========================================================================

    @Test
    @DisplayName("Historical Baseline: Correctly computes median and average from user history")
    void testHistoricalBaselineComputedCorrectly() {
        List<Tip> tips = List.of(
                createTip("USD", 10.00, 10.00, "Cafe A", ServiceQuality.AVERAGE),
                createTip("USD", 15.00, 15.00, "Cafe B", ServiceQuality.GOOD),
                createTip("USD", 18.00, 18.00, "Cafe C", ServiceQuality.GOOD),
                createTip("USD", 20.00, 20.00, "Cafe D", ServiceQuality.EXCELLENT),
                createTip("USD", 22.00, 22.00, "Cafe E", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        // Median of [10, 15, 18, 20, 22] = 18.00; Average = 17.00
        assertEquals(new BigDecimal("18.00"), response.historicalMedianTipPercentage());
        assertEquals(new BigDecimal("17.00"), response.historicalAverageTipPercentage());

        // Suggestions contain HISTORICAL_TYPICAL with 18.00%
        Optional<SmartTipSuggestion> typicalOpt = response.suggestions().stream()
                .filter(s -> s.type() == SmartTipSuggestionType.HISTORICAL_TYPICAL)
                .findFirst();
        assertTrue(typicalOpt.isPresent());
        assertEquals(new BigDecimal("18.00"), typicalOpt.get().tipPercentage());
        assertEquals(new BigDecimal("9.00"), typicalOpt.get().tipAmount()); // 50 * 0.18
        assertEquals(new BigDecimal("59.00"), typicalOpt.get().totalAmount());
    }

    // =========================================================================
    // 4. RESTAURANT CONTEXT MATCHING
    // =========================================================================

    @Test
    @DisplayName("Restaurant Context: Matches known restaurant and calculates restaurant stats")
    void testRestaurantContextMatched() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "Pizzeria Bella", ServiceQuality.GOOD),
                createTip("USD", 16.00, 16.00, "Pizzeria Bella", ServiceQuality.GOOD),
                createTip("USD", 17.00, 17.00, "Pizzeria Bella", ServiceQuality.GOOD),
                createTip("USD", 25.00, 25.00, "Sushi Palace", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("60.00"), "Pizzeria Bella", null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(3, response.restaurantTipCount());
        assertEquals(new BigDecimal("16.00"), response.restaurantMedianTipPercentage());
        assertEquals(new BigDecimal("16.00"), response.restaurantAverageTipPercentage());

        // Suggestion should reference restaurant typical
        Optional<SmartTipSuggestion> match = response.suggestions().stream()
                .filter(s -> s.reason().toLowerCase().contains("pizzeria bella"))
                .findFirst();
        assertTrue(match.isPresent(), "Expected suggestion mentioning restaurant history");
    }

    @Test
    @DisplayName("Restaurant Context: Case and leading/trailing whitespace insensitive")
    void testRestaurantContextNormalization() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "Blue Fin Bistro", ServiceQuality.GOOD),
                createTip("USD", 19.00, 19.00, "Blue Fin Bistro", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), "  blue fin bistro  ", null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(2, response.restaurantTipCount());
        assertEquals(new BigDecimal("17.00"), response.restaurantMedianTipPercentage());
    }

    @Test
    @DisplayName("Restaurant Context: Single visit (< 2 tips) does not override baseline suggestions")
    void testRestaurantContextSingleVisitIgnored() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "New Diner", ServiceQuality.GOOD),
                createTip("USD", 18.00, 18.00, "Other Place", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), "New Diner", null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        // sampleSize < 2 at restaurant -> not enough history for restaurant-specific suggestion
        assertNull(response.restaurantTipCount());
        assertNull(response.restaurantMedianTipPercentage());
    }

    // =========================================================================
    // 5. SERVICE QUALITY CONTEXT
    // =========================================================================

    @Test
    @DisplayName("Service Quality: Calculates average for specified rating and reflects in reason")
    void testServiceQualityStats() {
        List<Tip> tips = List.of(
                createTip("USD", 20.00, 20.00, "R1", ServiceQuality.EXCELLENT),
                createTip("USD", 22.00, 22.00, "R2", ServiceQuality.EXCELLENT),
                createTip("USD", 15.00, 15.00, "R3", ServiceQuality.AVERAGE)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("100.00"), null, ServiceQuality.EXCELLENT, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(2, response.serviceQualityTipCount());
        assertEquals(new BigDecimal("21.00"), response.serviceQualityAverageTipPercentage());
    }

    // =========================================================================
    // 6. OPTIMIZATION BOUNDS (Day 18)
    // =========================================================================

    @Test
    @DisplayName("Optimization: Integrates Day 18 optimization range and includes OPTIMIZED suggestion")
    void testOptimizationBoundsIntegration() {
        List<Tip> tips = List.of(
                createTip("USD", 16.00, 16.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 18.00, 18.00, "R2", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        TipOptimizationResponse optResponse = new TipOptimizationResponse(
                "USD", new BigDecimal("17.00"), new BigDecimal("17.00"), new BigDecimal("17.00"),
                new BigDecimal("15.00"), new BigDecimal("19.00"), 2,
                BudgetConfidence.MEDIUM, null, null, null, "Optimization ready"
        );
        when(tipOptimizationService.optimize(eq(TEST_EMAIL), any())).thenReturn(optResponse);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("80.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(new BigDecimal("15.00"), response.optimizedMinimumPercentage());
        assertEquals(new BigDecimal("19.00"), response.optimizedMaximumPercentage());

        boolean hasOptimized = response.suggestions().stream()
                .anyMatch(s -> s.type() == SmartTipSuggestionType.OPTIMIZED);
        assertTrue(hasOptimized, "Expected at least one OPTIMIZED suggestion type");
    }

    // =========================================================================
    // 7. BUDGET CONTEXT & IMPACT (Day 17)
    // =========================================================================

    @Test
    @DisplayName("Budget Impact: Accurately reflects remaining budget and overspend")
    void testBudgetImpactCalculation() {
        List<Tip> tips = List.of(createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        // Budget: limit $200, spent $150 -> remaining $50
        TipBudgetStatusResponse budgetRes = new TipBudgetStatusResponse(
                "USD", new BigDecimal("200.00"), new BigDecimal("150.00"),
                new BigDecimal("50.00"), new BigDecimal("75.00"),
                new BigDecimal("160.00"), TipBudgetStatus.UNDER_BUDGET,
                5, BudgetConfidence.HIGH, "Budget is on track"
        );
        when(tipBudgetService.getBudgetStatus(TEST_EMAIL, "USD")).thenReturn(budgetRes);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("100.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(TipBudgetStatus.UNDER_BUDGET, response.budgetStatus());
        assertEquals(new BigDecimal("75.00"), response.budgetUsagePercentage());

        // Check suggestions for budget impact text
        for (SmartTipSuggestion s : response.suggestions()) {
            assertNotNull(s.budgetImpact());
            assertTrue(s.budgetImpact().contains("monthly budget") || s.budgetImpact().contains("Exceeds"));
        }
    }

    @Test
    @DisplayName("Budget Warning: When budget is OVER_BUDGET, LOWER suggestion is selected as recommended")
    void testBudgetExceededPrioritizesLower() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 20.00, 20.00, "R2", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        // Budget is OVER_BUDGET
        TipBudgetStatusResponse budgetRes = new TipBudgetStatusResponse(
                "USD", new BigDecimal("100.00"), new BigDecimal("120.00"),
                new BigDecimal("-20.00"), new BigDecimal("120.00"),
                new BigDecimal("80.00"), TipBudgetStatus.OVER_BUDGET,
                5, BudgetConfidence.HIGH, "Budget is exceeded"
        );
        when(tipBudgetService.getBudgetStatus(TEST_EMAIL, "USD")).thenReturn(budgetRes);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(TipBudgetStatus.OVER_BUDGET, response.budgetStatus());
        assertNotNull(response.primarySuggestion());
        assertEquals(SmartTipSuggestionType.LOWER, response.primarySuggestion().type());
        assertTrue(response.primarySuggestion().isRecommended());
    }

    // =========================================================================
    // 8. GOAL RELEVANCE (Day 22)
    // =========================================================================

    @Test
    @DisplayName("Goal Alignment: Active goal in same currency sets goalRelevant = true")
    void testActiveGoalAlignment() {
        List<Tip> tips = List.of(createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        TipGoalProgressResponse goal = new TipGoalProgressResponse(
                UUID.randomUUID(), TipGoalType.AVERAGE_TIP_PERCENTAGE,
                new BigDecimal("15.00"), new BigDecimal("14.00"),
                new BigDecimal("93.33"), new BigDecimal("1.00"),
                TipGoalStatus.ACTIVE, 5, "HIGH", "Stay on track",
                "USD", com.aitip.entity.TipGoalPeriod.CURRENT_MONTH, java.time.LocalDate.now().minusDays(10), java.time.LocalDate.now().plusDays(20),
                null, null
        );
        when(tipGoalService.getGoals(TEST_EMAIL)).thenReturn(List.of(goal));

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertTrue(response.goalRelevant());
    }

    @Test
    @DisplayName("Goal Alignment: Goal in different currency does not trigger goalRelevant")
    void testCrossCurrencyGoalIgnored() {
        List<Tip> tips = List.of(createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        TipGoalProgressResponse eurGoal = new TipGoalProgressResponse(
                UUID.randomUUID(), TipGoalType.AVERAGE_TIP_PERCENTAGE,
                new BigDecimal("15.00"), new BigDecimal("14.00"),
                new BigDecimal("93.33"), new BigDecimal("1.00"),
                TipGoalStatus.ACTIVE, 5, "HIGH", "Stay on track",
                "EUR", com.aitip.entity.TipGoalPeriod.CURRENT_MONTH, java.time.LocalDate.now().minusDays(10), java.time.LocalDate.now().plusDays(20),
                null, null
        );
        when(tipGoalService.getGoals(TEST_EMAIL)).thenReturn(List.of(eurGoal));

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertFalse(response.goalRelevant());
    }

    // =========================================================================
    // 9. RECENT EVOLUTION DIRECTION (Day 30)
    // =========================================================================

    @Test
    @DisplayName("Recent Direction: Successfully pulls overall direction from Day 30 evolution")
    void testRecentDirectionIntegration() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 18.00, 18.00, "R2", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        TipEvolutionResponse evoResponse = new TipEvolutionResponse(
                LocalDateTime.now(), TipEvolutionPeriod.LAST_6_MONTHS, "USD", 2, 2,
                TipEvolutionDirection.MORE_GENEROUS, new BigDecimal("16.50"),
                new BigDecimal("15.00"), new BigDecimal("18.00"),
                new BigDecimal("3.00"), Collections.emptyList(), null, "Trending upward", null
        );
        when(tipEvolutionService.getEvolution(eq(TEST_EMAIL), eq("USD"), any())).thenReturn(evoResponse);

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(TipEvolutionDirection.MORE_GENEROUS, response.recentDirection());
    }

    @Test
    @DisplayName("Recent Direction: Gracefully falls back to INSUFFICIENT_DATA when evolution service throws")
    void testRecentDirectionFallbackOnException() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 18.00, 18.00, "R2", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);
        when(tipEvolutionService.getEvolution(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Evolution unavailable"));

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("50.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(TipEvolutionDirection.INSUFFICIENT_DATA, response.recentDirection());
    }

    // =========================================================================
    // 10. CURRENCY ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Currency Isolation: Request for EUR uses ONLY EUR tips; USD tips are completely ignored")
    void testStrictCurrencyIsolation() {
        List<Tip> allTips = List.of(
                createTip("USD", 25.00, 25.00, "US Cafe", ServiceQuality.GOOD),
                createTip("USD", 30.00, 30.00, "US Diner", ServiceQuality.GOOD),
                createTip("EUR", 10.00, 10.00, "Euro Bistro", ServiceQuality.GOOD),
                createTip("EUR", 12.00, 12.00, "Euro Cafe", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(allTips);

        SmartTipRequest request = new SmartTipRequest("EUR", new BigDecimal("100.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals("EUR", response.currency());
        // Median of EUR tips [10.00, 12.00] = 11.00% (NOT 25% or 30% from USD)
        assertEquals(new BigDecimal("11.00"), response.historicalMedianTipPercentage());
        assertEquals(new BigDecimal("11.00"), response.historicalAverageTipPercentage());
    }

    @Test
    @DisplayName("Currency Isolation: Request for GBP when user only has USD tips triggers no-history flow")
    void testCurrencyIsolationNoMatchingTips() {
        List<Tip> allTips = List.of(
                createTip("USD", 25.00, 25.00, "US Cafe", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(allTips);

        SmartTipRequest request = new SmartTipRequest("GBP", new BigDecimal("100.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals("GBP", response.currency());
        assertNull(response.historicalMedianTipPercentage());
        assertTrue(response.message().contains("General tip options"));
    }

    // =========================================================================
    // 11. USER ISOLATION
    // =========================================================================

    @Test
    @DisplayName("User Isolation: User A's tips never leak into User B's smart tip calculation")
    void testStrictUserIsolation() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setEmail("other@example.com");

        when(userService.getUserByEmail("other@example.com")).thenReturn(otherUser);
        when(tipRepository.findAllByUserId(otherUserId)).thenReturn(Collections.emptyList());

        SmartTipRequest request = new SmartTipRequest("USD", new BigDecimal("100.00"), null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip("other@example.com", request);

        assertNull(response.historicalMedianTipPercentage());
        assertEquals(4, response.suggestions().size());
        assertTrue(response.message().contains("General tip options"));
    }

    // =========================================================================
    // 12. SUGGESTION INVARIANTS & ORDERING
    // =========================================================================

    @Test
    @DisplayName("Invariants: Suggestions strictly sorted ascending, exactly 1 recommended, amounts mathematically exact")
    void testSuggestionInvariants() {
        List<Tip> tips = List.of(
                createTip("USD", 15.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 17.50, 17.50, "R2", ServiceQuality.GOOD),
                createTip("USD", 20.00, 20.00, "R3", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        BigDecimal bill = new BigDecimal("83.50");
        SmartTipRequest request = new SmartTipRequest("USD", bill, null, null, new BigDecimal("15.00"));
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        List<SmartTipSuggestion> suggestions = response.suggestions();
        assertTrue(suggestions.size() >= 3 && suggestions.size() <= 4);

        // Strictly sorted ascending
        for (int i = 0; i < suggestions.size() - 1; i++) {
            assertTrue(suggestions.get(i).tipPercentage().compareTo(suggestions.get(i + 1).tipPercentage()) < 0,
                    "Suggestions must be strictly ascending with no duplicate percentages");
        }

        // Exactly one isRecommended = true
        long recommendedCount = suggestions.stream().filter(s -> Boolean.TRUE.equals(s.isRecommended())).count();
        assertEquals(1, recommendedCount);
        assertEquals(response.primarySuggestion().tipPercentage(),
                suggestions.stream().filter(s -> Boolean.TRUE.equals(s.isRecommended())).findFirst().get().tipPercentage());

        // Check math accuracy
        for (SmartTipSuggestion s : suggestions) {
            BigDecimal expectedTip = bill.multiply(s.tipPercentage()).divide(new BigDecimal("100.00"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal expectedTotal = bill.add(expectedTip);
            assertEquals(expectedTip, s.tipAmount());
            assertEquals(expectedTotal, s.totalAmount());
            assertNotNull(s.label());
            assertNotNull(s.reason());
        }
    }

    // =========================================================================
    // 13. DAY 32: BEHAVIORAL ADAPTATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Day 32: Bounded adaptation adapts primary suggestion and preserves baseline recommendation")
    void testAdaptation_AppliedSuccessfully() {
        List<Tip> tips = List.of(
                createTip("USD", 50.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 50.00, 15.00, "R2", ServiceQuality.GOOD),
                createTip("USD", 50.00, 15.00, "R3", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        // Mock adaptation service returning +2.50 pp adjustment
        SmartTipAdaptationResult adaptation = new SmartTipAdaptationResult(
                6, 1, 5, 0, 6, new BigDecimal("2.50"),
                SmartTipFeedbackDirection.PREFERS_HIGHER, true, new BigDecimal("2.50"),
                "Based on 6 previous decisions, you usually choose around 2.5 percentage points above the suggested tip."
        );
        when(smartTipAdaptationService.getAdaptation(testUser, "USD")).thenReturn(adaptation);

        BigDecimal bill = new BigDecimal("100.00");
        SmartTipRequest request = new SmartTipRequest("USD", bill, null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        // Baseline median is 15.00%
        assertEquals(new BigDecimal("15.00"), response.historicalMedianTipPercentage());

        // Baseline primary suggestion was 15.00%
        assertNotNull(response.baselinePrimarySuggestion());
        assertEquals(new BigDecimal("15.00"), response.baselinePrimarySuggestion().tipPercentage());

        // Adapted primary suggestion is 15.00 + 2.50 = 17.50%
        assertEquals(new BigDecimal("17.50"), response.primarySuggestion().tipPercentage());
        assertEquals(new BigDecimal("17.50"), response.primarySuggestion().tipAmount());
        assertEquals(new BigDecimal("117.50"), response.primarySuggestion().totalAmount());
        assertTrue(response.primarySuggestion().label().contains("Personalized"));

        // Response metadata
        assertEquals(6L, response.feedbackCount());
        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, response.feedbackDirection());
        assertTrue(response.adaptationApplied());
        assertEquals(new BigDecimal("2.50"), response.adaptationAdjustment());
        assertNotNull(response.adaptationMessage());
    }

    @Test
    @DisplayName("Day 32: Bounded adaptation clamps maximum adjustment to +3.00 pp")
    void testAdaptation_ClampedToMaximumThreePp() {
        List<Tip> tips = List.of(
                createTip("USD", 50.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 50.00, 15.00, "R2", ServiceQuality.GOOD),
                createTip("USD", 50.00, 15.00, "R3", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        // Mock adaptation adjustment of +3.00 (clamped from +5.00)
        SmartTipAdaptationResult adaptation = new SmartTipAdaptationResult(
                5, 0, 5, 0, 5, new BigDecimal("5.00"),
                SmartTipFeedbackDirection.PREFERS_HIGHER, true, new BigDecimal("3.00"),
                "Based on 5 previous decisions..."
        );
        when(smartTipAdaptationService.getAdaptation(testUser, "USD")).thenReturn(adaptation);

        BigDecimal bill = new BigDecimal("100.00");
        SmartTipRequest request = new SmartTipRequest("USD", bill, null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        // 15.00 + 3.00 = 18.00%
        assertEquals(new BigDecimal("18.00"), response.primarySuggestion().tipPercentage());
        assertEquals(new BigDecimal("15.00"), response.baselinePrimarySuggestion().tipPercentage());
    }

    @Test
    @DisplayName("Day 32: Negative adaptation lowers primary suggestion bounded to -3.00 pp")
    void testAdaptation_NegativeAdjustment() {
        List<Tip> tips = List.of(
                createTip("USD", 50.00, 18.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 50.00, 18.00, "R2", ServiceQuality.GOOD),
                createTip("USD", 50.00, 18.00, "R3", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipAdaptationResult adaptation = new SmartTipAdaptationResult(
                5, 0, 5, 0, 5, new BigDecimal("-2.50"),
                SmartTipFeedbackDirection.PREFERS_LOWER, true, new BigDecimal("-2.50"),
                "Based on 5 previous decisions..."
        );
        when(smartTipAdaptationService.getAdaptation(testUser, "USD")).thenReturn(adaptation);

        BigDecimal bill = new BigDecimal("100.00");
        SmartTipRequest request = new SmartTipRequest("USD", bill, null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        // 18.00 - 2.50 = 15.50%
        assertEquals(new BigDecimal("15.50"), response.primarySuggestion().tipPercentage());
        assertEquals(new BigDecimal("18.00"), response.baselinePrimarySuggestion().tipPercentage());
    }

    @Test
    @DisplayName("Day 32: No adaptation when insufficient evidence or aligned")
    void testAdaptation_NotApplied_BaselineEqualsPrimary() {
        List<Tip> tips = List.of(
                createTip("USD", 50.00, 15.00, "R1", ServiceQuality.GOOD),
                createTip("USD", 50.00, 15.00, "R2", ServiceQuality.GOOD),
                createTip("USD", 50.00, 15.00, "R3", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(tips);

        SmartTipAdaptationResult adaptation = new SmartTipAdaptationResult(
                3, 1, 2, 0, 3, new BigDecimal("2.50"),
                SmartTipFeedbackDirection.INSUFFICIENT_DATA, false, null,
                "Not enough feedback..."
        );
        when(smartTipAdaptationService.getAdaptation(testUser, "USD")).thenReturn(adaptation);

        BigDecimal bill = new BigDecimal("100.00");
        SmartTipRequest request = new SmartTipRequest("USD", bill, null, null, null);
        SmartTipResponse response = smartTipService.getSmartTip(TEST_EMAIL, request);

        assertEquals(new BigDecimal("15.00"), response.primarySuggestion().tipPercentage());
        assertEquals(response.primarySuggestion().tipPercentage(), response.baselinePrimarySuggestion().tipPercentage());
        assertFalse(response.adaptationApplied());
    }
}
