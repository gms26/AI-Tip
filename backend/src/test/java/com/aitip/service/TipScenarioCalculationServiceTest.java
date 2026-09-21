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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipScenarioCalculationServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TipScenarioCalculationService service;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setup() {
        testUser = User.builder()
                .name("Test User")
                .email(TEST_EMAIL)
                .password("encoded")
                .build();
        try {
            var idField = testUser.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Tip createTip(BigDecimal tipAmount, BigDecimal tipPercentage, String currency) {
        Tip tip = new Tip();
        tip.setUser(testUser);
        tip.setBillAmount(new BigDecimal("100.00"));
        tip.setTipAmount(tipAmount);
        tip.setTipPercentage(tipPercentage);
        tip.setTotalAmount(new BigDecimal("100.00").add(tipAmount));
        tip.setCurrency(currency);
        tip.setRestaurantName("Test Restaurant");
        tip.setCreatedAt(LocalDateTime.now().minusDays(5));
        return tip;
    }

    private void setupMock(List<Tip> tips) {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                any(UUID.class), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(tips);
    }

    private TipScenarioRequest makeRequest(String currency, String billAmount,
                                           List<BigDecimal> percentages, BigDecimal budget) {
        return new TipScenarioRequest(currency, new BigDecimal(billAmount), percentages, budget);
    }

    // =============================================
    // 1. Basic calculation tests
    // =============================================

    @Test
    void testBasicCalculation_10Percent() {
        setupMock(Collections.emptyList());
        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("10")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals(1, res.scenarioResults().size());
        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("10"), r.tipPercentage());
        assertEquals(new BigDecimal("100.00"), r.tipAmount());
        assertEquals(new BigDecimal("1100.00"), r.totalAmount());
    }

    @Test
    void testBasicCalculation_15Percent() {
        setupMock(Collections.emptyList());
        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("15")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("150.00"), r.tipAmount());
        assertEquals(new BigDecimal("1150.00"), r.totalAmount());
    }

    @Test
    void testBasicCalculation_20Percent() {
        setupMock(Collections.emptyList());
        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("20")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("200.00"), r.tipAmount());
        assertEquals(new BigDecimal("1200.00"), r.totalAmount());
    }

    @Test
    void testBasicCalculation_zeroPercent() {
        setupMock(Collections.emptyList());
        TipScenarioRequest req = makeRequest("USD", "500",
                List.of(BigDecimal.ZERO), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("0.00"), r.tipAmount());
        assertEquals(new BigDecimal("500.00"), r.totalAmount());
    }

    @Test
    void testBasicCalculation_100Percent() {
        setupMock(Collections.emptyList());
        TipScenarioRequest req = makeRequest("USD", "200",
                List.of(new BigDecimal("100")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("200.00"), r.tipAmount());
        assertEquals(new BigDecimal("400.00"), r.totalAmount());
    }

    @Test
    void testBasicCalculation_decimalPercentage() {
        setupMock(Collections.emptyList());
        TipScenarioRequest req = makeRequest("USD", "100",
                List.of(new BigDecimal("12.5")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("12.50"), r.tipAmount());
        assertEquals(new BigDecimal("112.50"), r.totalAmount());
    }

    // =============================================
    // 2. BigDecimal rounding
    // =============================================

    @Test
    void testRounding_fractionalBill() {
        setupMock(Collections.emptyList());
        // 33.33 Ãƒâ€” 15% = 4.9995 Ã¢â€ â€™ 5.00 (HALF_UP)
        TipScenarioRequest req = makeRequest("USD", "33.33",
                List.of(new BigDecimal("15")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("5.00"), r.tipAmount());
        assertEquals(new BigDecimal("38.33"), r.totalAmount());
    }

    @Test
    void testRounding_halfUp() {
        setupMock(Collections.emptyList());
        // 77.77 Ãƒâ€” 13% = 10.1101 Ã¢â€ â€™ 10.11 (HALF_UP)
        TipScenarioRequest req = makeRequest("USD", "77.77",
                List.of(new BigDecimal("13")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("10.11"), r.tipAmount());
    }

    // =============================================
    // 3. Historical comparison
    // =============================================

    @Test
    void testHistoricalComparison_median() {
        // Median of 10, 15, 20 = 15
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD"),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("20")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals(new BigDecimal("15.00"), res.historicalMedianTipPercentage());
        TipScenarioResult r = res.scenarioResults().get(0);
        // 20 - 15 = +5.00 percentage points
        assertEquals(new BigDecimal("5.00"), r.differenceFromHistorical());
    }

    @Test
    void testHistoricalComparison_negativeDifference() {
        // Median of 15, 20, 25 = 20
        List<Tip> tips = List.of(
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD"),
                createTip(new BigDecimal("25.00"), new BigDecimal("25.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("10")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        // 10 - 20 = -10.00 percentage points
        assertEquals(new BigDecimal("-10.00"), r.differenceFromHistorical());
    }

    @Test
    void testHistoricalComparison_monetaryDifference() {
        // Median = 15, bill = 1000
        // Historical tip for bill = 1000 Ãƒâ€” 15/100 = 150
        // Scenario 20% Ã¢â€ â€™ 200, diff = 200 - 150 = +50
        List<Tip> tips = List.of(
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("20")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("50.00"), r.monetaryDifference());
    }

    @Test
    void testHistoricalComparison_evenTipCount() {
        // Median of 10, 15, 20, 25 = (15 + 20) / 2 = 17.50
        List<Tip> tips = List.of(
                createTip(new BigDecimal("10.00"), new BigDecimal("10.00"), "USD"),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD"),
                createTip(new BigDecimal("25.00"), new BigDecimal("25.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("17.5")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals(new BigDecimal("17.50"), res.historicalMedianTipPercentage());
        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(new BigDecimal("0.00"), r.differenceFromHistorical());
    }

    @Test
    void testHistoricalComparison_noHistory() {
        setupMock(Collections.emptyList());

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("15")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals(0, res.historicalTipCount());
        assertNull(res.historicalMedianTipPercentage());
        assertNull(res.historicalAverageTipPercentage());
        assertNull(res.historicalAverageTipAmount());

        TipScenarioResult r = res.scenarioResults().get(0);
        assertNull(r.differenceFromHistorical());
        assertNull(r.monetaryDifference());
        // Tip calculation still works
        assertEquals(new BigDecimal("150.00"), r.tipAmount());
        assertEquals(new BigDecimal("1150.00"), r.totalAmount());
    }

    // =============================================
    // 4. Currency isolation
    // =============================================

    @Test
    void testCurrencyIsolation() {
        // Mock returns INR tips only (DB filters by currency)
        List<Tip> inrTips = List.of(
                createTip(new BigDecimal("100.00"), new BigDecimal("10.00"), "INR")
        );
        setupMock(inrTips);

        TipScenarioRequest req = makeRequest("INR", "1000",
                List.of(new BigDecimal("15")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals("INR", res.currency());
        assertEquals(1, res.historicalTipCount());
    }

    // =============================================
    // 5. Budget projection
    // =============================================

    @Test
    void testBudgetProjection_underBudget() {
        // 3 tips in 90 days, avg tip = 15, median = 15%
        // dailyRate = 3/90 = 0.033333
        // Scenario 10% on bill 1000 Ã¢â€ â€™ tip = 100
        // scalingRatio = 100 / 15 = 6.666667
        // monthlyProjected = 0.033333 Ãƒâ€” 30 Ãƒâ€” 6.666667 Ãƒâ€” 15 = 100.00
        List<Tip> tips = List.of(
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD"),
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("10")), new BigDecimal("5000"));

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertNotNull(r.monthlyProjectedTipAmount());
        assertNotNull(r.monthlyBudgetUsagePercentage());
        assertEquals(TipBudgetStatus.UNDER_BUDGET, r.budgetStatus());
    }

    @Test
    void testBudgetProjection_overBudget() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("50.00"), new BigDecimal("50.00"), "USD"),
                createTip(new BigDecimal("50.00"), new BigDecimal("50.00"), "USD"),
                createTip(new BigDecimal("50.00"), new BigDecimal("50.00"), "USD")
        );
        setupMock(tips);

        // Scenario 80% on bill 1000 Ã¢â€ â€™ large projected spending vs small budget
        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("80")), new BigDecimal("10"));

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertEquals(TipBudgetStatus.OVER_BUDGET, r.budgetStatus());
    }

    @Test
    void testBudgetProjection_noBudgetSupplied() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("15")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertNull(r.monthlyProjectedTipAmount());
        assertNull(r.monthlyBudgetUsagePercentage());
        assertNull(r.budgetStatus());
    }

    @Test
    void testBudgetProjection_noHistoryWithBudget() {
        setupMock(Collections.emptyList());

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("15")), new BigDecimal("5000"));

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        TipScenarioResult r = res.scenarioResults().get(0);
        assertNull(r.monthlyProjectedTipAmount());
        assertNull(r.monthlyBudgetUsagePercentage());
        assertNull(r.budgetStatus());
        assertNotNull(res.monthlyBudget());
    }

    @Test
    void testBudgetProjection_historicalAvgTipAmountZero() {
        // Edge case: all tips have tipAmount = 0
        List<Tip> tips = List.of(
                createTip(BigDecimal.ZERO, new BigDecimal("0.00"), "USD"),
                createTip(BigDecimal.ZERO, new BigDecimal("0.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "1000",
                List.of(new BigDecimal("15")), new BigDecimal("500"));

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        // Monthly projection unavailable when historicalAvgTipAmount == 0
        TipScenarioResult r = res.scenarioResults().get(0);
        assertNull(r.monthlyProjectedTipAmount());
        assertNull(r.monthlyBudgetUsagePercentage());
        assertNull(r.budgetStatus());
    }

    // =============================================
    // 6. Multiple scenarios & order preservation
    // =============================================

    @Test
    void testMultipleScenarios_orderPreserved() {
        setupMock(Collections.emptyList());
        List<BigDecimal> percentages = List.of(
                new BigDecimal("20"),
                new BigDecimal("10"),
                new BigDecimal("15"),
                new BigDecimal("5")
        );

        TipScenarioRequest req = makeRequest("USD", "1000", percentages, null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals(4, res.scenarioResults().size());
        assertEquals(new BigDecimal("20"), res.scenarioResults().get(0).tipPercentage());
        assertEquals(new BigDecimal("10"), res.scenarioResults().get(1).tipPercentage());
        assertEquals(new BigDecimal("15"), res.scenarioResults().get(2).tipPercentage());
        assertEquals(new BigDecimal("5"), res.scenarioResults().get(3).tipPercentage());
    }

    @Test
    void testMultipleScenarios_fiveMax() {
        setupMock(Collections.emptyList());
        List<BigDecimal> percentages = List.of(
                new BigDecimal("5"), new BigDecimal("10"),
                new BigDecimal("15"), new BigDecimal("20"),
                new BigDecimal("25")
        );

        TipScenarioRequest req = makeRequest("USD", "100", percentages, null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals(5, res.scenarioResults().size());
    }

    // =============================================
    // 7. Validation
    // =============================================

    @Test
    void testValidation_invalidCurrency() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        TipScenarioRequest req = makeRequest("XYZ", "100",
                List.of(new BigDecimal("10")), null);

        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(TEST_EMAIL, req));
    }

    @Test
    void testValidation_negativeBudget() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        TipScenarioRequest req = makeRequest("USD", "100",
                List.of(new BigDecimal("10")), new BigDecimal("-1"));

        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(TEST_EMAIL, req));
    }

    @Test
    void testValidation_zeroBudget() {
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        TipScenarioRequest req = makeRequest("USD", "100",
                List.of(new BigDecimal("10")), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () ->
                service.calculate(TEST_EMAIL, req));
    }

    // =============================================
    // 8. Response structure
    // =============================================

    @Test
    void testResponseStructure_withHistory() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("20.00"), new BigDecimal("20.00"), "USD"),
                createTip(new BigDecimal("25.00"), new BigDecimal("25.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "500",
                List.of(new BigDecimal("15"), new BigDecimal("20")),
                new BigDecimal("1000"));

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals("USD", res.currency());
        assertEquals(new BigDecimal("500"), res.billAmount());
        assertNotNull(res.historicalMedianTipPercentage());
        assertNotNull(res.historicalAverageTipPercentage());
        assertNotNull(res.historicalAverageTipAmount());
        assertEquals(2, res.historicalTipCount());
        assertEquals(2, res.scenarioResults().size());
        assertNotNull(res.monthlyBudget());
        assertNotNull(res.historicalMonthlyTipAmount());
        assertNotNull(res.message());
        assertNull(res.aiExplanation()); // Not populated by service
    }

    @Test
    void testResponseStructure_noHistory() {
        setupMock(Collections.emptyList());

        TipScenarioRequest req = makeRequest("EUR", "100",
                List.of(new BigDecimal("10")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertEquals("EUR", res.currency());
        assertEquals(0, res.historicalTipCount());
        assertNull(res.historicalMedianTipPercentage());
        assertNull(res.historicalAverageTipPercentage());
        assertNull(res.historicalAverageTipAmount());
        assertNull(res.historicalMonthlyTipAmount());
        assertTrue(res.message().contains("No EUR tipping history"));
    }

    // =============================================
    // 9. Budget status transitions
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
    }

    // =============================================
    // 10. Median & Mean unit tests
    // =============================================

    @Test
    void testCalculateMedian_oddCount() {
        BigDecimal result = service.calculateMedian(
                List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")));
        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void testCalculateMedian_evenCount() {
        BigDecimal result = service.calculateMedian(
                List.of(new BigDecimal("10"), new BigDecimal("20"),
                        new BigDecimal("30"), new BigDecimal("40")));
        assertEquals(new BigDecimal("25.00"), result);
    }

    @Test
    void testCalculateMedian_empty() {
        assertEquals(BigDecimal.ZERO, service.calculateMedian(Collections.emptyList()));
    }

    @Test
    void testCalculateMean_basic() {
        BigDecimal result = service.calculateMean(
                List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")));
        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void testCalculateMean_empty() {
        assertEquals(BigDecimal.ZERO, service.calculateMean(Collections.emptyList()));
    }

    // =============================================
    // 11. Message formatting
    // =============================================

    @Test
    void testMessage_withHistory() {
        List<Tip> tips = List.of(
                createTip(new BigDecimal("15.00"), new BigDecimal("15.00"), "USD")
        );
        setupMock(tips);

        TipScenarioRequest req = makeRequest("USD", "100",
                List.of(new BigDecimal("10")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertTrue(res.message().contains("historical median"));
        assertTrue(res.message().contains("hypothetical"));
    }

    @Test
    void testMessage_noHistory() {
        setupMock(Collections.emptyList());

        TipScenarioRequest req = makeRequest("USD", "100",
                List.of(new BigDecimal("10")), null);

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertTrue(res.message().contains("No USD tipping history"));
    }

    @Test
    void testMessage_budgetNoHistory() {
        setupMock(Collections.emptyList());

        TipScenarioRequest req = makeRequest("USD", "100",
                List.of(new BigDecimal("10")), new BigDecimal("500"));

        TipScenarioResponse res = service.calculate(TEST_EMAIL, req);

        assertTrue(res.message().contains("unavailable without historical data"));
    }
}
