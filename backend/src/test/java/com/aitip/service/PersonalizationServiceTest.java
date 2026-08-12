package com.aitip.service;

import com.aitip.dto.PersonalizationContext;
import com.aitip.dto.PersonalizationResponse;
import com.aitip.dto.RestaurantInsight;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PersonalizationServiceTest {

    private PersonalizationService personalizationService;
    private TipRepository tipRepository;
    private UserRepository userRepository;
    private User testUser;

    @BeforeEach
    void setUp() {
        tipRepository = Mockito.mock(TipRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        personalizationService = new PersonalizationService(tipRepository, userRepository);

        testUser = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        // Simulate a UUID
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, UUID.randomUUID());
        } catch (Exception ignored) {}

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
    }

    // =============================================
    // 1. User with no tips
    // =============================================
    @Test
    void testNoTips_returnsZeroCountAndMessage() {
        when(tipRepository.findAllByUserId(any())).thenReturn(Collections.emptyList());

        PersonalizationResponse response = personalizationService.getSummary("alice@example.com");

        assertEquals(0, response.tipCount());
        assertEquals("No tipping history yet.", response.message());
        assertNull(response.averageTipPercentage());
        assertNull(response.personalizedPercentage());
    }

    // =============================================
    // 2. User with one tip
    // =============================================
    @Test
    void testOneTip_returnsCountOneMessage() {
        Tip tip = buildTip("18.00", "5.40", "35.40", "Place A");
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(tip));

        PersonalizationResponse response = personalizationService.getSummary("alice@example.com");

        assertEquals(1, response.tipCount());
        assertEquals("You have 1 recorded tip.", response.message());
        assertEquals(new BigDecimal("18.00"), response.medianTipPercentage());
    }

    // =============================================
    // 3. Multiple tips — verifies mean, median, min, max, count
    // =============================================
    @Test
    void testMultipleTips_calculatesCorrectStatistics() {
        List<Tip> tips = List.of(
                buildTip("15.00", "3.00", "23.00", null),
                buildTip("18.00", "5.40", "35.40", null),
                buildTip("20.00", "10.00", "60.00", null),
                buildTip("22.00", "11.00", "61.00", null),
                buildTip("17.00", "4.25", "29.25", null)
        );
        when(tipRepository.findAllByUserId(any())).thenReturn(tips);

        PersonalizationResponse response = personalizationService.getSummary("alice@example.com");

        assertEquals(5, response.tipCount());
        // Sorted: 15, 17, 18, 20, 22 → median = 18
        assertEquals(new BigDecimal("18.00"), response.medianTipPercentage());
        // Mean: (15+17+18+20+22)/5 = 18.4
        assertEquals(new BigDecimal("18.40"), response.averageTipPercentage());
        assertEquals(new BigDecimal("15.00"), response.minimumTipPercentage());
        assertEquals(new BigDecimal("22.00"), response.maximumTipPercentage());
        assertTrue(response.message().contains("typically tip"));
    }

    // =============================================
    // 4. Restaurant-specific history
    // =============================================
    @Test
    void testRestaurantSpecificHistory() {
        List<Tip> allTips = List.of(
                buildTip("18.00", "5.00", "33.00", "Italian Place"),
                buildTip("20.00", "10.00", "60.00", "Italian Place"),
                buildTip("15.00", "3.00", "23.00", "Burger Joint")
        );
        List<Tip> restaurantTips = List.of(
                buildTip("18.00", "5.00", "33.00", "Italian Place"),
                buildTip("20.00", "10.00", "60.00", "Italian Place")
        );
        when(tipRepository.findAllByUserId(any())).thenReturn(allTips);
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(any(), eq("Italian Place")))
                .thenReturn(restaurantTips);

        PersonalizationResponse response = personalizationService.getRestaurantPersonalization(
                "alice@example.com", "Italian Place");

        assertNotNull(response.restaurantInsight());
        assertEquals(2, response.restaurantInsight().visitCount());
        // Average: (18+20)/2 = 19.00
        assertEquals(new BigDecimal("19.00"), response.restaurantInsight().averageTipPercentage());
    }

    // =============================================
    // 5. Restaurant with no history
    // =============================================
    @Test
    void testRestaurantNoHistory_returnsEmptyInsight() {
        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(buildTip("18.00", "5.00", "33.00", "Other")));
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(any(), eq("Unknown Place")))
                .thenReturn(Collections.emptyList());

        PersonalizationResponse response = personalizationService.getRestaurantPersonalization(
                "alice@example.com", "Unknown Place");

        assertNotNull(response.restaurantInsight());
        assertEquals(0, response.restaurantInsight().visitCount());
        assertNull(response.restaurantInsight().averageTipPercentage());
    }

    // =============================================
    // 6. Case-insensitive restaurant matching (repository layer)
    // Verified by the Spring Data method name convention
    // =============================================

    // =============================================
    // 7. Latest restaurant tip is correctly identified
    // =============================================
    @Test
    void testLatestRestaurantTipIdentified() {
        Tip older = buildTipWithDate("18.00", "5.00", "33.00", "Italian Place",
                LocalDateTime.of(2025, 1, 1, 12, 0));
        Tip newer = buildTipWithDate("20.00", "10.00", "60.00", "Italian Place",
                LocalDateTime.of(2025, 6, 15, 18, 30));

        when(tipRepository.findAllByUserId(any())).thenReturn(List.of(older, newer));
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(any(), eq("Italian Place")))
                .thenReturn(List.of(older, newer));

        PersonalizationResponse response = personalizationService.getRestaurantPersonalization(
                "alice@example.com", "Italian Place");

        assertEquals(new BigDecimal("20.00"), response.restaurantInsight().lastTipPercentage());
        assertEquals(LocalDateTime.of(2025, 6, 15, 18, 30), response.restaurantInsight().lastVisitDate());
    }

    // =============================================
    // 9. Outlier handling — median is robust
    // =============================================
    @Test
    void testOutlierDoesNotDistortMedian() {
        // History: 18, 19, 20, 100. Mean = 39.25, Median = 19.50
        List<Tip> tips = List.of(
                buildTip("18.00", "5.00", "33.00", null),
                buildTip("19.00", "5.50", "34.50", null),
                buildTip("20.00", "10.00", "60.00", null),
                buildTip("100.00", "50.00", "100.00", null)
        );
        when(tipRepository.findAllByUserId(any())).thenReturn(tips);

        PersonalizationResponse response = personalizationService.getSummary("alice@example.com");

        // Median of [18, 19, 20, 100] = (19 + 20) / 2 = 19.5
        assertEquals(new BigDecimal("19.50"), response.medianTipPercentage());
        assertEquals(new BigDecimal("19.5"), response.personalizedPercentage());

        // Mean should be high (39.25) but the personalized percentage should not be
        assertEquals(new BigDecimal("39.25"), response.averageTipPercentage());
    }

    // =============================================
    // 10. Personalized percentage is deterministic
    // =============================================
    @Test
    void testPersonalizedPercentageIsDeterministic() {
        List<Tip> tips = List.of(
                buildTip("15.00", "3.00", "23.00", null),
                buildTip("20.00", "10.00", "60.00", null),
                buildTip("18.00", "5.40", "35.40", null)
        );
        when(tipRepository.findAllByUserId(any())).thenReturn(tips);

        PersonalizationResponse r1 = personalizationService.getSummary("alice@example.com");
        PersonalizationResponse r2 = personalizationService.getSummary("alice@example.com");

        assertEquals(r1.personalizedPercentage(), r2.personalizedPercentage());
        assertEquals(r1.medianTipPercentage(), r2.medianTipPercentage());
    }

    // =============================================
    // 11. AI context is built correctly
    // =============================================
    @Test
    void testBuildContextForAi() {
        List<Tip> tips = List.of(
                buildTip("18.00", "5.00", "33.00", "Italian Place"),
                buildTip("20.00", "10.00", "60.00", "Italian Place")
        );
        when(tipRepository.findAllByUserId(any())).thenReturn(tips);
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCase(any(), eq("Italian Place")))
                .thenReturn(tips);

        PersonalizationContext context = personalizationService.buildContextForAi("alice@example.com", "Italian Place", null);

        assertNotNull(context);
        assertTrue(context.hasPersonalizationData());
        assertTrue(context.hasRestaurantData());
        assertEquals(2, context.overallTipCount());
        assertEquals(2, context.restaurantVisitCount());
        assertEquals(com.aitip.dto.PersonalizationSource.RESTAURANT, context.source());
        assertEquals("MEDIUM", context.confidence());
    }

    @Test
    void testBuildContextForAi_noTips_returnsNull() {
        when(tipRepository.findAllByUserId(any())).thenReturn(Collections.emptyList());

        PersonalizationContext context = personalizationService.buildContextForAi("alice@example.com", "Italian Place", null);
        assertNull(context);
    }
    
    @Test
    void testBuildContextForAi_RestAndServiceQualityStrongest() {
        List<Tip> allTips = List.of(
                buildTip("18.00", "5.00", "33.00", "Italian Place"),
                buildTip("20.00", "10.00", "60.00", "Italian Place"),
                buildTip("22.00", "11.00", "61.00", "Italian Place")
        );
        when(tipRepository.findAllByUserId(any())).thenReturn(allTips);
        when(tipRepository.findByUserIdAndRestaurantNameIgnoreCaseAndServiceQuality(any(), eq("Italian Place"), eq(com.aitip.dto.ServiceQuality.EXCELLENT)))
                .thenReturn(allTips);

        PersonalizationContext context = personalizationService.buildContextForAi("alice@example.com", "Italian Place", com.aitip.dto.ServiceQuality.EXCELLENT);

        assertNotNull(context);
        assertEquals(com.aitip.dto.PersonalizationSource.RESTAURANT_AND_SERVICE, context.source());
        assertEquals("MEDIUM", context.confidence()); // 3 tips -> MEDIUM
    }

    // =============================================
    // Helpers
    // =============================================
    private Tip buildTip(String percentage, String amount, String total, String restaurant) {
        return buildTipWithDate(percentage, amount, total, restaurant, LocalDateTime.now());
    }

    private Tip buildTipWithDate(String percentage, String amount, String total, String restaurant, LocalDateTime date) {
        return Tip.builder()
                .user(testUser)
                .restaurantName(restaurant)
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(new BigDecimal(percentage))
                .tipAmount(new BigDecimal(amount))
                .totalAmount(new BigDecimal(total))
                .currency("USD")
                .serviceQuality(com.aitip.dto.ServiceQuality.GOOD)
                .createdAt(date)
                .updatedAt(date)
                .build();
    }
}
