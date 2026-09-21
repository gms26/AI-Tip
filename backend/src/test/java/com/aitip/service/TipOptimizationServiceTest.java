package com.aitip.service;

import com.aitip.dto.BudgetConfidence;
import com.aitip.dto.TipOptimizationRequest;
import com.aitip.dto.TipOptimizationResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TipOptimizationService.
 *
 * Tests cover: empty state, single tip, odd/even median, mean,
 * outlier handling, target range clamping, confidence levels,
 * currency isolation, bill-level comparison, monthly estimation,
 * BigDecimal rounding, and the monthlyBudget-as-assumption contract.
 */
class TipOptimizationServiceTest {

    private TipOptimizationService service;
    private TipRepository tipRepository;
    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        tipRepository = Mockito.mock(TipRepository.class);
        userService = Mockito.mock(UserService.class);
        service = new TipOptimizationService(tipRepository, userService);

        testUser = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, java.util.UUID.randomUUID());
        } catch (Exception ignored) {}

        when(userService.getUserByEmail("alice@example.com")).thenReturn(testUser);
    }

    // =============================================
    // 1. No history Ã¢â‚¬â€ empty state
    // =============================================
    @Test
    void noHistory_returnsEmptyState() {
        when(tipRepository.findAllByUserId(any())).thenReturn(Collections.emptyList());

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals("USD", response.currency());
        assertEquals(new BigDecimal("20"), response.currentTipPercentage());
        assertEquals(0, response.sampleSize());
        assertEquals(BudgetConfidence.LOW, response.confidence());
        assertNull(response.historicalMedianPercentage());
        assertNull(response.historicalMeanPercentage());
        assertNull(response.recommendedMinimumPercentage());
        assertNull(response.recommendedMaximumPercentage());
        assertNull(response.monthlyTipEstimate());
        assertNull(response.optimizedMonthlyTipEstimate());
        assertNull(response.potentialMonthlyDifference());
        assertTrue(response.message().contains("No tipping history"));
    }

    // =============================================
    // 2. One tip Ã¢â‚¬â€ LOW confidence
    // =============================================
    @Test
    void oneTip_returnsLowConfidence() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("18.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(1, response.sampleSize());
        assertEquals(BudgetConfidence.LOW, response.confidence());
        assertEquals(new BigDecimal("18.00"), response.historicalMedianPercentage());
        assertEquals(new BigDecimal("18.00"), response.historicalMeanPercentage());
    }

    // =============================================
    // 3. Two tips Ã¢â‚¬â€ even median, MEDIUM confidence
    // =============================================
    @Test
    void twoTips_evenMedian_mediumConfidence() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("18.00", "USD"),
                buildTip("22.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("25"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(2, response.sampleSize());
        assertEquals(BudgetConfidence.MEDIUM, response.confidence());
        // Median of [18, 22] = (18 + 22) / 2 = 20.00
        assertEquals(new BigDecimal("20.00"), response.historicalMedianPercentage());
        // Mean of [18, 22] = 40 / 2 = 20.00
        assertEquals(new BigDecimal("20.00"), response.historicalMeanPercentage());
    }

    // =============================================
    // 4. Three tips Ã¢â‚¬â€ odd median
    // =============================================
    @Test
    void threeTips_oddMedian() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("15.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("25.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("22"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(3, response.sampleSize());
        // Sorted: [15, 20, 25] Ã¢â€ â€™ median = 20
        assertEquals(new BigDecimal("20.00"), response.historicalMedianPercentage());
    }

    // =============================================
    // 5. Five tips Ã¢â‚¬â€ HIGH confidence, odd median
    // =============================================
    @Test
    void fiveTips_highConfidence_oddMedian() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("15.00", "USD"),
                buildTip("17.00", "USD"),
                buildTip("18.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("22.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("25"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(5, response.sampleSize());
        assertEquals(BudgetConfidence.HIGH, response.confidence());
        // Sorted: [15, 17, 18, 20, 22] Ã¢â€ â€™ median = 18
        assertEquals(new BigDecimal("18.00"), response.historicalMedianPercentage());
        // Mean: (15+17+18+20+22)/5 = 18.40
        assertEquals(new BigDecimal("18.40"), response.historicalMeanPercentage());
    }

    // =============================================
    // 6. Four tips Ã¢â‚¬â€ even median
    // =============================================
    @Test
    void fourTips_evenMedian() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("10.00", "USD"),
                buildTip("15.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("25.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Sorted: [10, 15, 20, 25] Ã¢â€ â€™ median = (15 + 20) / 2 = 17.50
        assertEquals(new BigDecimal("17.50"), response.historicalMedianPercentage());
    }

    // =============================================
    // 7. Mean calculation
    // =============================================
    @Test
    void meanCalculation_correctRounding() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("10.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("30.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Mean: (10+20+30)/3 = 20.00
        assertEquals(new BigDecimal("20.00"), response.historicalMeanPercentage());
    }

    // =============================================
    // 8. Outlier handling Ã¢â‚¬â€ median is robust
    // =============================================
    @Test
    void outlierDoesNotDistortMedian() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("18.00", "USD"),
                buildTip("19.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("100.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Sorted: [18, 19, 20, 100] Ã¢â€ â€™ median = (19 + 20) / 2 = 19.50
        assertEquals(new BigDecimal("19.50"), response.historicalMedianPercentage());
        // Mean: (18+19+20+100)/4 = 39.25 Ã¢â‚¬â€ much higher than median
        assertEquals(new BigDecimal("39.25"), response.historicalMeanPercentage());
    }

    // =============================================
    // 9. Target range = median Ã‚Â± 2
    // =============================================
    @Test
    void targetRange_medianPlusMinusTwo() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("18.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("22.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("25"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Median = 20.00 Ã¢â€ â€™ range = [18.00, 22.00]
        assertEquals(new BigDecimal("18.00"), response.recommendedMinimumPercentage());
        assertEquals(new BigDecimal("22.00"), response.recommendedMaximumPercentage());
    }

    // =============================================
    // 10. Minimum clamp at 0
    // =============================================
    @Test
    void targetRange_clampsMinimumAtZero() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("0.50", "USD"),
                buildTip("1.00", "USD"),
                buildTip("1.50", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("5"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Median = 1.00 Ã¢â€ â€™ min = max(0, 1 - 2) = 0.00
        assertEquals(new BigDecimal("0.00").compareTo(response.recommendedMinimumPercentage()), 0);
        assertEquals(new BigDecimal("3.00"), response.recommendedMaximumPercentage());
    }

    // =============================================
    // 11. Maximum clamp at 100
    // =============================================
    @Test
    void targetRange_clampsMaximumAt100() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("98.00", "USD"),
                buildTip("99.00", "USD"),
                buildTip("100.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("99"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Median = 99.00 Ã¢â€ â€™ max = min(100, 99 + 2) = 100
        assertEquals(new BigDecimal("97.00"), response.recommendedMinimumPercentage());
        assertEquals(new BigDecimal("100").compareTo(response.recommendedMaximumPercentage()), 0);
    }

    // =============================================
    // 12. LOW confidence (0-1 tips)
    // =============================================
    @Test
    void lowConfidence_zeroTips() {
        assertEquals(BudgetConfidence.LOW, service.determineConfidence(0));
    }

    @Test
    void lowConfidence_oneTip() {
        assertEquals(BudgetConfidence.LOW, service.determineConfidence(1));
    }

    // =============================================
    // 13. MEDIUM confidence (2-4 tips)
    // =============================================
    @Test
    void mediumConfidence_twoTips() {
        assertEquals(BudgetConfidence.MEDIUM, service.determineConfidence(2));
    }

    @Test
    void mediumConfidence_fourTips() {
        assertEquals(BudgetConfidence.MEDIUM, service.determineConfidence(4));
    }

    // =============================================
    // 14. HIGH confidence (5+ tips)
    // =============================================
    @Test
    void highConfidence_fiveTips() {
        assertEquals(BudgetConfidence.HIGH, service.determineConfidence(5));
    }

    @Test
    void highConfidence_tenTips() {
        assertEquals(BudgetConfidence.HIGH, service.determineConfidence(10));
    }

    // =============================================
    // 15. USD isolation Ã¢â‚¬â€ only USD tips counted
    // =============================================
    @Test
    void usdIsolation_onlyUsdTipsCounted() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("15.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("10.00", "INR"),
                buildTip("12.00", "INR")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("25"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(2, response.sampleSize());
        // Only USD tips: [15, 20] Ã¢â€ â€™ median = 17.50
        assertEquals(new BigDecimal("17.50"), response.historicalMedianPercentage());
    }

    // =============================================
    // 16. INR isolation Ã¢â‚¬â€ only INR tips counted
    // =============================================
    @Test
    void inrIsolation_onlyInrTipsCounted() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("15.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("10.00", "INR"),
                buildTip("12.00", "INR"),
                buildTip("14.00", "INR")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("INR", new BigDecimal("15"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(3, response.sampleSize());
        // Only INR tips: [10, 12, 14] Ã¢â€ â€™ median = 12.00
        assertEquals(new BigDecimal("12.00"), response.historicalMedianPercentage());
    }

    // =============================================
    // 17. Cross-currency exclusion
    // =============================================
    @Test
    void crossCurrencyExclusion_noHistoryForGbp() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("15.00", "USD"),
                buildTip("20.00", "INR")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("GBP", new BigDecimal("15"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertEquals(0, response.sampleSize());
        assertNull(response.historicalMedianPercentage());
        assertTrue(response.message().contains("No tipping history"));
    }

    // =============================================
    // 18. Current tip difference calculation (bill-level)
    // =============================================
    @Test
    void billLevelComparison_currentVsMedian() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("18.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("22.00", "USD")
        ));

        // Bill = 1000, current tip = 25%, median = 20%
        TipOptimizationRequest request = new TipOptimizationRequest(
                "USD", new BigDecimal("25"), null, new BigDecimal("1000.00"));
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Median = 20.00
        assertEquals(new BigDecimal("20.00"), response.historicalMedianPercentage());
        // Message should mention above/below median
        assertTrue(response.message().contains("above"));
    }

    // =============================================
    // 19. BigDecimal rounding (HALF_UP)
    // =============================================
    @Test
    void bigDecimalRounding_halfUp() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("10.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("30.00", "USD"),
                buildTip("33.33", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // Mean: (10+20+30+33.33)/4 = 93.33/4 = 23.3325 Ã¢â€ â€™ 23.33 (HALF_UP)
        assertEquals(new BigDecimal("23.33"), response.historicalMeanPercentage());
    }

    // =============================================
    // 20. Monthly estimation with budget
    // =============================================
    @Test
    void monthlyEstimation_withBudget() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("18.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("22.00", "USD")
        ));

        // Budget = 5000, current tip = 25%, median = 20%
        TipOptimizationRequest request = new TipOptimizationRequest(
                "USD", new BigDecimal("25"), new BigDecimal("5000.00"), null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // monthlyTipEstimate = 5000 * 25 / 100 = 1250.00
        assertEquals(new BigDecimal("1250.00"), response.monthlyTipEstimate());
        // optimizedMonthlyTipEstimate = 5000 * 20 / 100 = 1000.00
        assertEquals(new BigDecimal("1000.00"), response.optimizedMonthlyTipEstimate());
        // potentialMonthlyDifference = 1250 - 1000 = 250.00
        assertEquals(new BigDecimal("250.00"), response.potentialMonthlyDifference());
    }

    // =============================================
    // 21. Monthly estimation Ã¢â‚¬â€ no budget Ã¢â€ â€™ null monthly fields
    // =============================================
    @Test
    void monthlyEstimation_noBudget_nullFields() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("20.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("25"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertNull(response.monthlyTipEstimate());
        assertNull(response.optimizedMonthlyTipEstimate());
        assertNull(response.potentialMonthlyDifference());
    }

    // =============================================
    // 22. Monthly estimation Ã¢â‚¬â€ budget supplied but no history Ã¢â€ â€™ null optimized/difference
    // =============================================
    @Test
    void monthlyEstimation_budgetButNoHistory_nullOptimized() {
        when(tipRepository.findAllByUserId(any())).thenReturn(Collections.emptyList());

        TipOptimizationRequest request = new TipOptimizationRequest(
                "USD", new BigDecimal("25"), new BigDecimal("5000.00"), null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        // No history Ã¢â€ â€™ empty state, all monthly fields null
        assertNull(response.monthlyTipEstimate());
        assertNull(response.optimizedMonthlyTipEstimate());
        assertNull(response.potentialMonthlyDifference());
    }

    // =============================================
    // 23. Historical median comparison Ã¢â‚¬â€ same as current
    // =============================================
    @Test
    void currentMatchesMedian_messageReflects() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("20.00", "USD"),
                buildTip("20.00", "USD"),
                buildTip("20.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("20"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertTrue(response.message().contains("matches your historical median"));
    }

    // =============================================
    // 24. Below median message
    // =============================================
    @Test
    void currentBelowMedian_messageReflects() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(
                buildTip("20.00", "USD"),
                buildTip("22.00", "USD"),
                buildTip("24.00", "USD")
        ));

        TipOptimizationRequest request = new TipOptimizationRequest("USD", new BigDecimal("15"), null, null);
        TipOptimizationResponse response = service.optimize("alice@example.com", request);

        assertTrue(response.message().contains("below"));
    }

    // =============================================
    // 25. Median internal method Ã¢â‚¬â€ empty list
    // =============================================
    @Test
    void calculateMedian_emptyList_returnsZero() {
        assertEquals(BigDecimal.ZERO, service.calculateMedian(List.of()));
    }

    // =============================================
    // 26. Mean internal method Ã¢â‚¬â€ empty list
    // =============================================
    @Test
    void calculateMean_emptyList_returnsZero() {
        assertEquals(BigDecimal.ZERO, service.calculateMean(List.of()));
    }

    // =============================================
    // Helpers
    // =============================================
    private Tip buildTip(String percentage, String currency) {
        return Tip.builder()
                .user(testUser)
                .restaurantName("Test Restaurant")
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal(percentage))
                .tipAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .currency(currency)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
