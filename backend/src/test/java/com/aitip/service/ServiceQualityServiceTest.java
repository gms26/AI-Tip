package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.ServiceQualityStatsResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceQualityService}.
 *
 * <p>Covers all 11 specified scenarios. Uses Mockito to isolate
 * the service from the database and JWT layers.</p>
 */
class ServiceQualityServiceTest {

    private ServiceQualityService serviceQualityService;
    private TipRepository tipRepository;
    private UserRepository userRepository;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        tipRepository = Mockito.mock(TipRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        serviceQualityService = new ServiceQualityService(tipRepository, userRepository);

        testUser = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        setId(testUser, UUID.randomUUID());

        otherUser = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("encoded")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        setId(otherUser, UUID.randomUUID());

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));
    }

    // =============================================
    // 1. No rated tips
    // =============================================
    @Test
    void testNoRatedTips_returnsZeroTotalAndEmptyMap() {
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any()))
                .thenReturn(Collections.emptyList());

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(0, response.totalRatedTips());
        assertEquals(0, response.poorCount());
        assertEquals(0, response.averageCount());
        assertEquals(0, response.goodCount());
        assertEquals(0, response.excellentCount());
        assertNull(response.mostCommon());
        assertTrue(response.averageTipPercentageByQuality().isEmpty());
    }

    // =============================================
    // 2. One rated tip
    // =============================================
    @Test
    void testOneRatedTip_EXCELLENT_returnsCorrectStats() {
        Tip tip = buildTip("20.00", ServiceQuality.EXCELLENT);
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(List.of(tip));

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(1, response.totalRatedTips());
        assertEquals(0, response.poorCount());
        assertEquals(0, response.averageCount());
        assertEquals(0, response.goodCount());
        assertEquals(1, response.excellentCount());
        assertEquals(ServiceQuality.EXCELLENT, response.mostCommon());
        assertEquals(new BigDecimal("20.00"),
                response.averageTipPercentageByQuality().get(ServiceQuality.EXCELLENT));
    }

    // =============================================
    // 3. Multiple ratings Ã¢â‚¬â€ correct counts
    // =============================================
    @Test
    void testMultipleRatings_correctCounts() {
        List<Tip> tips = List.of(
                buildTip("20.00", ServiceQuality.EXCELLENT),
                buildTip("18.00", ServiceQuality.EXCELLENT),
                buildTip("15.00", ServiceQuality.GOOD),
                buildTip("12.00", ServiceQuality.AVERAGE)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(4, response.totalRatedTips());
        assertEquals(0, response.poorCount());
        assertEquals(1, response.averageCount());
        assertEquals(1, response.goodCount());
        assertEquals(2, response.excellentCount());
    }

    // =============================================
    // 4. Most common rating Ã¢â‚¬â€ clear winner
    // =============================================
    @Test
    void testMostCommon_clearWinner() {
        List<Tip> tips = List.of(
                buildTip("20.00", ServiceQuality.EXCELLENT),
                buildTip("18.00", ServiceQuality.EXCELLENT),
                buildTip("18.00", ServiceQuality.EXCELLENT),
                buildTip("15.00", ServiceQuality.GOOD),
                buildTip("12.00", ServiceQuality.AVERAGE)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(ServiceQuality.EXCELLENT, response.mostCommon());
    }

    // =============================================
    // 4b. Tie-breaking: EXCELLENT beats GOOD on tie
    // =============================================
    @Test
    void testMostCommon_tieBreakingHigherQualityWins() {
        // GOOD=2, EXCELLENT=2 Ã¢â‚¬â€ EXCELLENT should win (higher in enum order)
        List<Tip> tips = List.of(
                buildTip("15.00", ServiceQuality.GOOD),
                buildTip("16.00", ServiceQuality.GOOD),
                buildTip("20.00", ServiceQuality.EXCELLENT),
                buildTip("21.00", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(ServiceQuality.EXCELLENT, response.mostCommon());
    }

    // =============================================
    // 5. Average tip percentage by quality
    // =============================================
    @Test
    void testAverageTipPercentageByQuality_correct() {
        // EXCELLENT: 20% + 18% = 38 / 2 = 19.00
        // GOOD: 15% alone = 15.00
        List<Tip> tips = List.of(
                buildTip("20.00", ServiceQuality.EXCELLENT),
                buildTip("18.00", ServiceQuality.EXCELLENT),
                buildTip("15.00", ServiceQuality.GOOD)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(new BigDecimal("19.00"),
                response.averageTipPercentageByQuality().get(ServiceQuality.EXCELLENT));
        assertEquals(new BigDecimal("15.00"),
                response.averageTipPercentageByQuality().get(ServiceQuality.GOOD));
        // POOR and AVERAGE not present in map (no data)
        assertFalse(response.averageTipPercentageByQuality().containsKey(ServiceQuality.POOR));
        assertFalse(response.averageTipPercentageByQuality().containsKey(ServiceQuality.AVERAGE));
    }

    // =============================================
    // 6. POOR calculation
    // =============================================
    @Test
    void testPoorCalculation() {
        List<Tip> tips = List.of(
                buildTip("8.00", ServiceQuality.POOR),
                buildTip("10.00", ServiceQuality.POOR)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(2, response.poorCount());
        assertEquals(0, response.excellentCount());
        assertEquals(ServiceQuality.POOR, response.mostCommon());
        // (8 + 10) / 2 = 9.00
        assertEquals(new BigDecimal("9.00"),
                response.averageTipPercentageByQuality().get(ServiceQuality.POOR));
    }

    // =============================================
    // 7. AVERAGE calculation
    // =============================================
    @Test
    void testAverageQualityCalculation() {
        // 12% + 14% + 13% = 39 / 3 = 13.00
        List<Tip> tips = List.of(
                buildTip("12.00", ServiceQuality.AVERAGE),
                buildTip("14.00", ServiceQuality.AVERAGE),
                buildTip("13.00", ServiceQuality.AVERAGE)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(3, response.averageCount());
        assertEquals(new BigDecimal("13.00"),
                response.averageTipPercentageByQuality().get(ServiceQuality.AVERAGE));
    }

    // =============================================
    // 8. GOOD calculation
    // =============================================
    @Test
    void testGoodQualityCalculation() {
        // 15% + 17% + 18% = 50 / 3 = 16.67 (HALF_UP)
        List<Tip> tips = List.of(
                buildTip("15.00", ServiceQuality.GOOD),
                buildTip("17.00", ServiceQuality.GOOD),
                buildTip("18.00", ServiceQuality.GOOD)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(3, response.goodCount());
        assertEquals(new BigDecimal("16.67"),
                response.averageTipPercentageByQuality().get(ServiceQuality.GOOD));
    }

    // =============================================
    // 9. EXCELLENT calculation with rounding
    // =============================================
    @Test
    void testExcellentQualityCalculation_withRounding() {
        // 17% + 18% + 20% = 55 / 3 = 18.33 (HALF_UP, scale=2)
        List<Tip> tips = List.of(
                buildTip("17.00", ServiceQuality.EXCELLENT),
                buildTip("18.00", ServiceQuality.EXCELLENT),
                buildTip("20.00", ServiceQuality.EXCELLENT)
        );
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(tips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        assertEquals(3, response.excellentCount());
        assertEquals(new BigDecimal("18.33"),
                response.averageTipPercentageByQuality().get(ServiceQuality.EXCELLENT));
    }

    // =============================================
    // 10. Historical NULL ratings are excluded
    // =============================================
    @Test
    void testHistoricalNullRatings_areExcluded() {
        // Repository mock returns only non-null rated tips (simulating DB filter)
        // One EXCELLENT tip; the historical tips are already excluded by the query
        List<Tip> ratedTips = List.of(buildTip("20.00", ServiceQuality.EXCELLENT));
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(any())).thenReturn(ratedTips);

        ServiceQualityStatsResponse response = serviceQualityService.getStats("alice@example.com");

        // Only 1 rated tip counted Ã¢â‚¬â€ historical nulls are invisible
        assertEquals(1, response.totalRatedTips());
        assertEquals(1, response.excellentCount());
        assertEquals(ServiceQuality.EXCELLENT, response.mostCommon());
    }

    // =============================================
    // 11. User-specific statistics (isolation)
    // =============================================
    @Test
    void testUserSpecificStatistics_isolation() {
        // Alice has 2 EXCELLENT tips
        List<Tip> aliceTips = List.of(
                buildTip("20.00", ServiceQuality.EXCELLENT),
                buildTip("18.00", ServiceQuality.EXCELLENT)
        );
        // Bob has no rated tips
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(testUser.getId()))
                .thenReturn(aliceTips);
        when(tipRepository.findByUserIdAndServiceQualityIsNotNull(otherUser.getId()))
                .thenReturn(Collections.emptyList());

        ServiceQualityStatsResponse aliceResponse = serviceQualityService.getStats("alice@example.com");
        ServiceQualityStatsResponse bobResponse = serviceQualityService.getStats("bob@example.com");

        assertEquals(2, aliceResponse.totalRatedTips());
        assertEquals(ServiceQuality.EXCELLENT, aliceResponse.mostCommon());

        assertEquals(0, bobResponse.totalRatedTips());
        assertNull(bobResponse.mostCommon());
    }

    // =============================================
    // Helpers
    // =============================================
    private Tip buildTip(String tipPercentage, ServiceQuality quality) {
        return Tip.builder()
                .user(testUser)
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal(tipPercentage))
                .tipAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .currency("USD")
                .serviceQuality(quality)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void setId(User user, UUID id) {
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception ignored) {
        }
    }
}
