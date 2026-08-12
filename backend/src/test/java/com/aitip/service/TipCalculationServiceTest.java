package com.aitip.service;

import com.aitip.dto.SplitRequest;
import com.aitip.dto.SplitResponse;
import com.aitip.exception.InvalidSplitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TipCalculationServiceTest {

    private TipCalculationService tipCalculationService;

    @BeforeEach
    void setUp() {
        tipCalculationService = new TipCalculationService();
    }

    @Test
    void testBill0_01() {
        BigDecimal bill = new BigDecimal("0.01");
        BigDecimal tipPercentage = new BigDecimal("20.00");
        
        BigDecimal tipAmount = tipCalculationService.calculateTipAmount(bill, tipPercentage);
        BigDecimal totalAmount = tipCalculationService.calculateTotalAmount(bill, tipAmount);
        
        // 0.01 * 0.20 = 0.002, rounds to 0.00. Total should be 0.01.
        assertEquals(new BigDecimal("0.00"), tipAmount);
        assertEquals(new BigDecimal("0.01"), totalAmount);
    }

    @Test
    void testTip0Percent() {
        BigDecimal bill = new BigDecimal("100.00");
        BigDecimal tipPercentage = new BigDecimal("0.00");
        
        BigDecimal tipAmount = tipCalculationService.calculateTipAmount(bill, tipPercentage);
        BigDecimal totalAmount = tipCalculationService.calculateTotalAmount(bill, tipAmount);
        
        assertEquals(new BigDecimal("0.00"), tipAmount);
        assertEquals(new BigDecimal("100.00"), totalAmount);
    }

    @Test
    void testTip100Percent() {
        BigDecimal bill = new BigDecimal("100.00");
        BigDecimal tipPercentage = new BigDecimal("100.00");
        
        BigDecimal tipAmount = tipCalculationService.calculateTipAmount(bill, tipPercentage);
        BigDecimal totalAmount = tipCalculationService.calculateTotalAmount(bill, tipAmount);
        
        assertEquals(new BigDecimal("100.00"), tipAmount);
        assertEquals(new BigDecimal("200.00"), totalAmount);
    }

    @Test
    void testSplit10DollarsAmong3People() {
        // Total bill is 10.00 (inclusive of tip for this scenario to test the division logic)
        // Let's set bill to 10.00 and tip to 0%
        SplitRequest request = new SplitRequest(
                new BigDecimal("10.00"),
                new BigDecimal("0.00"),
                List.of("Alice", "Bob", "Charlie"),
                null
        );

        SplitResponse response = tipCalculationService.calculateSplit(request);
        
        assertEquals(3, response.numberOfPeople());
        
        // 10 / 3 = 3.33 each. 3.33 * 3 = 9.99. Remainder 0.01 goes to first person.
        // So: Alice = 3.34, Bob = 3.33, Charlie = 3.33
        assertEquals(new BigDecimal("3.34"), response.splits().get(0).amountOwed());
        assertEquals("Alice", response.splits().get(0).name());
        
        assertEquals(new BigDecimal("3.33"), response.splits().get(1).amountOwed());
        assertEquals("Bob", response.splits().get(1).name());
        
        assertEquals(new BigDecimal("3.33"), response.splits().get(2).amountOwed());
        assertEquals("Charlie", response.splits().get(2).name());
    }

    @Test
    void testCustomSplitFailsIfTotalsDoNotMatch() {
        // Bill = 100, Tip = 20% -> Total = 120.
        SplitRequest request = new SplitRequest(
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                null,
                Map.of("Alice", new BigDecimal("50.00"), "Bob", new BigDecimal("50.00")) // Sum is 100, not 120
        );

        InvalidSplitException exception = assertThrows(InvalidSplitException.class, () -> {
            tipCalculationService.calculateSplit(request);
        });

        assertTrue(exception.getMessage().contains("does not match the calculated total"));
    }
}
