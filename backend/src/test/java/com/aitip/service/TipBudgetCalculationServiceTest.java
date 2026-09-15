package com.aitip.service;

import com.aitip.dto.BudgetConfidence;
import com.aitip.dto.TipBudgetStatus;
import com.aitip.dto.TipBudgetStatusResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.TipBudget;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TipBudgetCalculationService}.
 *
 * All calculations are deterministic. No AI involvement.
 */
@ExtendWith(MockitoExtension.class)
class TipBudgetCalculationServiceTest {

    @Mock
    private TipRepository tipRepository;

    @InjectMocks
    private TipBudgetCalculationService calculationService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("alice@example.com").build();
    }

    private TipBudget createBudget(String currency, BigDecimal limit, BigDecimal threshold) {
        return TipBudget.builder()
                .id(UUID.randomUUID())
                .user(user)
                .currency(currency)
                .monthlyLimit(limit)
                .warningThreshold(threshold)
                .build();
    }

    private Tip createTip(BigDecimal tipAmount, String currency) {
        return Tip.builder()
                .id(UUID.randomUUID())
                .user(user)
                .billAmount(new BigDecimal("50.00"))
                .tipPercentage(new BigDecimal("15"))
                .tipAmount(tipAmount)
                .totalAmount(new BigDecimal("50.00").add(tipAmount))
                .currency(currency)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // === Status Tests ===

    @Test
    void zeroTips_returnsNoHistory() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(Collections.emptyList());

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(TipBudgetStatus.NO_HISTORY, result.status());
        assertEquals(0, result.tipCount());
        assertEquals(BigDecimal.ZERO, result.currentMonthTips());
        assertEquals(new BigDecimal("100.00"), result.remainingBudget());
    }

    @Test
    void spendingBelowThreshold_returnsUnderBudget() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(createTip(new BigDecimal("20.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(TipBudgetStatus.UNDER_BUDGET, result.status());
        assertEquals(new BigDecimal("80.00"), result.remainingBudget());
        assertEquals(0, new BigDecimal("20.00").compareTo(result.percentageUsed()));
    }

    @Test
    void spendingExactlyAtWarningThreshold_returnsApproachingLimit() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(createTip(new BigDecimal("80.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(TipBudgetStatus.APPROACHING_LIMIT, result.status());
        assertEquals(0, new BigDecimal("80.00").compareTo(result.percentageUsed()));
    }

    @Test
    void spendingBetweenThresholdAnd100_returnsApproachingLimit() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(createTip(new BigDecimal("90.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(TipBudgetStatus.APPROACHING_LIMIT, result.status());
        assertEquals(new BigDecimal("10.00"), result.remainingBudget());
    }

    @Test
    void spendingExactly100Percent_returnsLimitReached() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(createTip(new BigDecimal("100.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(TipBudgetStatus.LIMIT_REACHED, result.status());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.remainingBudget()));
        assertEquals(0, new BigDecimal("100.00").compareTo(result.percentageUsed()));
    }

    @Test
    void spendingAbove100Percent_returnsOverBudget() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(createTip(new BigDecimal("120.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(TipBudgetStatus.OVER_BUDGET, result.status());
        assertTrue(result.remainingBudget().compareTo(BigDecimal.ZERO) < 0,
                "Remaining budget should be negative when over budget");
        assertEquals(0, new BigDecimal("120.00").compareTo(result.percentageUsed()));
    }

    @Test
    void remainingBudgetCalculation_positiveAndNegative() {
        TipBudget budget = createBudget("USD", new BigDecimal("50.00"), new BigDecimal("80"));

        // Under budget
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(List.of(createTip(new BigDecimal("30.00"), "USD")));

        TipBudgetStatusResponse under = calculationService.calculateStatus(budget, userId);
        assertEquals(new BigDecimal("20.00"), under.remainingBudget());

        // Over budget
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(List.of(createTip(new BigDecimal("75.00"), "USD")));

        TipBudgetStatusResponse over = calculationService.calculateStatus(budget, userId);
        assertEquals(new BigDecimal("-25.00"), over.remainingBudget());
    }

    @Test
    void percentageCalculation_roundsHalfUp() {
        // 33.33 / 100 * 100 = 33.33  (exact)
        // 1.00 / 3.00 * 100 = 33.33  (rounded HALF_UP to 2dp)
        TipBudget budget = createBudget("USD", new BigDecimal("3.00"), new BigDecimal("80"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(List.of(createTip(new BigDecimal("1.00"), "USD")));

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(0, new BigDecimal("33.33").compareTo(result.percentageUsed()));
    }

    @Test
    void currencyIsolation_usdTipsDoNotAffectInrBudget() {
        // INR budget, but only USD tips exist — DB query returns empty
        TipBudget inrBudget = createBudget("INR", new BigDecimal("5000.00"), new BigDecimal("80"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("INR"), any(), any()))
                .thenReturn(Collections.emptyList());

        TipBudgetStatusResponse result = calculationService.calculateStatus(inrBudget, userId);

        assertEquals(TipBudgetStatus.NO_HISTORY, result.status());
        assertEquals(BigDecimal.ZERO, result.currentMonthTips());
    }

    // === Confidence Tests ===

    @Test
    void confidenceLow_zeroTips() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(Collections.emptyList());

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);
        assertEquals(BudgetConfidence.LOW, result.confidence());
    }

    @Test
    void confidenceLow_oneTip() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(List.of(createTip(new BigDecimal("5.00"), "USD")));

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);
        assertEquals(BudgetConfidence.LOW, result.confidence());
    }

    @Test
    void confidenceMedium_twoTips() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(
                createTip(new BigDecimal("5.00"), "USD"),
                createTip(new BigDecimal("5.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);
        assertEquals(BudgetConfidence.MEDIUM, result.confidence());
    }

    @Test
    void confidenceMedium_fourTips() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = IntStream.range(0, 4)
                .mapToObj(i -> createTip(new BigDecimal("5.00"), "USD"))
                .toList();
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);
        assertEquals(BudgetConfidence.MEDIUM, result.confidence());
    }

    @Test
    void confidenceHigh_fivePlusTips() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = IntStream.range(0, 5)
                .mapToObj(i -> createTip(new BigDecimal("5.00"), "USD"))
                .toList();
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);
        assertEquals(BudgetConfidence.HIGH, result.confidence());
    }

    // === Message Tests ===

    @Test
    void messageContainsCurrencyAndStatus() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(List.of(createTip(new BigDecimal("50.00"), "USD")));

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertNotNull(result.message());
        assertTrue(result.message().contains("USD"));
    }

    // === Multiple tips sum correctly ===

    @Test
    void multipleTips_summedCorrectly() {
        TipBudget budget = createBudget("USD", new BigDecimal("100.00"), new BigDecimal("80"));
        List<Tip> tips = List.of(
                createTip(new BigDecimal("25.00"), "USD"),
                createTip(new BigDecimal("30.00"), "USD"),
                createTip(new BigDecimal("15.00"), "USD"));
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(userId), eq("USD"), any(), any()))
                .thenReturn(tips);

        TipBudgetStatusResponse result = calculationService.calculateStatus(budget, userId);

        assertEquals(0, new BigDecimal("70.00").compareTo(result.currentMonthTips()));
        assertEquals(0, new BigDecimal("30.00").compareTo(result.remainingBudget()));
        assertEquals(0, new BigDecimal("70.00").compareTo(result.percentageUsed()));
        assertEquals(TipBudgetStatus.UNDER_BUDGET, result.status());
        assertEquals(BudgetConfidence.MEDIUM, result.confidence());
    }
}
