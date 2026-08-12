package com.aitip.service;

import com.aitip.dto.CurrencyTipSummary;
import com.aitip.dto.TaxPeriod;
import com.aitip.dto.TaxSummaryRequest;
import com.aitip.dto.TaxSummaryResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxSummaryServiceTest {

    @Mock
    private TipRepository tipRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaxSummaryService taxSummaryService;

    private User testUser;
    private final String EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(EMAIL);
    }

    @Test
    void getSummary_withEmptyHistory_returnsEmptyState() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(new ArrayList<>());

        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("100"));
        TaxSummaryResponse response = taxSummaryService.getSummary(EMAIL, request);

        // Verification of repository query parameters (proving arguments passed to DB are correctly scoped)
        verify(tipRepository).findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any());

        assertEquals(0, response.tipCount());
        assertEquals(BigDecimal.ZERO, response.totalTips());
        assertEquals(BigDecimal.ZERO, response.estimatedTaxableTips());
        assertNull(response.averageTip());
        assertNull(response.medianTip());
        assertTrue(response.currencyBreakdown().isEmpty());
    }

    @Test
    void getSummary_withSingleCurrency_returnsParentMetrics() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        tips.add(createTip("USD", "100.00"));
        tips.add(createTip("USD", "50.00"));
        tips.add(createTip("USD", "25.00"));
        
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("50"));
        TaxSummaryResponse response = taxSummaryService.getSummary(EMAIL, request);

        // median = 50, avg = 175/3 = 58.33, total = 175. est = 87.50
        assertEquals(3, response.tipCount());
        assertEquals(new BigDecimal("175.00"), response.totalTips());
        assertEquals(new BigDecimal("87.50"), response.estimatedTaxableTips());
        assertEquals(new BigDecimal("58.33"), response.averageTip());
        assertEquals(new BigDecimal("50.00"), response.medianTip());
        
        assertEquals(1, response.currencyBreakdown().size());
    }

    @Test
    void getSummary_withMultipleCurrencies_returnsNullParentMetrics() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        tips.add(createTip("USD", "200.00"));
        tips.add(createTip("INR", "5000.00"));
        
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("100"));
        TaxSummaryResponse response = taxSummaryService.getSummary(EMAIL, request);

        assertNull(response.totalTips());
        assertNull(response.tipCount());
        assertNull(response.averageTip());
        assertNull(response.medianTip());
        assertNull(response.estimatedTaxableTips());
        
        assertEquals(2, response.currencyBreakdown().size());
        
        CurrencyTipSummary usd = response.currencyBreakdown().stream().filter(c -> c.currency().equals("USD")).findFirst().get();
        assertEquals(new BigDecimal("200.00"), usd.totalTips());
        
        CurrencyTipSummary inr = response.currencyBreakdown().stream().filter(c -> c.currency().equals("INR")).findFirst().get();
        assertEquals(new BigDecimal("5000.00"), inr.totalTips());
    }

    @Test
    void getSummary_withInvalidPercentage_throwsException() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("150"));
        assertThrows(IllegalArgumentException.class, () -> {
            taxSummaryService.getSummary(EMAIL, request);
        });

        TaxSummaryRequest requestNeg = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("-10"));
        assertThrows(IllegalArgumentException.class, () -> {
            taxSummaryService.getSummary(EMAIL, requestNeg);
        });
    }

    @Test
    void getSummary_withCustomPeriodInvalidDates_throwsException() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        TaxSummaryRequest request = new TaxSummaryRequest(
            TaxPeriod.CUSTOM, 
            LocalDate.of(2026, 9, 1), 
            LocalDate.of(2026, 8, 1), 
            new BigDecimal("100")
        );
        
        assertThrows(IllegalArgumentException.class, () -> {
            taxSummaryService.getSummary(EMAIL, request);
        });
    }
    
    @Test
    void getSummary_calculatesEvenMedianCorrectly() {
        when(userService.getUserByEmail(EMAIL)).thenReturn(testUser);
        
        List<Tip> tips = new ArrayList<>();
        tips.add(createTip("USD", "10.00"));
        tips.add(createTip("USD", "20.00"));
        tips.add(createTip("USD", "30.00"));
        tips.add(createTip("USD", "40.00"));
        
        when(tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(testUser.getId()), any(), any()))
                .thenReturn(tips);

        TaxSummaryRequest request = new TaxSummaryRequest(TaxPeriod.CURRENT_MONTH, null, null, new BigDecimal("100"));
        TaxSummaryResponse response = taxSummaryService.getSummary(EMAIL, request);

        // median of 10,20,30,40 is (20+30)/2 = 25
        assertEquals(new BigDecimal("25.00"), response.medianTip());
    }

    private Tip createTip(String currency, String amount) {
        Tip tip = new Tip();
        tip.setCurrency(currency);
        tip.setTipAmount(new BigDecimal(amount));
        return tip;
    }
}
