package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipForecastCalculationServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TipForecastCalculationService service;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setup() {
        testUser = User.builder()
                .name("Test User")
                .email(TEST_EMAIL)
                .password("encoded")
                .build();
        // Use reflection to set ID since it's auto-generated
        try {
            var idField = testUser.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Tip createTip(BigDecimal tipAmount, BigDecimal tipPercentage, String currency,
                          LocalDateTime createdAt) {
        Tip tip = new Tip();
        tip.setUser(testUser);
        tip.setBillAmount(new BigDecimal("100.00"));
        tip.setTipAmount(tipAmount);
        tip.setTipPercentage(tipPercentage);
        tip.setTotalAmount(new BigDecimal("100.00").add(tipAmount));
        tip.setCurrency(currency);
        tip.setRestaurantName("Test Restaurant");
        tip.setCreatedAt(createdAt);
        return tip;
    }

    private void setupMock(List<Tip> tips) {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(UUID.class), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(tips);
    }

    // =============================================
    // 1. Zero-history tests
    // =============================================

    @Test
    void testZeroHistory_returnsLowConfidence() {
        setupMock(Collections.emptyList());
        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(0, res.historicalTipCount());
        assertEquals(BudgetConfidence.LOW, res.confidence());
        assertEquals(BigDecimal.ZERO, res.estimatedMonthlyTipAmount());
        assertEquals(0, res.estimatedTipCount());
        assertTrue(res.message().contains("No tipping history"));
    }

    @Test
    void testZeroHistory_withBudget_noBudgetProjection() {
        setupMock(Collections.emptyList());
        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS,
                new BigDecimal("1000"), 90);

        assertNull(res.projectedBudgetUsage());
        assertNull(res.budgetStatus());
        assertNotNull(res.monthlyBudget());
    }

    // =============================================
    // 2. Single tip (LOW confidence)
    // =============================================

    @Test
    void testSingleTip_lowConfidence() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(5))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(1, res.historicalTipCount());
        assertEquals(BudgetConfidence.LOW, res.confidence());
        assertEquals(new BigDecimal("20.00"), res.historicalAverageTipAmount());
        assertEquals(new BigDecimal("20.00"), res.historicalMedianPercentage());
        assertTrue(res.message().contains("limited history"));
    }

    // =============================================
    // 3. 2Ã¢â‚¬â€œ4 tips (MEDIUM confidence)
    // =============================================

    @Test
    void testTwoTips_mediumConfidence() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(5)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(3))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(2, res.historicalTipCount());
        assertEquals(BudgetConfidence.MEDIUM, res.confidence());
    }

    @Test
    void testFourTips_mediumConfidence() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(10)),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD", LocalDateTime.now().minusDays(8)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(5)),
                createTip(new BigDecimal("25.00"), new BigDecimal("25.00"), "USD", LocalDateTime.now().minusDays(2))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(4, res.historicalTipCount());
        assertEquals(BudgetConfidence.MEDIUM, res.confidence());
    }

    // =============================================
    // 4. 5+ tips (HIGH confidence)
    // =============================================

    @Test
    void testFiveTips_highConfidence() {
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tips.add(createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD",
                    LocalDateTime.now().minusDays(i + 1)));
        }
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(5, res.historicalTipCount());
        assertEquals(BudgetConfidence.HIGH, res.confidence());
    }

    // =============================================
    // 5. Median tests (odd vs even)
    // =============================================

    @Test
    void testMedian_oddCount() {
        // Values: 10, 15, 20 Ã¢â€ â€™ median = 15
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(new BigDecimal("15.00"), res.historicalMedianPercentage());
    }

    @Test
    void testMedian_evenCount() {
        // Values: 10, 15, 20, 25 Ã¢â€ â€™ median = (15 + 20) / 2 = 17.50
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(4)),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("25.00"), new BigDecimal("25.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(new BigDecimal("17.50"), res.historicalMedianPercentage());
    }

    // =============================================
    // 6. Mean calculations
    // =============================================

    @Test
    void testMeanPercentage() {
        // Values: 10, 20, 30 Ã¢â€ â€™ mean = 20
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("30.00"), new BigDecimal("30.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(new BigDecimal("20.00"), res.historicalAveragePercentage());
        assertEquals(new BigDecimal("20.00"), res.historicalAverageTipAmount());
    }

    @Test
    void testMeanAmount_fractional() {
        // Amounts: 10, 20, 33 Ã¢â€ â€™ mean = 63/3 = 21.00
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("33.00"), new BigDecimal("33.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(new BigDecimal("21.00"), res.historicalAverageTipAmount());
    }

    // =============================================
    // 7. Daily rate & projection
    // =============================================

    @Test
    void testDailyRate_next30Days() {
        // 10 tips in 90 days Ã¢â€ â€™ dailyRate = 10/90 = 0.1111
        // Estimated count = 0.1111 * 30 = 3.333 Ã¢â€ â€™ 3
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tips.add(createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD",
                    LocalDateTime.now().minusDays(i * 9 + 1)));
        }
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(30, res.forecastDays());
        // dailyRate = 10/90 = 0.1111, estimatedCount = 0.1111 * 30 = 3.333 Ã¢â€ â€™ 3
        assertEquals(3, res.estimatedTipCount());
    }

    @Test
    void testProjection_next3Months() {
        // 9 tips in 90 days Ã¢â€ â€™ dailyRate = 0.1000
        // Estimated count = 0.1000 * 90 = 9
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            tips.add(createTip(new BigDecimal("30.00"), new BigDecimal("15.00"), "USD",
                    LocalDateTime.now().minusDays(i * 10 + 1)));
        }
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_3_MONTHS, null, 90);

        assertEquals(90, res.forecastDays());
        assertEquals(9, res.estimatedTipCount());
        // Estimated amount = 9 * 30.00 = 270.00
        assertEquals(new BigDecimal("270.00"), res.estimatedMonthlyTipAmount());
    }

    @Test
    void testProjection_currentMonth() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.CURRENT_MONTH, null, 90);

        // forecastDays should be remaining days in current month
        LocalDate today = LocalDate.now();
        int expectedDays = today.lengthOfMonth() - today.getDayOfMonth();
        assertEquals(expectedDays, res.forecastDays());
        assertEquals(TipForecastPeriod.CURRENT_MONTH, res.forecastPeriod());
    }

    // =============================================
    // 8. Estimated spending calculation
    // =============================================

    @Test
    void testEstimatedSpending() {
        // 3 tips in 30 days Ã¢â€ â€™ dailyRate = 0.1000
        // forecastDays = 30 Ã¢â€ â€™ estimatedCount = 3
        // avgTipAmount = (10+20+30)/3 = 20
        // estimatedAmount = 3 * 20 = 60.00
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("30.00"), new BigDecimal("30.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 30);

        // 3 tips in 30 days Ã¢â€ â€™ rate = 0.1 Ã¢â€ â€™ count in 30d = 3
        assertEquals(3, res.estimatedTipCount());
        assertEquals(new BigDecimal("60.00"), res.estimatedMonthlyTipAmount());
    }

    // =============================================
    // 9. Budget projection tests
    // =============================================

    @Test
    void testBudgetProjection_underBudget() {
        // 3 tips, avg amount 20, lookback 30d
        // dailyRate = 3/30 = 0.1, estimatedCount = 3, estimatedAmount = 60
        // projectedUsage = 60/1000 * 100 = 6.00%
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS,
                new BigDecimal("1000.00"), 30);

        assertNotNull(res.projectedBudgetUsage());
        assertEquals(TipBudgetStatus.UNDER_BUDGET, res.budgetStatus());
        assertTrue(res.projectedBudgetUsage().compareTo(new BigDecimal("80")) < 0);
    }

    @Test
    void testBudgetProjection_approachingLimit() {
        // 3 tips, avg amount 80, lookback 30d
        // dailyRate = 3/30 = 0.1, estimatedCount = 3, estimatedAmount = 240
        // projectedUsage = 240/300 * 100 = 80.00%
        List<Tip> tips = List.of(
                createTip(new BigDecimal("80.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("80.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("80.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS,
                new BigDecimal("300.00"), 30);

        assertEquals(TipBudgetStatus.APPROACHING_LIMIT, res.budgetStatus());
    }

    @Test
    void testBudgetProjection_overBudget() {
        // 3 tips, avg amount 100, lookback 30d
        // dailyRate = 0.1, estimatedCount = 3, estimatedAmount = 300
        // projectedUsage = 300/200 * 100 = 150.00%
        List<Tip> tips = List.of(
                createTip(new BigDecimal("100.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("100.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("100.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS,
                new BigDecimal("200.00"), 30);

        assertEquals(TipBudgetStatus.OVER_BUDGET, res.budgetStatus());
        assertTrue(res.projectedBudgetUsage().compareTo(HUNDRED) > 0);
        assertTrue(res.message().contains("exceeds"));
    }

    @Test
    void testBudgetProjection_exactly100Percent() {
        // 3 tips, avg amount 100, lookback 30d
        // dailyRate = 0.1, estimatedCount = 3, estimatedAmount = 300
        // projectedUsage = 300/300 * 100 = 100.00%
        List<Tip> tips = List.of(
                createTip(new BigDecimal("100.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("100.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("100.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS,
                new BigDecimal("300.00"), 30);

        assertEquals(TipBudgetStatus.LIMIT_REACHED, res.budgetStatus());
        assertEquals(0, res.projectedBudgetUsage().compareTo(HUNDRED));
    }

    @Test
    void testBudgetProjection_noBudgetSupplied() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertNull(res.monthlyBudget());
        assertNull(res.projectedBudgetUsage());
        assertNull(res.budgetStatus());
    }

    // =============================================
    // 10. Currency isolation
    // =============================================

    @Test
    void testCurrencyIsolation() {
        // Mock returns only USD tips (because DB filters by currency)
        List<Tip> usdTips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(usdTips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals("USD", res.currency());
        assertEquals(1, res.historicalTipCount());
    }

    // =============================================
    // 11. Lookback validation
    // =============================================

    @Test
    void testLookback_default() {
        setupMock(Collections.emptyList());
        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, null);
        assertEquals(90, res.lookbackDays());
    }

    @Test
    void testLookback_minimum() {
        setupMock(Collections.emptyList());
        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 7);
        assertEquals(7, res.lookbackDays());
    }

    @Test
    void testLookback_maximum() {
        setupMock(Collections.emptyList());
        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 365);
        assertEquals(365, res.lookbackDays());
    }

    @Test
    void testLookback_tooLow_throws() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        assertThrows(IllegalArgumentException.class, () ->
                service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 6));
    }

    @Test
    void testLookback_tooHigh_throws() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        assertThrows(IllegalArgumentException.class, () ->
                service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 400));
    }

    // =============================================
    // 12. Invalid budget
    // =============================================

    @Test
    void testInvalidBudget_negative_throws() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        assertThrows(IllegalArgumentException.class, () ->
                service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, new BigDecimal("-1"), 90));
    }

    @Test
    void testInvalidBudget_zero_throws() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        assertThrows(IllegalArgumentException.class, () ->
                service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, BigDecimal.ZERO, 90));
    }

    // =============================================
    // 13. Projected tip percentage uses median
    // =============================================

    @Test
    void testProjectedPercentage_usesMedian() {
        // Values: 10, 20, 30 Ã¢â€ â€™ median = 20
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("30.00"), new BigDecimal("30.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(res.historicalMedianPercentage(), res.projectedTipPercentage());
    }

    // =============================================
    // 14. Confidence boundary transitions
    // =============================================

    @Test
    void testConfidence_boundary_1() {
        assertEquals(BudgetConfidence.LOW, service.determineConfidence(0));
        assertEquals(BudgetConfidence.LOW, service.determineConfidence(1));
    }

    @Test
    void testConfidence_boundary_2() {
        assertEquals(BudgetConfidence.MEDIUM, service.determineConfidence(2));
        assertEquals(BudgetConfidence.MEDIUM, service.determineConfidence(4));
    }

    @Test
    void testConfidence_boundary_5() {
        assertEquals(BudgetConfidence.HIGH, service.determineConfidence(5));
        assertEquals(BudgetConfidence.HIGH, service.determineConfidence(100));
    }

    // =============================================
    // 15. Budget status transitions
    // =============================================

    @Test
    void testBudgetStatus_underBudget() {
        assertEquals(TipBudgetStatus.UNDER_BUDGET,
                service.determineBudgetStatus(new BigDecimal("50.00")));
    }

    @Test
    void testBudgetStatus_approachingLimit() {
        assertEquals(TipBudgetStatus.APPROACHING_LIMIT,
                service.determineBudgetStatus(new BigDecimal("80.00")));
        assertEquals(TipBudgetStatus.APPROACHING_LIMIT,
                service.determineBudgetStatus(new BigDecimal("99.99")));
    }

    @Test
    void testBudgetStatus_limitReached() {
        assertEquals(TipBudgetStatus.LIMIT_REACHED,
                service.determineBudgetStatus(new BigDecimal("100.00")));
    }

    @Test
    void testBudgetStatus_overBudget() {
        assertEquals(TipBudgetStatus.OVER_BUDGET,
                service.determineBudgetStatus(new BigDecimal("100.01")));
        assertEquals(TipBudgetStatus.OVER_BUDGET,
                service.determineBudgetStatus(new BigDecimal("200.00")));
    }

    // =============================================
    // 16. Forecast days calculation
    // =============================================

    @Test
    void testForecastDays_next30() {
        assertEquals(30, service.calculateForecastDays(TipForecastPeriod.NEXT_30_DAYS));
    }

    @Test
    void testForecastDays_next3Months() {
        assertEquals(90, service.calculateForecastDays(TipForecastPeriod.NEXT_3_MONTHS));
    }

    @Test
    void testForecastDays_currentMonth() {
        int forecastDays = service.calculateForecastDays(TipForecastPeriod.CURRENT_MONTH);
        LocalDate today = LocalDate.now();
        int expected = today.lengthOfMonth() - today.getDayOfMonth();
        assertEquals(expected, forecastDays);
    }

    // =============================================
    // 17. BigDecimal rounding
    // =============================================

    @Test
    void testBigDecimalRounding_halfUp() {
        // 3 tips with amounts 10, 10, 11 Ã¢â€ â€™ mean = 31/3 = 10.33 (HALF_UP)
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(3)),
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD", LocalDateTime.now().minusDays(2)),
                createTip(new BigDecimal("11.00"), new BigDecimal("11.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertEquals(new BigDecimal("10.33"), res.historicalAverageTipAmount());
        assertEquals(new BigDecimal("10.33"), res.historicalAveragePercentage());
    }

    // =============================================
    // 18. Message formatting
    // =============================================

    @Test
    void testMessage_containsHistoricalFact() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertTrue(res.message().contains("Based on"));
        assertTrue(res.message().contains("average tip was"));
    }

    @Test
    void testMessage_containsProjection() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS, null, 90);

        assertTrue(res.message().contains("Estimated from your recent history"));
    }

    // =============================================
    // 19. Response structure completeness
    // =============================================

    @Test
    void testResponseStructure_allFieldsPresent() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD", LocalDateTime.now().minusDays(1))
        );
        setupMock(tips);

        TipForecastResponse res = service.forecast(TEST_EMAIL, "USD", TipForecastPeriod.NEXT_30_DAYS,
                new BigDecimal("500"), 90);

        assertEquals("USD", res.currency());
        assertEquals(TipForecastPeriod.NEXT_30_DAYS, res.forecastPeriod());
        assertEquals(30, res.forecastDays());
        assertEquals(90, res.lookbackDays());
        assertNotNull(res.historicalAveragePercentage());
        assertNotNull(res.historicalMedianPercentage());
        assertNotNull(res.historicalAverageTipAmount());
        assertNotNull(res.estimatedMonthlyTipAmount());
        assertNotNull(res.projectedTipPercentage());
        assertNotNull(res.confidence());
        assertNotNull(res.monthlyBudget());
        assertNotNull(res.projectedBudgetUsage());
        assertNotNull(res.budgetStatus());
        assertNotNull(res.message());
    }

    // =============================================
    // 20. Median & Mean unit calculations
    // =============================================

    @Test
    void testCalculateMedian_singleValue() {
        BigDecimal result = service.calculateMedian(List.of(new BigDecimal("42.50")));
        assertEquals(new BigDecimal("42.50"), result);
    }

    @Test
    void testCalculateMean_singleValue() {
        BigDecimal result = service.calculateMean(List.of(new BigDecimal("42.50")));
        assertEquals(new BigDecimal("42.50"), result);
    }

    @Test
    void testCalculateMedian_empty() {
        assertEquals(BigDecimal.ZERO, service.calculateMedian(Collections.emptyList()));
    }

    @Test
    void testCalculateMean_empty() {
        assertEquals(BigDecimal.ZERO, service.calculateMean(Collections.emptyList()));
    }

    // =============================================
    // 21. Invalid currency
    // =============================================

    @Test
    void testInvalidCurrency_throws() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        assertThrows(IllegalArgumentException.class, () ->
                service.forecast(TEST_EMAIL, "XYZ", TipForecastPeriod.NEXT_30_DAYS, null, 90));
    }

    // Helper constant
    private static final BigDecimal HUNDRED = new BigDecimal("100");
}
