package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipGoalProgressResponse;
import com.aitip.entity.*;
import com.aitip.repository.TipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipGoalCalculationServiceTest {

    @Mock
    private TipRepository tipRepository;

    @InjectMocks
    private TipGoalCalculationService calculationService;

    private User testUser;
    private TipGoal.TipGoalBuilder goalBuilder;
    private List<Tip> mockTips;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        
        goalBuilder = TipGoal.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .targetValue(new BigDecimal("100.00"))
                .period(TipGoalPeriod.ALL_TIME)
                .status(TipGoalStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
                
        mockTips = new ArrayList<>();
    }

    private Tip createTip(String amount, String percentage, String currency, String restaurant, ServiceQuality sq) {
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setUser(testUser);
        tip.setTipAmount(new BigDecimal(amount));
        if (percentage != null) {
            tip.setTipPercentage(new BigDecimal(percentage));
        }
        tip.setCurrency(currency);
        tip.setRestaurantName(restaurant);
        tip.setServiceQuality(sq);
        tip.setCreatedAt(LocalDateTime.now());
        return tip;
    }

    @Test
    void calculate_zeroHistory() {
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipGoal goal = goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build();
        TipGoalProgressResponse response = calculationService.calculateProgress(goal);
        
        assertThat(response.currentValue()).isEqualByComparingTo("0.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("0.00");
        assertThat(response.remainingValue()).isEqualByComparingTo("100.00");
        assertThat(response.confidence()).isEqualTo("LOW");
        assertThat(response.status()).isEqualTo(TipGoalStatus.ACTIVE);
    }

    @Test
    void calculate_oneTip() {
        mockTips.add(createTip("50.00", "20.00", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        TipGoal goal = goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build();
        var response = calculationService.calculateProgress(goal);
        
        assertThat(response.currentValue()).isEqualByComparingTo("50.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("50.00");
        assertThat(response.confidence()).isEqualTo("LOW");
    }

    @Test
    void calculate_multipleTips() {
        mockTips.add(createTip("50.00", "20.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("25.00", "15.00", "USD", "B", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build());
        assertThat(response.currentValue()).isEqualByComparingTo("75.00");
        assertThat(response.progressPercentage()).isEqualByComparingTo("75.00");
        assertThat(response.confidence()).isEqualTo("MEDIUM");
    }

    @Test
    void calculate_totalAmount() {
        mockTips.add(createTip("100.00", "20", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build());
        assertThat(response.currentValue()).isEqualByComparingTo("100.00");
        assertThat(response.status()).isEqualTo(TipGoalStatus.COMPLETED);
    }

    @Test
    void calculate_tipCount() {
        mockTips.add(createTip("10", "10", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TIP_COUNT).targetValue(new BigDecimal("5")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("2");
        assertThat(response.progressPercentage()).isEqualByComparingTo("40.00");
    }

    @Test
    void calculate_averageTipPercentage() {
        mockTips.add(createTip("10", "10.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "20.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "25.00", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.AVERAGE_TIP_PERCENTAGE).targetValue(new BigDecimal("20.00")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("18.33");
        assertThat(response.progressPercentage()).isEqualByComparingTo("91.65");
    }

    @Test
    void calculate_oddMedian() {
        mockTips.add(createTip("10", "10.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "20.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "25.00", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.MEDIAN_TIP_PERCENTAGE).targetValue(new BigDecimal("20")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("20.00");
    }

    @Test
    void calculate_evenMedian() {
        mockTips.add(createTip("10", "10.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "15.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "25.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "30.00", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.MEDIAN_TIP_PERCENTAGE).targetValue(new BigDecimal("20")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("20.00");
    }

    @Test
    void calculate_medianOutlier() {
        mockTips.add(createTip("10", "15.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "18.00", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "99.00", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.MEDIAN_TIP_PERCENTAGE).targetValue(new BigDecimal("20")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("18.00");
    }

    @Test
    void calculate_restaurantNormalization() {
        mockTips.add(createTip("10", "10", "USD", "Italian Place", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "10", "USD", " italian   place ", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "10", "USD", "ITALIAN PLACE", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TIP_COUNT).restaurantName("Italian Place").targetValue(new BigDecimal("3")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("3");
    }

    @Test
    void calculate_restaurantExploration() {
        mockTips.add(createTip("10", "10", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "10", "USD", "a", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "10", "USD", "B", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.RESTAURANT_EXPLORATION).targetValue(new BigDecimal("2")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("2");
        assertThat(response.status()).isEqualTo(TipGoalStatus.COMPLETED);
    }

    @Test
    void calculate_serviceQualityFiltering() {
        mockTips.add(createTip("10", "10", "USD", "A", ServiceQuality.EXCELLENT));
        mockTips.add(createTip("10", "10", "USD", "B", ServiceQuality.EXCELLENT));
        mockTips.add(createTip("10", "10", "USD", "C", ServiceQuality.AVERAGE));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.SERVICE_QUALITY).serviceQuality(ServiceQuality.EXCELLENT).targetValue(new BigDecimal("3")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("2");
    }

    @Test
    void calculate_currencyIsolation() {
        mockTips.add(createTip("10", "10", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("10", "10", "INR", "B", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).currency("USD").targetValue(new BigDecimal("20")).build());
        assertThat(response.currentValue()).isEqualByComparingTo("10");
    }

    @Test
    void calculate_progressBelow100() {
        mockTips.add(createTip("40", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).currency("USD").targetValue(new BigDecimal("100")).build());
        assertThat(response.progressPercentage()).isEqualByComparingTo("40.00");
    }

    @Test
    void calculate_progressExactly100() {
        mockTips.add(createTip("100", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).currency("USD").targetValue(new BigDecimal("100")).build());
        assertThat(response.progressPercentage()).isEqualByComparingTo("100.00");
        assertThat(response.status()).isEqualTo(TipGoalStatus.COMPLETED);
    }

    @Test
    void calculate_progressAbove100() {
        mockTips.add(createTip("150", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).currency("USD").targetValue(new BigDecimal("100")).build());
        assertThat(response.progressPercentage()).isEqualByComparingTo("100.00");
        assertThat(response.status()).isEqualTo(TipGoalStatus.COMPLETED);
    }

    @Test
    void calculate_remainingAmount() {
        mockTips.add(createTip("40", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).currency("USD").targetValue(new BigDecimal("100")).build());
        assertThat(response.remainingValue()).isEqualByComparingTo("60.00");
    }

    @Test
    void calculate_zeroRemaining() {
        mockTips.add(createTip("140", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).currency("USD").targetValue(new BigDecimal("100")).build());
        assertThat(response.remainingValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void calculate_confidenceLow() {
        mockTips.add(createTip("40", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build());
        assertThat(response.confidence()).isEqualTo("LOW");
    }

    @Test
    void calculate_confidenceMedium() {
        mockTips.add(createTip("40", "10", "USD", "A", ServiceQuality.GOOD));
        mockTips.add(createTip("40", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build());
        assertThat(response.confidence()).isEqualTo("MEDIUM");
    }

    @Test
    void calculate_confidenceHigh() {
        for (int i=0; i<5; i++) {
            mockTips.add(createTip("40", "10", "USD", "A", ServiceQuality.GOOD));
        }
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).build());
        assertThat(response.confidence()).isEqualTo("HIGH");
    }

    @Test
    void calculate_expiredGoal() {
        TipGoal goal = goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT)
                .period(TipGoalPeriod.CUSTOM)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(1))
                .build();
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goal);
        assertThat(response.status()).isEqualTo(TipGoalStatus.EXPIRED);
    }

    @Test
    void calculate_completedGoal() {
        mockTips.add(createTip("140", "10", "USD", "A", ServiceQuality.GOOD));
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        TipGoal goal = goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT)
                .targetValue(new BigDecimal("100"))
                .status(TipGoalStatus.COMPLETED)
                .build();
                
        var response = calculationService.calculateProgress(goal);
        assertThat(response.status()).isEqualTo(TipGoalStatus.COMPLETED);
    }

    @Test
    void calculate_customPeriod() {
        Tip t1 = createTip("100", "10", "USD", "A", ServiceQuality.GOOD);
        t1.setCreatedAt(LocalDateTime.now().minusDays(5));
        Tip t2 = createTip("100", "10", "USD", "B", ServiceQuality.GOOD);
        t2.setCreatedAt(LocalDateTime.now().minusDays(15));
        
        mockTips.add(t1);
        mockTips.add(t2);
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        TipGoal goal = goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT)
                .period(TipGoalPeriod.CUSTOM)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(10))
                .build();
                
        var response = calculationService.calculateProgress(goal);
        assertThat(response.currentValue()).isEqualByComparingTo("100");
    }

    @Test
    void calculate_currentMonth() {
        Tip t1 = createTip("100", "10", "USD", "A", ServiceQuality.GOOD);
        t1.setCreatedAt(LocalDateTime.now());
        Tip t2 = createTip("100", "10", "USD", "B", ServiceQuality.GOOD);
        t2.setCreatedAt(LocalDateTime.now().minusMonths(2));
        
        mockTips.add(t1);
        mockTips.add(t2);
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).period(TipGoalPeriod.CURRENT_MONTH).build());
        assertThat(response.currentValue()).isEqualByComparingTo("100");
    }

    @Test
    void calculate_currentYear() {
        Tip t1 = createTip("100", "10", "USD", "A", ServiceQuality.GOOD);
        t1.setCreatedAt(LocalDateTime.now());
        Tip t2 = createTip("100", "10", "USD", "B", ServiceQuality.GOOD);
        t2.setCreatedAt(LocalDateTime.now().minusYears(2));
        
        mockTips.add(t1);
        mockTips.add(t2);
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).period(TipGoalPeriod.CURRENT_YEAR).build());
        assertThat(response.currentValue()).isEqualByComparingTo("100");
    }

    @Test
    void calculate_allTime() {
        Tip t1 = createTip("100", "10", "USD", "A", ServiceQuality.GOOD);
        t1.setCreatedAt(LocalDateTime.now());
        Tip t2 = createTip("100", "10", "USD", "B", ServiceQuality.GOOD);
        t2.setCreatedAt(LocalDateTime.now().minusYears(10));
        
        mockTips.add(t1);
        mockTips.add(t2);
        when(tipRepository.findAllByUserId(any())).thenReturn(mockTips);
        
        var response = calculationService.calculateProgress(goalBuilder.goalType(TipGoalType.TOTAL_TIP_AMOUNT).period(TipGoalPeriod.ALL_TIME).build());
        assertThat(response.currentValue()).isEqualByComparingTo("200");
    }

}
