package com.aitip.service;

import com.aitip.dto.TipAnomalyRecord;
import com.aitip.dto.TipAnomalySeverity;
import com.aitip.dto.TipAnomalyType;
import com.aitip.dto.TipDataQualityResponse;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipDataQualityServiceTest {

    @Mock
    private TipRepository tipRepository;

    @InjectMocks
    private TipDataQualityService service;

    private User testUser;
    private List<Tip> mockTips;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@example.com");
        mockTips = new ArrayList<>();
    }

    private Tip createTip(String amount, String percentage, String currency, String restaurant, LocalDateTime createdAt) {
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setUser(testUser);
        if (amount != null) tip.setBillAmount(new BigDecimal("100.00")); // Hardcode bill to 100 for easy percentage checks unless otherwise
        if (amount != null) tip.setTipAmount(new BigDecimal(amount));
        if (percentage != null) tip.setTipPercentage(new BigDecimal(percentage));
        tip.setCurrency(currency);
        tip.setRestaurantName(restaurant);
        tip.setCreatedAt(createdAt);
        return tip;
    }

    @Test
    void testEmptyHistory() {
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        assertThat(res.totalTips()).isEqualTo(0);
        assertThat(res.cleanTips()).isEqualTo(0);
        assertThat(res.anomalyCount()).isEqualTo(0);
        assertThat(res.message()).isEqualTo("0 / 0 records look consistent.");
    }

    @Test
    void testNormalTip_NoAnomaly() {
        mockTips.add(createTip("20", "20", "USD", "A", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        assertThat(res.anomalyCount()).isEqualTo(0);
        assertThat(res.cleanTips()).isEqualTo(1);
    }

    @Test
    void testZeroPercentTip_NoAnomaly() {
        mockTips.add(createTip("0", "0", "USD", "A", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        assertThat(res.anomalyCount()).isEqualTo(0);
        assertThat(res.cleanTips()).isEqualTo(1);
    }

    @Test
    void test100PercentTip_NoAnomaly() {
        mockTips.add(createTip("100", "100", "USD", "A", LocalDateTime.now())); // >100 is anomaly, 100 is warning if >= 50
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        // 100% falls into >= 50% which is WARNING
        assertThat(res.anomalyCount()).isEqualTo(1);
        assertThat(res.anomalies().get(0).anomalyType()).isEqualTo(TipAnomalyType.UNUSUAL_TIP_PERCENTAGE);
        assertThat(res.anomalies().get(0).severity()).isEqualTo(TipAnomalySeverity.WARNING);
    }

    @Test
    void testGreaterThan100PercentTip_HighAnomaly() {
        mockTips.add(createTip("110", "110", "USD", "A", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.anomalyCount()).isGreaterThanOrEqualTo(1);
        boolean hasHighPercentage = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.UNUSUAL_TIP_PERCENTAGE && a.severity() == TipAnomalySeverity.HIGH);
        assertThat(hasHighPercentage).isTrue();
    }

    @Test
    void testNegativePercentageTip_HighAnomaly() {
        mockTips.add(createTip("-10", "-10", "USD", "A", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        boolean hasHighPercentage = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.UNUSUAL_TIP_PERCENTAGE && a.severity() == TipAnomalySeverity.HIGH);
        assertThat(hasHighPercentage).isTrue();
    }

    @Test
    void testExtremeTipAmount_DerivedPercentage() {
        Tip tip = createTip("200", "200", "USD", "A", LocalDateTime.now());
        tip.setBillAmount(new BigDecimal("100"));
        tip.setTipAmount(new BigDecimal("150"));
        mockTips.add(tip);
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        boolean hasExtreme = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.EXTREME_TIP_AMOUNT && a.severity() == TipAnomalySeverity.HIGH);
        assertThat(hasExtreme).isTrue();
    }

    @Test
    void testMissingRestaurant() {
        mockTips.add(createTip("20", "20", "USD", "", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        boolean hasMissingRest = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.MISSING_RESTAURANT && a.severity() == TipAnomalySeverity.INFO);
        assertThat(hasMissingRest).isTrue();
    }

    @Test
    void testMissingCurrency() {
        mockTips.add(createTip("20", "20", null, "A", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        boolean hasInvalidCurr = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.INVALID_CURRENCY && a.severity() == TipAnomalySeverity.HIGH);
        assertThat(hasInvalidCurr).isTrue();
    }

    @Test
    void testInvalidCurrency() {
        mockTips.add(createTip("20", "20", "US", "A", LocalDateTime.now()));
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        boolean hasInvalidCurr = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.INVALID_CURRENCY && a.severity() == TipAnomalySeverity.HIGH);
        assertThat(hasInvalidCurr).isTrue();
    }

    @Test
    void testDuplicateLikeRecords() {
        LocalDateTime now = LocalDateTime.now();
        mockTips.add(createTip("20", "20", "USD", "A", now));
        mockTips.add(createTip("20", "20", "USD", "A", now.plusMinutes(1)));
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.anomalyCount()).isEqualTo(2); // One for each tip
        boolean hasDuplicate = res.anomalies().stream().allMatch(a -> a.anomalyType() == TipAnomalyType.DUPLICATE_LIKE_RECORD && a.severity() == TipAnomalySeverity.WARNING);
        assertThat(hasDuplicate).isTrue();
        assertThat(res.cleanTips()).isEqualTo(0);
    }
    
    @Test
    void testDuplicateLikeRecords_NotDupeDueToTime() {
        LocalDateTime now = LocalDateTime.now();
        mockTips.add(createTip("20", "20", "USD", "A", now));
        mockTips.add(createTip("20", "20", "USD", "A", now.plusMinutes(5))); // outside 2 mins
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.anomalyCount()).isEqualTo(0);
    }

    @Test
    void testRestaurantNormalizationDuplicate() {
        LocalDateTime now = LocalDateTime.now();
        mockTips.add(createTip("20", "20", "USD", "Italian Place", now));
        mockTips.add(createTip("20", "20", "usd", " italian   place ", now));
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.anomalyCount()).isEqualTo(2);
    }

    @Test
    void testCurrencyIsolationDuplicate() {
        LocalDateTime now = LocalDateTime.now();
        mockTips.add(createTip("20", "20", "USD", "A", now));
        mockTips.add(createTip("20", "20", "INR", "A", now));
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.anomalyCount()).isEqualTo(0);
    }

    @Test
    void testStatisticalOutlier_InsufficientSample() {
        mockTips.add(createTip("20", "20", "USD", "A", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "B", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "C", LocalDateTime.now()));
        mockTips.add(createTip("99", "99", "USD", "D", LocalDateTime.now())); // Outlier but sample size is 4
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        // The 99% tip will trigger WARNING because of >=50% rule, but NOT statistical outlier because size < 5
        long outlierCount = res.anomalies().stream().filter(a -> a.reason().contains("Statistical")).count();
        assertThat(outlierCount).isEqualTo(0);
    }

    @Test
    void testStatisticalOutlier_OddMedian() {
        mockTips.add(createTip("20", "20", "USD", "A", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "B", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "C", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "D", LocalDateTime.now()));
        mockTips.add(createTip("45", "45", "USD", "E", LocalDateTime.now())); // Sample = 5. Median = 20. Outlier 45 - 20 >= 20. 
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        long outlierCount = res.anomalies().stream().filter(a -> a.reason().contains("Statistical")).count();
        assertThat(outlierCount).isEqualTo(1);
    }

    @Test
    void testStatisticalOutlier_EvenMedian() {
        mockTips.add(createTip("20", "20", "USD", "A", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "B", LocalDateTime.now()));
        mockTips.add(createTip("22", "22", "USD", "C", LocalDateTime.now())); // Median of 20 & 22 = 21
        mockTips.add(createTip("22", "22", "USD", "D", LocalDateTime.now()));
        mockTips.add(createTip("22", "22", "USD", "E", LocalDateTime.now()));
        mockTips.add(createTip("45", "45", "USD", "F", LocalDateTime.now())); // 45 - 21 = 24 >= 20
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        long outlierCount = res.anomalies().stream().filter(a -> a.reason().contains("Statistical")).count();
        assertThat(outlierCount).isEqualTo(1);
    }

    @Test
    void testMultipleAnomaliesOneRecord() {
        Tip tip = createTip("150", "150", "", "", LocalDateTime.now());
        tip.setBillAmount(new BigDecimal("100"));
        mockTips.add(tip);
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.cleanTips()).isEqualTo(0);
        // MISSING_RESTAURANT, INVALID_CURRENCY, UNUSUAL_TIP_PERCENTAGE (HIGH), EXTREME_TIP_AMOUNT
        assertThat(res.anomalyCount()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void testSeverityClassification() {
        mockTips.add(createTip("20", "20", "US", "A", LocalDateTime.now())); // HIGH
        mockTips.add(createTip("50", "50", "USD", "B", LocalDateTime.now())); // WARNING
        mockTips.add(createTip("20", "20", "USD", "", LocalDateTime.now())); // INFO
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        assertThat(res.highSeverityCount()).isEqualTo(1);
        assertThat(res.warningCount()).isEqualTo(1);
        assertThat(res.infoCount()).isEqualTo(1);
    }

    @Test
    void testFilteringByCurrency() {
        mockTips.add(createTip("150", "150", "USD", "A", LocalDateTime.now()));
        mockTips.add(createTip("150", "150", "INR", "B", LocalDateTime.now()));
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, "USD", null, null);
        
        assertThat(res.anomalyCount()).isGreaterThan(0);
        assertThat(res.anomalies().stream().allMatch(a -> a.currency().equals("USD"))).isTrue();
    }

    @Test
    void testFilteringBySeverity() {
        mockTips.add(createTip("20", "20", "USD", "", LocalDateTime.now())); // INFO
        mockTips.add(createTip("150", "150", "USD", "B", LocalDateTime.now())); // HIGH
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, TipAnomalySeverity.INFO, null);
        
        assertThat(res.anomalies().size()).isEqualTo(1);
        assertThat(res.anomalies().get(0).severity()).isEqualTo(TipAnomalySeverity.INFO);
    }

    @Test
    void testFilteringByType() {
        mockTips.add(createTip("20", "20", "USD", "", LocalDateTime.now())); // MISSING_RESTAURANT
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, TipAnomalyType.MISSING_RESTAURANT);
        
        assertThat(res.anomalies().size()).isEqualTo(1);
    }

    @Test
    void testUserIsolation() {
        // Just verify that the service only fetches by testUser.getId()
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        service.analyzeDataQuality(testUser, null, null, null);
    }

    @Test
    void testMissingFinancialValues() {
        Tip tip = new Tip();
        tip.setId(UUID.randomUUID());
        tip.setCurrency("USD");
        tip.setRestaurantName("A");
        tip.setCreatedAt(LocalDateTime.now());
        mockTips.add(tip);
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        boolean hasMissingAmount = res.anomalies().stream().anyMatch(a -> a.anomalyType() == TipAnomalyType.UNUSUAL_BILL_AMOUNT && a.severity() == TipAnomalySeverity.HIGH);
        assertThat(hasMissingAmount).isTrue();
    }

    @Test
    void testValidOutlierWithHighPercentageAlreadyFlagged() {
        // If a tip is 150%, it's already flagged as UNUSUAL_TIP_PERCENTAGE (HIGH).
        // It should NOT be flagged AGAIN as Statistical outlier (WARNING) for the same type.
        mockTips.add(createTip("20", "20", "USD", "A", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "B", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "C", LocalDateTime.now()));
        mockTips.add(createTip("20", "20", "USD", "D", LocalDateTime.now()));
        mockTips.add(createTip("150", "150", "USD", "E", LocalDateTime.now()));
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(mockTips);
        TipDataQualityResponse res = service.analyzeDataQuality(testUser, null, null, null);
        
        long percentageAnomalies = res.anomalies().stream()
                .filter(a -> a.tipId().equals(mockTips.get(4).getId()))
                .filter(a -> a.anomalyType() == TipAnomalyType.UNUSUAL_TIP_PERCENTAGE)
                .count();
                
        assertThat(percentageAnomalies).isEqualTo(1);
    }
}
