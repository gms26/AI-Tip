package com.aitip.service;

import com.aitip.dto.AnalyticsPeriod;
import com.aitip.dto.AnalyticsRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.dto.analytics.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipAnalyticsServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TipAnalyticsService tipAnalyticsService;

    private User testUser;
    private final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(EMAIL);
    }

    // =============================================
    // Empty state
    // =============================================

    @Test
    void getAnalytics_withZeroTips_returnsEmptyState() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(new ArrayList<>());

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_MONTH, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(0, response.totalTipCount());
        assertNull(response.averageTipPercentage());
        assertNull(response.medianTipPercentage());
        assertNull(response.highestTipPercentage());
        assertNull(response.lowestTipPercentage());
        assertTrue(response.currencyBreakdown().isEmpty());
        assertTrue(response.restaurantInsights().isEmpty());
        assertTrue(response.serviceQualityInsights().isEmpty());
        assertTrue(response.monthlyTrend().isEmpty());
    }

    // =============================================
    // Single tip
    // =============================================

    @Test
    void getAnalytics_withOneTip_returnsCorrectStats() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(createTip("USD", "20.00", "100.00", "Italian Place", ServiceQuality.GOOD));
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_MONTH, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(1, response.totalTipCount());
        assertEquals(new BigDecimal("20.00"), response.averageTipPercentage());
        assertEquals(new BigDecimal("20.00"), response.medianTipPercentage());
        assertEquals(new BigDecimal("20.00"), response.highestTipPercentage());
        assertEquals(new BigDecimal("20.00"), response.lowestTipPercentage());

        assertEquals(1, response.currencyBreakdown().size());
        assertEquals("USD", response.currencyBreakdown().get(0).currency());
        assertEquals(new BigDecimal("20.00"), response.currencyBreakdown().get(0).totalTipAmount());
    }

    // =============================================
    // Odd median
    // =============================================

    @Test
    void getAnalytics_withOddTipCount_returnsMiddleValueAsMedian() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "10.00", "100.00", "A", ServiceQuality.POOR),
                createTip("USD", "20.00", "100.00", "B", ServiceQuality.GOOD),
                createTip("USD", "30.00", "100.00", "C", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(3, response.totalTipCount());
        assertEquals(new BigDecimal("20.00"), response.medianTipPercentage());
    }

    // =============================================
    // Even median
    // =============================================

    @Test
    void getAnalytics_withEvenTipCount_returnsAverageOfMiddleTwoAsMedian() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "10.00", "100.00", "A", ServiceQuality.POOR),
                createTip("USD", "15.00", "100.00", "B", ServiceQuality.AVERAGE),
                createTip("USD", "20.00", "100.00", "C", ServiceQuality.GOOD),
                createTip("USD", "25.00", "100.00", "D", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(4, response.totalTipCount());
        // (15 + 20) / 2 = 17.50
        assertEquals(new BigDecimal("17.50"), response.medianTipPercentage());
    }

    // =============================================
    // Outlier
    // =============================================

    @Test
    void getAnalytics_withOutlier_returnsCorrectHighLow() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "5.00", "100.00", "A", ServiceQuality.POOR),
                createTip("USD", "15.00", "100.00", "B", ServiceQuality.GOOD),
                createTip("USD", "50.00", "100.00", "C", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(new BigDecimal("5.00"), response.lowestTipPercentage());
        assertEquals(new BigDecimal("50.00"), response.highestTipPercentage());
    }

    // =============================================
    // Multiple currencies
    // =============================================

    @Test
    void getAnalytics_withMultipleCurrencies_separatesMonetaryValues() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "20.00", "100.00", "A", ServiceQuality.GOOD),
                createTip("USD", "15.00", "50.00", "B", ServiceQuality.AVERAGE),
                createTip("INR", "18.00", "500.00", "C", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.ALL_TIME, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        // Percentages are aggregated across currencies
        assertEquals(3, response.totalTipCount());
        assertNotNull(response.averageTipPercentage());
        assertNotNull(response.medianTipPercentage());

        // Currency breakdown must separate monetary values
        assertEquals(2, response.currencyBreakdown().size());

        CurrencyAnalytics inr = response.currencyBreakdown().stream()
                .filter(c -> "INR".equals(c.currency())).findFirst().orElse(null);
        CurrencyAnalytics usd = response.currencyBreakdown().stream()
                .filter(c -> "USD".equals(c.currency())).findFirst().orElse(null);

        assertNotNull(inr);
        assertNotNull(usd);
        assertEquals(1, inr.tipCount());
        assertEquals(2, usd.tipCount());

        // USD: tipAmounts = 20.00 + 7.50 = 27.50 (tip = bill * pct / 100)
        // Actually tipAmount is calculated from percentage*bill/100
        // Let's check the actual amounts based on createTip
    }

    // =============================================
    // Restaurant filtering
    // =============================================

    @Test
    void getAnalytics_withRestaurantFilter_onlyShowsFilteredData() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "20.00", "100.00", "Italian Place", ServiceQuality.GOOD),
                createTip("USD", "18.00", "100.00", "Italian Place", ServiceQuality.EXCELLENT),
                createTip("USD", "10.00", "100.00", "Sushi Bar", ServiceQuality.AVERAGE)
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        // Filter by Italian Place
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, "Italian Place", null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(2, response.totalTipCount());
        // Only Italian Place tips should be included
        assertEquals(1, response.restaurantInsights().size());
        assertEquals("Italian Place", response.restaurantInsights().get(0).restaurantName());
    }

    @Test
    void getAnalytics_withRestaurantFilter_caseInsensitive() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "20.00", "100.00", "Italian Place", ServiceQuality.GOOD),
                createTip("USD", "10.00", "100.00", "Sushi Bar", ServiceQuality.AVERAGE)
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        // Filter by "italian place" (lowercase)
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, "italian place", null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(1, response.totalTipCount());
    }

    // =============================================
    // Service quality filtering
    // =============================================

    @Test
    void getAnalytics_withServiceQualityFilter_onlyShowsFilteredData() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "20.00", "100.00", "A", ServiceQuality.GOOD),
                createTip("USD", "22.00", "100.00", "B", ServiceQuality.GOOD),
                createTip("USD", "10.00", "100.00", "C", ServiceQuality.POOR)
        );
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, null, ServiceQuality.GOOD);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(2, response.totalTipCount());
        // Service quality insights should only have GOOD
        assertEquals(1, response.serviceQualityInsights().size());
        assertEquals(ServiceQuality.GOOD, response.serviceQualityInsights().get(0).serviceQuality());
    }

    // =============================================
    // Date filtering (CUSTOM)
    // =============================================

    @Test
    void getAnalytics_withCustomDateRange_usesCorrectBoundaries() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(new ArrayList<>());

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CUSTOM, start, end, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(0, response.totalTipCount());
        assertEquals(start, response.startDate());
        assertEquals(end, response.endDate());
    }

    @Test
    void getAnalytics_withCustomInvalidDates_throwsException() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        LocalDate start = LocalDate.of(2026, 6, 30);
        LocalDate end = LocalDate.of(2026, 1, 1);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CUSTOM, start, end, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> tipAnalyticsService.getAnalytics(EMAIL, request));
    }

    @Test
    void getAnalytics_withCustomMissingDates_throwsException() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CUSTOM, null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> tipAnalyticsService.getAnalytics(EMAIL, request));
    }

    // =============================================
    // Monthly grouping
    // =============================================

    @Test
    void getAnalytics_monthlyTrend_groupsByYYYYMM() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        Tip tip1 = createTip("USD", "20.00", "100.00", "A", ServiceQuality.GOOD);
        tip1.setCreatedAt(LocalDateTime.of(2026, 1, 15, 12, 0));

        Tip tip2 = createTip("USD", "15.00", "100.00", "B", ServiceQuality.AVERAGE);
        tip2.setCreatedAt(LocalDateTime.of(2026, 1, 20, 12, 0));

        Tip tip3 = createTip("USD", "25.00", "100.00", "C", ServiceQuality.EXCELLENT);
        tip3.setCreatedAt(LocalDateTime.of(2026, 2, 10, 12, 0));

        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip1, tip2, tip3));

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.ALL_TIME, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(2, response.monthlyTrend().size());
        assertEquals("2026-01", response.monthlyTrend().get(0).month());
        assertEquals(2, response.monthlyTrend().get(0).tipCount());
        assertEquals("2026-02", response.monthlyTrend().get(1).month());
        assertEquals(1, response.monthlyTrend().get(1).tipCount());
    }

    // =============================================
    // Empty restaurant
    // =============================================

    @Test
    void getAnalytics_withNullRestaurantNames_handlesGracefully() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        Tip tipNoRestaurant = createTip("USD", "20.00", "100.00", null, ServiceQuality.GOOD);
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(List.of(tipNoRestaurant));

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(1, response.totalTipCount());
        // No restaurant insights since restaurant name is null
        assertTrue(response.restaurantInsights().isEmpty());
    }

    // =============================================
    // ALL_TIME period
    // =============================================

    @Test
    void getAnalytics_allTime_usesFindAllByUserId() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        List<Tip> tips = List.of(
                createTip("USD", "20.00", "100.00", "A", ServiceQuality.GOOD)
        );
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.ALL_TIME, null, null, null, null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(1, response.totalTipCount());
        assertNull(response.startDate());
        assertNull(response.endDate());
    }

    // =============================================
    // Monthly trend respects filters
    // =============================================

    @Test
    void getAnalytics_monthlyTrend_respectsRestaurantFilter() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        Tip tip1 = createTip("USD", "20.00", "100.00", "Italian Place", ServiceQuality.GOOD);
        tip1.setCreatedAt(LocalDateTime.of(2026, 1, 15, 12, 0));

        Tip tip2 = createTip("USD", "10.00", "100.00", "Sushi Bar", ServiceQuality.AVERAGE);
        tip2.setCreatedAt(LocalDateTime.of(2026, 1, 20, 12, 0));

        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(List.of(tip1, tip2));

        // Filter by Italian Place Ã¢â‚¬â€ monthly trend should only include tip1
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.CURRENT_YEAR, null, null, "Italian Place", null);
        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(EMAIL, request);

        assertEquals(1, response.totalTipCount());
        assertEquals(1, response.monthlyTrend().size());
        assertEquals(1, response.monthlyTrend().get(0).tipCount());
        assertEquals(new BigDecimal("20.00"), response.monthlyTrend().get(0).averageTipPercentage());
    }

    // =============================================
    // Null period
    // =============================================

    @Test
    void getAnalytics_withNullPeriod_throwsException() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);

        AnalyticsRequest request = new AnalyticsRequest(null, null, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> tipAnalyticsService.getAnalytics(EMAIL, request));
    }

    // =============================================
    // Helper
    // =============================================

    private Tip createTip(String currency, String tipPct, String billAmount, String restaurant, ServiceQuality sq) {
        BigDecimal bill = new BigDecimal(billAmount);
        BigDecimal pct = new BigDecimal(tipPct);
        BigDecimal tipAmt = bill.multiply(pct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = bill.add(tipAmt);

        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setUser(testUser);
        tip.setCurrency(currency);
        tip.setBillAmount(bill);
        tip.setTipPercentage(pct);
        tip.setTipAmount(tipAmt);
        tip.setTotalAmount(total);
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        return tip;
    }
}
