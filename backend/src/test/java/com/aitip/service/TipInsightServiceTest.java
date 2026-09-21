package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.insights.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipInsightServiceTest {

    @Mock
    private TipRepository tipRepository;
    @Mock
    private TipInsightPromptBuilder promptBuilder;
    @Mock
    private AiProvider AiProvider;
    @Mock
    private UserRepository userRepository;

    private TipInsightService tipInsightService;
    private User testUser;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        tipInsightService = new TipInsightService(tipRepository, promptBuilder, AiProvider, new ObjectMapper(), userRepository);
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(email);
        lenient().when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(testUser));
    }

    @Test
    void testZeroTips_InsufficientData() {
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of());
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals("INSUFFICIENT_DATA", response.overallTrend().trendDirection());
        assertEquals("LOW", response.overallTrend().confidence());
        assertTrue(response.topRestaurants().isEmpty());
    }

    @Test
    void testOneTip_InsufficientData_LowConfidence() {
        Tip tip = Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(10)).build();
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(tip));
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals("INSUFFICIENT_DATA", response.overallTrend().trendDirection());
        assertEquals("LOW", response.overallTrend().confidence());
    }

    @Test
    void testIncreasingTrend() {
        List<Tip> tips = new ArrayList<>();
        // Previous (30-60 days): 15%, 15% -> Avg 15%
        tips.add(Tip.builder().tipPercentage(new BigDecimal("15.00")).createdAt(LocalDateTime.now().minusDays(40)).build());
        tips.add(Tip.builder().tipPercentage(new BigDecimal("15.00")).createdAt(LocalDateTime.now().minusDays(45)).build());
        
        // Recent (0-30 days): 20%, 20% -> Avg 20%
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(5)).build());
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals("INCREASING", response.overallTrend().trendDirection());
        assertEquals("MEDIUM", response.overallTrend().confidence()); // 4 tips total
    }

    @Test
    void testDecreasingTrend() {
        List<Tip> tips = new ArrayList<>();
        // Previous: 25%, 25% -> Avg 25%
        tips.add(Tip.builder().tipPercentage(new BigDecimal("25.00")).createdAt(LocalDateTime.now().minusDays(40)).build());
        tips.add(Tip.builder().tipPercentage(new BigDecimal("25.00")).createdAt(LocalDateTime.now().minusDays(45)).build());
        
        // Recent: 20%, 20% -> Avg 20%
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(5)).build());
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals("DECREASING", response.overallTrend().trendDirection());
    }

    @Test
    void testStableTrend() {
        List<Tip> tips = new ArrayList<>();
        // Previous: 20%, 20%
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(40)).build());
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(45)).build());
        
        // Recent: 20%, 20.5% -> Change is < 5%
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().tipPercentage(new BigDecimal("20.50")).createdAt(LocalDateTime.now().minusDays(5)).build());
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals("STABLE", response.overallTrend().trendDirection());
    }

    @Test
    void testHighConfidence() {
        List<Tip> tips = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tips.add(Tip.builder().tipPercentage(new BigDecimal("20.00")).createdAt(LocalDateTime.now().minusDays(10)).build());
        }
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals("HIGH", response.overallTrend().confidence()); // 6 tips
    }

    @Test
    void testRestaurantGrouping_CurrenciesIsolated() {
        List<Tip> tips = new ArrayList<>();
        tips.add(Tip.builder().restaurantName("Pizza Place").currency("USD").tipPercentage(new BigDecimal("20")).tipAmount(new BigDecimal("5.00")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().restaurantName("Pizza Place").currency("USD").tipPercentage(new BigDecimal("20")).tipAmount(new BigDecimal("5.00")).createdAt(LocalDateTime.now().minusDays(11)).build());
        
        tips.add(Tip.builder().restaurantName("Pizza Place").currency("INR").tipPercentage(new BigDecimal("15")).tipAmount(new BigDecimal("500.00")).createdAt(LocalDateTime.now().minusDays(12)).build());
        tips.add(Tip.builder().restaurantName("Pizza Place").currency("INR").tipPercentage(new BigDecimal("15")).tipAmount(new BigDecimal("500.00")).createdAt(LocalDateTime.now().minusDays(13)).build());
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals(2, response.topRestaurants().size()); // Two distinct items
        assertTrue(response.topRestaurants().stream().anyMatch(rt -> rt.currency().equals("USD") && rt.totalTipAmount().compareTo(new BigDecimal("10.00")) == 0));
        assertTrue(response.topRestaurants().stream().anyMatch(rt -> rt.currency().equals("INR") && rt.totalTipAmount().compareTo(new BigDecimal("1000.00")) == 0));
    }

    @Test
    void testNullServiceQualityIgnored() {
        List<Tip> tips = new ArrayList<>();
        tips.add(Tip.builder().serviceQuality(ServiceQuality.GOOD).tipPercentage(new BigDecimal("20")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().serviceQuality(null).tipPercentage(new BigDecimal("10")).createdAt(LocalDateTime.now().minusDays(11)).build());
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals(1, response.serviceQualityTrends().size());
        assertTrue(response.serviceQualityTrends().containsKey(ServiceQuality.GOOD));
    }

    @Test
    void testMedianOddEven() {
        List<Tip> tips = new ArrayList<>();
        // Odd count
        tips.add(Tip.builder().serviceQuality(ServiceQuality.POOR).tipPercentage(new BigDecimal("10")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().serviceQuality(ServiceQuality.POOR).tipPercentage(new BigDecimal("12")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().serviceQuality(ServiceQuality.POOR).tipPercentage(new BigDecimal("20")).createdAt(LocalDateTime.now().minusDays(10)).build());
        
        // Even count
        tips.add(Tip.builder().serviceQuality(ServiceQuality.EXCELLENT).tipPercentage(new BigDecimal("20")).createdAt(LocalDateTime.now().minusDays(10)).build());
        tips.add(Tip.builder().serviceQuality(ServiceQuality.EXCELLENT).tipPercentage(new BigDecimal("30")).createdAt(LocalDateTime.now().minusDays(10)).build());
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(tips);
        
        TipInsightSummaryResponse response = tipInsightService.getInsights(email);
        
        assertEquals(new BigDecimal("12"), response.serviceQualityTrends().get(ServiceQuality.POOR).medianTipPercentage());
        assertEquals(new BigDecimal("25.00"), response.serviceQualityTrends().get(ServiceQuality.EXCELLENT).medianTipPercentage());
    }
}
