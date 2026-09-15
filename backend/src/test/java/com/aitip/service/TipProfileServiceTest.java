package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipBehaviorType;
import com.aitip.dto.TipProfileResponse;
import com.aitip.dto.TipStyle;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TipProfileServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    private TipProfileService tipProfileService;
    private GenerosityScoreService generosityScoreService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Use real generosity score service for deterministic mappings
        generosityScoreService = new GenerosityScoreService(null, null);
        tipProfileService = new TipProfileService(tipRepository, userService, generosityScoreService);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
    }

    private Tip createTip(String currency, double amount, double percentage, String restaurant, ServiceQuality sq, int daysAgo) {
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setCurrency(currency);
        tip.setTipAmount(BigDecimal.valueOf(amount));
        tip.setTipPercentage(BigDecimal.valueOf(percentage));
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return tip;
    }

    @Test
    void testGetProfile_EmptyHistory() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of());

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(0, profile.totalTipCount());
        assertTrue(profile.currencyProfiles().isEmpty());
        assertNull(profile.overallBehaviorType());
        assertNull(profile.overallTipStyle());
        assertNull(profile.consistencyScore());
        assertNull(profile.recentTrend());
    }

    @Test
    void testGetProfile_LimitedHistory() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = List.of(
            createTip("USD", 5, 15, "Cafe", null, 1),
            createTip("USD", 10, 20, "Cafe", null, 2)
        );
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(2, profile.totalTipCount());
        assertEquals(1, profile.currencyProfiles().size());
        assertNull(profile.overallBehaviorType()); // Need >= 5 tips for behavior
        assertNull(profile.consistencyScore());    // Need >= 5 tips for consistency
        assertNull(profile.recentTrend());         // Need >= 10 tips for trend
        
        // Generosity logic is still applied to overall stats
        assertNotNull(profile.overallTipStyle());
        assertEquals(new BigDecimal("17.50"), profile.historicalMedianTipPercentage());
    }

    @Test
    void testGetProfile_ConsistentBehavior() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        // Mean 15%, standard deviation 0
        for (int i = 0; i < 5; i++) {
            tips.add(createTip("USD", 5, 15, "Cafe", null, i));
        }
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(5, profile.totalTipCount());
        assertNotNull(profile.consistencyScore());
        assertEquals(100, profile.consistencyScore()); // Perfect consistency
        assertEquals(TipBehaviorType.VERY_CONSISTENT, profile.overallBehaviorType());
    }

    @Test
    void testGetProfile_VariableBehavior() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        // Creating tips with standard deviation of ~5
        tips.add(createTip("USD", 5, 10, "Cafe", null, 1));
        tips.add(createTip("USD", 5, 15, "Cafe", null, 2));
        tips.add(createTip("USD", 5, 20, "Cafe", null, 3));
        tips.add(createTip("USD", 5, 25, "Cafe", null, 4));
        tips.add(createTip("USD", 5, 30, "Cafe", null, 5));
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(5, profile.totalTipCount());
        assertNotNull(profile.consistencyScore());
        
        // Standard deviation of [10, 15, 20, 25, 30] is ~7.07
        // Score = max(0, 100 - (7.07 * 10)) = 100 - 71 = 29
        assertEquals(29, profile.consistencyScore()); 
        assertEquals(TipBehaviorType.HIGHLY_VARIABLE, profile.overallBehaviorType());
    }

    @Test
    void testGetProfile_RecentTrend_MoreGenerous() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        // Historical tips (older than last 5): median 15
        for (int i = 6; i <= 15; i++) {
            tips.add(createTip("USD", 5, 15, "Cafe", null, i));
        }
        // Recent tips (last 5): median 20
        for (int i = 1; i <= 5; i++) {
            tips.add(createTip("USD", 5, 20, "Cafe", null, i));
        }
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(15, profile.totalTipCount());
        assertEquals("MORE_GENEROUS", profile.recentTrend());
    }
    
    @Test
    void testGetProfile_RecentTrend_Stable() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        // Historical tips: median 15
        for (int i = 6; i <= 15; i++) {
            tips.add(createTip("USD", 5, 15, "Cafe", null, i));
        }
        // Recent tips: median 16 (difference < 3)
        for (int i = 1; i <= 5; i++) {
            tips.add(createTip("USD", 5, 16, "Cafe", null, i));
        }
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals("STABLE", profile.recentTrend());
    }

    @Test
    void testGetProfile_CurrencyIsolation() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = List.of(
            createTip("USD", 5, 15, "Cafe", null, 1),
            createTip("USD", 5, 15, "Cafe", null, 2),
            createTip("EUR", 10, 10, "Cafe", null, 3)
        );
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(3, profile.totalTipCount());
        assertEquals(2, profile.currencyProfiles().size());
        
        // USD should be first because it has more tips
        assertEquals("USD", profile.currencyProfiles().get(0).currency());
        assertEquals(2, profile.currencyProfiles().get(0).tipCount());
        assertEquals(new BigDecimal("15.00"), profile.currencyProfiles().get(0).medianTipPercentage());
        
        assertEquals("EUR", profile.currencyProfiles().get(1).currency());
        assertEquals(1, profile.currencyProfiles().get(1).tipCount());
    }

    @Test
    void testGetProfile_CurrencyFilter() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = List.of(
            createTip("USD", 5, 15, "Cafe", null, 1),
            createTip("EUR", 10, 10, "Cafe", null, 3)
        );
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", "EUR");

        assertEquals(1, profile.totalTipCount()); // Filtered to EUR only
        assertEquals(1, profile.currencyProfiles().size());
        assertEquals("EUR", profile.currencyProfiles().get(0).currency());
    }
    
    @Test
    void testGetProfile_RestaurantNormalization() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        
        List<Tip> tips = List.of(
            createTip("USD", 5, 15, "Cafe Mocha", null, 1),
            createTip("USD", 5, 15, "cafe mocha", null, 2),
            createTip("USD", 5, 15, " CAFE MOCHA ", null, 3)
        );
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);

        TipProfileResponse profile = tipProfileService.getProfile("test@example.com", null);

        assertEquals(1, profile.topRestaurants().size());
        assertEquals(3, profile.topRestaurants().get(0).tipCount());
    }
}
