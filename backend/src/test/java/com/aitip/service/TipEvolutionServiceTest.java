package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TipEvolutionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TipRepository tipRepository;

    private GenerosityScoreService generosityScoreService;
    private TipEvolutionService tipEvolutionService;
    private Clock fixedClock;
    private User testUser;
    private UUID testUserId;

    // Fixed reference time: 2026-06-15 12:00:00 UTC
    private static final LocalDateTime REFERENCE_NOW = LocalDateTime.of(2026, 6, 15, 12, 0, 0);

    @BeforeEach
    void setUp() {
        generosityScoreService = new GenerosityScoreService(null, null);
        fixedClock = Clock.fixed(REFERENCE_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        tipEvolutionService = new TipEvolutionService(userRepository, tipRepository, generosityScoreService, fixedClock);

        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("user@example.com");

        lenient().when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
    }

    private Tip createTip(String currency, double amount, double percentage, String restaurant, ServiceQuality sq, LocalDateTime createdAt) {
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setUser(testUser);
        tip.setCurrency(currency);
        tip.setTipAmount(BigDecimal.valueOf(amount));
        tip.setTipPercentage(BigDecimal.valueOf(percentage));
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(createdAt);
        return tip;
    }

    // =========================================================================
    // 1. PERIODS
    // =========================================================================

    @Test
    @DisplayName("Period: LAST_3_MONTHS computes correct date boundaries")
    void testPeriodLast3MonthsBoundaries() {
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(Collections.emptyList());

        tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(tipRepository).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), startCaptor.capture(), endCaptor.capture());

        // For 2026-06 reference date, LAST_3_MONTHS should start 2026-04-01T00:00:00 and end 2026-07-01T00:00:00
        assertEquals(LocalDateTime.of(2026, 4, 1, 0, 0, 0), startCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0, 0), endCaptor.getValue());
    }

    @Test
    @DisplayName("Period: LAST_6_MONTHS computes correct date boundaries")
    void testPeriodLast6MonthsBoundaries() {
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(Collections.emptyList());

        tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_6_MONTHS);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(tipRepository).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), startCaptor.capture(), endCaptor.capture());

        // For 2026-06 reference date, LAST_6_MONTHS should start 2026-01-01T00:00:00 and end 2026-07-01T00:00:00
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), startCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0, 0), endCaptor.getValue());
    }

    @Test
    @DisplayName("Period: LAST_12_MONTHS computes correct date boundaries")
    void testPeriodLast12MonthsBoundaries() {
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(Collections.emptyList());

        tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_12_MONTHS);

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(tipRepository).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), startCaptor.capture(), endCaptor.capture());

        // For 2026-06, LAST_12_MONTHS starts 2025-07-01T00:00:00 and ends 2026-07-01T00:00:00
        assertEquals(LocalDateTime.of(2025, 7, 1, 0, 0, 0), startCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0, 0), endCaptor.getValue());
    }

    @Test
    @DisplayName("Period: ALL_TIME calls findAllByUserId")
    void testPeriodAllTime() {
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(Collections.emptyList());

        tipEvolutionResponseAssertion(tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.ALL_TIME));
        verify(tipRepository).findAllByUserId(testUserId);
    }

    @Test
    @DisplayName("Period: Default period is LAST_6_MONTHS when period parameter is null")
    void testDefaultPeriodIsLast6Months() {
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(Collections.emptyList());

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, null);
        assertEquals(TipEvolutionPeriod.LAST_6_MONTHS, res.period());
    }

    // =========================================================================
    // 2. MONTHLY GROUPING & BOUNDARIES
    // =========================================================================

    @Test
    @DisplayName("Monthly grouping: Same month tips are grouped together")
    void testSameMonthGrouping() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Cafe", ServiceQuality.GOOD, LocalDateTime.of(2026, 4, 5, 10, 0)),
                createTip("USD", 20.0, 18.0, "Diner", ServiceQuality.EXCELLENT, LocalDateTime.of(2026, 4, 25, 18, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(1, res.activeMonths());
        assertEquals(1, res.currencyTimelines().size());
        assertEquals(1, res.currencyTimelines().get(0).monthlyTimeline().size());
        assertEquals("2026-04", res.currencyTimelines().get(0).monthlyTimeline().get(0).month());
        assertEquals(2, res.currencyTimelines().get(0).monthlyTimeline().get(0).tipCount());
    }

    @Test
    @DisplayName("Monthly grouping: Different months produce separate timeline entries in chronological order")
    void testDifferentMonthsGrouping() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Cafe", ServiceQuality.GOOD, LocalDateTime.of(2026, 5, 5, 10, 0)),
                createTip("USD", 20.0, 18.0, "Diner", ServiceQuality.EXCELLENT, LocalDateTime.of(2026, 4, 25, 18, 0)),
                createTip("USD", 15.0, 12.0, "Bistro", ServiceQuality.AVERAGE, LocalDateTime.of(2026, 6, 1, 12, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(3, res.activeMonths());
        List<TipEvolutionMonth> timeline = res.currencyTimelines().get(0).monthlyTimeline();
        assertEquals(3, timeline.size());
        assertEquals("2026-04", timeline.get(0).month());
        assertEquals("2026-05", timeline.get(1).month());
        assertEquals("2026-06", timeline.get(2).month());
    }

    @Test
    @DisplayName("Monthly grouping: Year boundary December to January is handled cleanly")
    void testDecemberToJanuaryYearBoundary() {
        Clock janClock = Clock.fixed(LocalDateTime.of(2027, 1, 15, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        TipEvolutionService janService = new TipEvolutionService(userRepository, tipRepository, generosityScoreService, janClock);

        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Cafe", ServiceQuality.GOOD, LocalDateTime.of(2026, 12, 20, 10, 0)),
                createTip("USD", 20.0, 18.0, "Diner", ServiceQuality.EXCELLENT, LocalDateTime.of(2027, 1, 5, 18, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = janService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(2, res.activeMonths());
        List<TipEvolutionMonth> timeline = res.currencyTimelines().get(0).monthlyTimeline();
        assertEquals("2026-12", timeline.get(0).month());
        assertEquals("2027-01", timeline.get(1).month());
    }

    @Test
    @DisplayName("Monthly grouping: Empty month handling returns safe nulls")
    void testEmptyMonthSnapshot() {
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", Collections.emptyList());
        assertEquals("2026-04", month.month());
        assertEquals(0, month.tipCount());
        assertNull(month.medianTipPercentage());
        assertNull(month.averageTipPercentage());
        assertNull(month.consistencyScore());
        assertNull(month.tipStyle());
        assertEquals(0, month.uniqueRestaurantCount());
    }

    @Test
    @DisplayName("Monthly grouping: Month with one tip has identical min, max, avg, median")
    void testSingleTipMonth() {
        List<Tip> single = List.of(createTip("USD", 20.0, 15.0, "Cafe", ServiceQuality.GOOD, LocalDateTime.of(2026, 4, 10, 12, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", single);

        assertEquals(1, month.tipCount());
        assertEquals(new BigDecimal("15.0"), month.minTipPercentage());
        assertEquals(new BigDecimal("15.0"), month.maxTipPercentage());
        assertEquals(new BigDecimal("15.00"), month.averageTipPercentage());
        assertEquals(new BigDecimal("15.0"), month.medianTipPercentage());
        assertEquals(new BigDecimal("20.00"), month.averageTipAmount());
        assertEquals(new BigDecimal("20.0"), month.medianTipAmount());
        assertNull(month.consistencyScore());
        assertNull(month.behaviorType());
        assertNotNull(month.tipStyle());
    }

    // =========================================================================
    // 3. STATISTICS
    // =========================================================================

    @Test
    @DisplayName("Statistics: Correct mean, median, min, max calculation")
    void testStatisticsCalculations() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 10.0, "A", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 20.0, 15.0, "B", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 30.0, 20.0, "C", null, LocalDateTime.of(2026, 4, 3, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(new BigDecimal("10.0"), month.minTipPercentage());
        assertEquals(new BigDecimal("20.0"), month.maxTipPercentage());
        assertEquals(new BigDecimal("15.0"), month.medianTipPercentage());
        assertEquals(new BigDecimal("15.00"), month.averageTipPercentage());
        assertEquals(new BigDecimal("20.00"), month.averageTipAmount());
        assertEquals(new BigDecimal("20.0"), month.medianTipAmount());
    }

    @Test
    @DisplayName("Statistics: Even number of tips computes half-up average median")
    void testEvenTipsMedian() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 10.0, "A", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 20.0, 20.0, "B", null, LocalDateTime.of(2026, 4, 2, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(new BigDecimal("15.00"), month.medianTipPercentage());
        assertEquals(new BigDecimal("15.00"), month.medianTipAmount());
    }

    // =========================================================================
    // 4. CONSISTENCY
    // =========================================================================

    @Test
    @DisplayName("Consistency: Fewer than 5 tips returns null consistencyScore and null behaviorType")
    void testConsistencyUnder5Tips() {
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tips.add(createTip("USD", 10.0, 15.0, "Cafe", null, LocalDateTime.of(2026, 4, 1 + i, 10, 0)));
        }

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertNull(month.consistencyScore());
        assertNull(month.behaviorType());
    }

    @Test
    @DisplayName("Consistency: Exactly 5 identical tips gives 100 consistencyScore and VERY_CONSISTENT")
    void testConsistencyExactly5IdenticalTips() {
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tips.add(createTip("USD", 10.0, 15.0, "Cafe", null, LocalDateTime.of(2026, 4, 1 + i, 10, 0)));
        }

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(100, month.consistencyScore());
        assertEquals(TipBehaviorType.VERY_CONSISTENT, month.behaviorType());
    }

    @Test
    @DisplayName("Consistency: Variable tips produce reduced consistencyScore")
    void testConsistencyVariableTips() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 5.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 10.0, "Cafe", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 10.0, 20.0, "Cafe", null, LocalDateTime.of(2026, 4, 3, 10, 0)),
                createTip("USD", 10.0, 25.0, "Cafe", null, LocalDateTime.of(2026, 4, 4, 10, 0)),
                createTip("USD", 10.0, 30.0, "Cafe", null, LocalDateTime.of(2026, 4, 5, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertNotNull(month.consistencyScore());
        assertTrue(month.consistencyScore() < 70);
    }

    @Test
    @DisplayName("Consistency: Wildly fluctuating tips clamp score to 0")
    void testConsistencyScoreClampToZero() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 0.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 100.0, "Cafe", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 10.0, 0.0, "Cafe", null, LocalDateTime.of(2026, 4, 3, 10, 0)),
                createTip("USD", 10.0, 100.0, "Cafe", null, LocalDateTime.of(2026, 4, 4, 10, 0)),
                createTip("USD", 10.0, 0.0, "Cafe", null, LocalDateTime.of(2026, 4, 5, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(0, month.consistencyScore());
        assertEquals(TipBehaviorType.HIGHLY_VARIABLE, month.behaviorType());
    }

    // =========================================================================
    // 5. DIRECTION & TREND
    // =========================================================================

    @Test
    @DisplayName("Direction: MORE_GENEROUS when recent median - historical median >= +3%")
    void testDirectionMoreGenerous() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 10.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.MORE_GENEROUS, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: MORE_CONSERVATIVE when recent median - historical median <= -3%")
    void testDirectionMoreConservative() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 18.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 12.0, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.MORE_CONSERVATIVE, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: STABLE when recent median - historical median is between -3% and +3%")
    void testDirectionStable() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 16.0, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.STABLE, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: Exact +3.00% difference is MORE_GENEROUS")
    void testDirectionExactPlus3Boundary() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 12.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.MORE_GENEROUS, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: Exact -3.00% difference is MORE_CONSERVATIVE")
    void testDirectionExactMinus3Boundary() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 12.0, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.MORE_CONSERVATIVE, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: Odd number of active months allocates extra month to recent period")
    void testDirectionOddMonthsAllocation() {
        // 3 active months: Older = month 1, Recent = months 2 & 3
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 10.0, "M1", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 16.0, "M2", null, LocalDateTime.of(2026, 5, 1, 10, 0)),
                createTip("USD", 10.0, 18.0, "M3", null, LocalDateTime.of(2026, 6, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        // Older = 10%, Recent = [16, 18] -> median 17%. 17 - 10 = +7% -> MORE_GENEROUS
        assertEquals(TipEvolutionDirection.MORE_GENEROUS, res.overallDirection());
        assertEquals(new BigDecimal("10.0"), res.historicalMedianTipPercentage());
        assertEquals(new BigDecimal("17.00"), res.currentMedianTipPercentage());
    }

    @Test
    @DisplayName("Direction: Single active month results in INSUFFICIENT_DATA")
    void testDirectionSingleActiveMonth() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Cafe", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.INSUFFICIENT_DATA, res.overallDirection());
        assertNull(res.historicalMedianTipPercentage());
        assertEquals(new BigDecimal("15.0"), res.currentMedianTipPercentage());
    }

    // =========================================================================
    // 6. RESTAURANT DIVERSITY
    // =========================================================================

    @Test
    @DisplayName("Restaurant diversity: Case-insensitive and whitespace normalization")
    void testRestaurantNormalization() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Pizza Palace", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, " pizza palace ", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 10.0, 15.0, "PIZZA PALACE", null, LocalDateTime.of(2026, 4, 3, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(1, month.uniqueRestaurantCount());
    }

    @Test
    @DisplayName("Restaurant diversity: Null and blank restaurant names are ignored")
    void testRestaurantNullAndBlankIgnored() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, null, null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "   ", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 10.0, 15.0, "Bistro", null, LocalDateTime.of(2026, 4, 3, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(1, month.uniqueRestaurantCount());
    }

    @Test
    @DisplayName("Restaurant diversity: Distinct restaurants are counted accurately")
    void testMultipleDistinctRestaurants() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Bistro A", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "Bistro B", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 10.0, 15.0, "Bistro C", null, LocalDateTime.of(2026, 4, 3, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(3, month.uniqueRestaurantCount());
    }

    // =========================================================================
    // 7. SERVICE QUALITY EVOLUTION
    // =========================================================================

    @Test
    @DisplayName("Service quality: Correct monthly and overall averages")
    void testServiceQualityAverages() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 10.0, "A", ServiceQuality.GOOD, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 20.0, "B", ServiceQuality.GOOD, LocalDateTime.of(2026, 5, 1, 10, 0)),
                createTip("USD", 10.0, 25.0, "C", ServiceQuality.EXCELLENT, LocalDateTime.of(2026, 5, 2, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        List<TipServiceQualityEvolution> sqList = res.serviceQualityEvolution();

        assertEquals(2, sqList.size());

        TipServiceQualityEvolution goodSq = sqList.stream()
                .filter(s -> s.serviceQuality() == ServiceQuality.GOOD)
                .findFirst().orElseThrow();

        assertEquals(new BigDecimal("15.00"), goodSq.overallAverageTipPercentage());
        assertEquals(new BigDecimal("10.00"), goodSq.monthlyValues().get("2026-04"));
        assertEquals(new BigDecimal("20.00"), goodSq.monthlyValues().get("2026-05"));

        TipServiceQualityEvolution excSq = sqList.stream()
                .filter(s -> s.serviceQuality() == ServiceQuality.EXCELLENT)
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("25.00"), excSq.overallAverageTipPercentage());
        assertEquals(new BigDecimal("25.00"), excSq.monthlyValues().get("2026-05"));
    }

    @Test
    @DisplayName("Service quality: Tips with null service quality are ignored")
    void testServiceQualityNullIgnored() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "A", null, LocalDateTime.of(2026, 4, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertTrue(res.serviceQualityEvolution().isEmpty());
    }

    // =========================================================================
    // 8. CURRENCY ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Currency: Separate timelines for INR and USD without monetary mixing")
    void testCurrencyIsolationMultipleCurrencies() {
        List<Tip> tips = List.of(
                createTip("INR", 100.0, 10.0, "Dhaba", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 15.0, 20.0, "Diner", null, LocalDateTime.of(2026, 4, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(2, res.currencyTimelines().size());

        TipCurrencyEvolution inrTimeline = res.currencyTimelines().stream()
                .filter(c -> "INR".equals(c.currency())).findFirst().orElseThrow();
        TipCurrencyEvolution usdTimeline = res.currencyTimelines().stream()
                .filter(c -> "USD".equals(c.currency())).findFirst().orElseThrow();

        assertEquals(new BigDecimal("100.00"), inrTimeline.monthlyTimeline().get(0).averageTipAmount());
        assertEquals(new BigDecimal("15.00"), usdTimeline.monthlyTimeline().get(0).averageTipAmount());
    }

    @Test
    @DisplayName("Currency: Currency filter loads only specified currency")
    void testCurrencyFilterParameter() {
        List<Tip> inrTips = List.of(
                createTip("INR", 100.0, 10.0, "Dhaba", null, LocalDateTime.of(2026, 4, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(testUserId), eq("INR"), any(), any()))
                .thenReturn(inrTips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", "INR", TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals("INR", res.currency());
        assertEquals(1, res.currencyTimelines().size());
        assertEquals("INR", res.currencyTimelines().get(0).currency());
    }

    @Test
    @DisplayName("Currency: Invalid currency code throws IllegalArgumentException")
    void testInvalidCurrencyCodeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                tipEvolutionService.getEvolution("user@example.com", "INVALID_CODE", TipEvolutionPeriod.LAST_3_MONTHS));
    }

    // =========================================================================
    // 9. SECURITY & USER ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Security: User not found throws ResourceNotFoundException")
    void testUserNotFoundThrows() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                tipEvolutionService.getEvolution("unknown@example.com", null, TipEvolutionPeriod.LAST_6_MONTHS));
    }

    @Test
    @DisplayName("Security: Tips queried strictly with authenticated user's ID")
    void testUserIsolationQueryEnforcement() {
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(Collections.emptyList());

        tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_6_MONTHS);

        verify(tipRepository).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any());
        verify(tipRepository, never()).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(UUID.randomUUID()), any(), any());
    }

    // =========================================================================
    // 10. EMPTY & LIMITED STATES
    // =========================================================================

    @Test
    @DisplayName("Empty state: Zero tips returns empty response and INSUFFICIENT_DATA")
    void testZeroTipsState() {
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(Collections.emptyList());

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_6_MONTHS);

        assertEquals(0, res.totalTipCount());
        assertEquals(0, res.activeMonths());
        assertEquals(TipEvolutionDirection.INSUFFICIENT_DATA, res.overallDirection());
        assertTrue(res.currencyTimelines().isEmpty());
        assertTrue(res.serviceQualityEvolution().isEmpty());
        assertEquals("Add some tips to start seeing how your tipping behavior changes over time.", res.message());
    }

    @Test
    @DisplayName("Limited state: One active month prompts user to keep tipping")
    void testOneActiveMonthMessage() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(1, res.activeMonths());
        assertEquals("Keep tipping across multiple months to unlock your behavioral evolution.", res.message());
    }

    // =========================================================================
    // 11. MONTHLY TIP STYLE
    // =========================================================================

    @Test
    @DisplayName("TipStyle: Conservative style when median tip % <= 9.75% (score <= 39)")
    void testConservativeTipStyle() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 8.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.CONSERVATIVE, month.tipStyle());
    }

    @Test
    @DisplayName("TipStyle: Moderate style when median tip % around 12% (score 40-59)")
    void testModerateTipStyle() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 12.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.MODERATE, month.tipStyle());
    }

    @Test
    @DisplayName("TipStyle: Generous style when median tip % around 18% (score 60-79)")
    void testGenerousTipStyle() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 18.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.GENEROUS, month.tipStyle());
    }

    @Test
    @DisplayName("TipStyle: Very generous style when median tip % >= 20% (score >= 80)")
    void testVeryGenerousTipStyle() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 22.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.VERY_GENEROUS, month.tipStyle());
    }

    // =========================================================================
    // 12. PROMPT BUILDER
    // =========================================================================

    @Test
    @DisplayName("AI: TipEvolutionPromptBuilder produces facts prompt without altering facts")
    void testPromptBuilder() {
        TipEvolutionPromptBuilder promptBuilder = new TipEvolutionPromptBuilder(new com.fasterxml.jackson.databind.ObjectMapper());

        TipEvolutionResponse res = new TipEvolutionResponse(
                LocalDateTime.of(2026, 6, 15, 12, 0),
                TipEvolutionPeriod.LAST_6_MONTHS,
                "USD",
                10,
                2,
                TipEvolutionDirection.MORE_GENEROUS,
                new BigDecimal("18.00"),
                new BigDecimal("17.50"),
                new BigDecimal("12.00"),
                new BigDecimal("12.00"),
                Collections.emptyList(),
                Collections.emptyList(),
                "Ready",
                "Existing AI explanation that must be excluded"
        );

        String prompt = promptBuilder.buildPrompt(res);
        assertNotNull(prompt);
        assertTrue(prompt.contains("CRITICAL RULES:"));
        assertTrue(prompt.contains("MORE_GENEROUS"));
        assertFalse(prompt.contains("Existing AI explanation that must be excluded"));
    }

    // =========================================================================
    // 13. ADDITIONAL TESTS (Covering All Edge Cases)
    // =========================================================================

    @Test
    @DisplayName("Period: ALL_TIME with currency filter properly isolates tips by currency")
    void testAllTimeWithCurrencyFilter() {
        List<Tip> allTips = List.of(
                createTip("USD", 10.0, 15.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("EUR", 20.0, 20.0, "Bistro", null, LocalDateTime.of(2026, 4, 2, 10, 0))
        );
        when(tipRepository.findAllByUserId(testUserId)).thenReturn(allTips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", "USD", TipEvolutionPeriod.ALL_TIME);
        assertEquals("USD", res.currency());
        assertEquals(1, res.totalTipCount());
        assertEquals(1, res.currencyTimelines().size());
        assertEquals("USD", res.currencyTimelines().get(0).currency());
    }

    @Test
    @DisplayName("Direction: Trend near +3.00 boundary (+2.90%) is STABLE")
    void testTrendNearPlus3BoundaryIsStable() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 17.9, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.STABLE, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: Trend near -3.00 boundary (-2.90%) is STABLE")
    void testTrendNearMinus3BoundaryIsStable() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 12.1, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.STABLE, res.overallDirection());
    }

    @Test
    @DisplayName("Direction: Two months with exactly identical medians (diff 0.00%) is STABLE")
    void testDirectionIdenticalMediansIsStable() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Old", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "New", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(TipEvolutionDirection.STABLE, res.overallDirection());
    }

    @Test
    @DisplayName("Service Quality: All four service qualities handled accurately")
    void testAllFourServiceQualities() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 5.0, "A", ServiceQuality.POOR, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 10.0, "B", ServiceQuality.AVERAGE, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "C", ServiceQuality.GOOD, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 20.0, "D", ServiceQuality.EXCELLENT, LocalDateTime.of(2026, 4, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals(4, res.serviceQualityEvolution().size());
    }

    @Test
    @DisplayName("Consistency: Evaluated correctly for 6 tips")
    void testConsistencyEvaluatedFor6Tips() {
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tips.add(createTip("USD", 10.0, 18.0, "Cafe", null, LocalDateTime.of(2026, 4, 1 + i, 10, 0)));
        }

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertNotNull(month.consistencyScore());
        assertEquals(100, month.consistencyScore());
        assertEquals(TipBehaviorType.VERY_CONSISTENT, month.behaviorType());
    }

    @Test
    @DisplayName("TipStyle: Exactly 15.00% median maps to GENEROUS")
    void testGenerosityBoundaryExactly15() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 15.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.GENEROUS, month.tipStyle());
    }

    @Test
    @DisplayName("TipStyle: Exactly 10.00% median maps to MODERATE")
    void testGenerosityBoundaryExactly10() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 10.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.MODERATE, month.tipStyle());
    }

    @Test
    @DisplayName("TipStyle: Exactly 20.00% median maps to VERY_GENEROUS")
    void testGenerosityBoundaryExactly20() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 20.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.VERY_GENEROUS, month.tipStyle());
    }

    @Test
    @DisplayName("TipStyle: Below 10.00% median maps to CONSERVATIVE")
    void testGenerosityBoundaryBelow10() {
        List<Tip> tips = List.of(createTip("USD", 10.0, 9.0, "Cafe", null, LocalDateTime.of(2026, 4, 1, 10, 0)));
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(TipStyle.CONSERVATIVE, month.tipStyle());
    }

    @Test
    @DisplayName("Restaurant diversity: Varied whitespace, casing, and special characters")
    void testRestaurantDiversitySpecialNames() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "Joe's Diner", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 15.0, "  joe's diner  ", null, LocalDateTime.of(2026, 4, 2, 10, 0)),
                createTip("USD", 10.0, 15.0, "JOE'S DINER", null, LocalDateTime.of(2026, 4, 3, 10, 0)),
                createTip("USD", 10.0, 15.0, "Luigi's", null, LocalDateTime.of(2026, 4, 4, 10, 0))
        );

        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", tips);
        assertEquals(2, month.uniqueRestaurantCount());
    }

    @Test
    @DisplayName("Constructor: 3-argument constructor successfully instantiated with default clock")
    void testThreeArgConstructorInstantiates() {
        TipEvolutionService defaultService = new TipEvolutionService(userRepository, tipRepository, generosityScoreService);
        assertNotNull(defaultService);
    }

    @Test
    @DisplayName("Service Quality: Null service qualities ignored across all months")
    void testServiceQualityNullIgnoredMultipleMonths() {
        List<Tip> tips = List.of(
                createTip("USD", 10.0, 15.0, "A", null, LocalDateTime.of(2026, 4, 1, 10, 0)),
                createTip("USD", 10.0, 18.0, "B", null, LocalDateTime.of(2026, 5, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUserId), any(), any()))
                .thenReturn(tips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", null, TipEvolutionPeriod.LAST_3_MONTHS);
        assertTrue(res.serviceQualityEvolution().isEmpty());
    }

    @Test
    @DisplayName("Currency: Lowercase currency filter is normalized to uppercase")
    void testCurrencyFilterLowercaseNormalized() {
        List<Tip> inrTips = List.of(
                createTip("INR", 100.0, 10.0, "Dhaba", null, LocalDateTime.of(2026, 4, 1, 10, 0))
        );
        when(tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(testUserId), eq("INR"), any(), any()))
                .thenReturn(inrTips);

        TipEvolutionResponse res = tipEvolutionService.getEvolution("user@example.com", "inr", TipEvolutionPeriod.LAST_3_MONTHS);
        assertEquals("INR", res.currency());
        assertEquals(1, res.currencyTimelines().size());
    }

    @Test
    @DisplayName("Monthly grouping: Null tips list in calculateMonthSnapshot safely returns 0 count")
    void testCalculateMonthSnapshotNullTips() {
        TipEvolutionMonth month = tipEvolutionService.calculateMonthSnapshot("2026-04", null);
        assertEquals(0, month.tipCount());
        assertNull(month.medianTipPercentage());
    }

    private void tipEvolutionResponseAssertion(TipEvolutionResponse response) {
        assertNotNull(response);
        assertEquals(0, response.totalTipCount());
    }
}
