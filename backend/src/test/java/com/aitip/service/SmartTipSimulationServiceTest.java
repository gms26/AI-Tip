package com.aitip.service;

import com.aitip.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartTipSimulationServiceTest {

    @Mock
    private SmartTipService smartTipService;
    
    @Mock
    private TipBudgetService tipBudgetService;
    
    @Mock
    private TipGoalService tipGoalService;
    
    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private SmartTipSimulationService simulationService;

    private SmartTipSimulationRequest request;
    private SmartTipResponse mockResponse;

    @BeforeEach
    void setUp() {
        request = new SmartTipSimulationRequest(
                "USD",
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                "Test Restaurant",
                ServiceQuality.EXCELLENT
        );

        SmartTipSuggestion currentSuggestion = new SmartTipSuggestion(
                SmartTipSuggestionType.MODERATE,
                new BigDecimal("15.00"),
                new BigDecimal("15.00"),
                new BigDecimal("115.00"),
                "Standard tip",
                "Reason",
                new BigDecimal("0.00"),
                "Impact",
                true
        );

        mockResponse = new SmartTipResponse(
                "USD", new BigDecimal("100.00"), null, null, null, null,
                TipBudgetStatus.UNDER_BUDGET, new BigDecimal("0.00"), false,
                TipEvolutionDirection.STABLE, 0, null, null, 0, null,
                Collections.singletonList(currentSuggestion), currentSuggestion,
                "Message", "AI Explanation", 0L, SmartTipFeedbackDirection.INSUFFICIENT_DATA,
                new BigDecimal("0.00"), false, new BigDecimal("0.00"), "Adaptation message",
                currentSuggestion, null, true
        );
    }

    @Test
    void simulate_CalculatesDifferencesCorrectly() {
        when(smartTipService.getSmartTip(eq("test@example.com"), any(SmartTipRequest.class)))
                .thenReturn(mockResponse);

        TipBudgetStatusResponse budgetStatus = new TipBudgetStatusResponse(
                "USD", new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("150.00"),
                new BigDecimal("25.00"), new BigDecimal("80.00"), TipBudgetStatus.UNDER_BUDGET, 1,
                BudgetConfidence.LOW, "Message"
        );
        when(tipBudgetService.getBudgetStatus(eq("test@example.com"), eq("USD")))
                .thenReturn(budgetStatus);

        when(tipGoalService.getGoals(eq("test@example.com")))
                .thenReturn(Collections.emptyList());

        when(geminiService.generateSimulationExplanation(any(), any(), any(), any(), any(), any()))
                .thenReturn("AI test explanation");

        SmartTipSimulationResponse response = simulationService.simulate("test@example.com", request);

        assertNotNull(response);
        assertEquals("USD", response.currency());
        
        // Current state checks (15% of 100 = 15)
        assertEquals(new BigDecimal("15.00"), response.current().tipPercentage());
        assertEquals(new BigDecimal("15.00"), response.current().tipAmount());
        assertEquals(new BigDecimal("115.00"), response.current().totalAmount());

        // Simulated state checks (20% of 100 = 20)
        assertEquals(new BigDecimal("20.00"), response.simulated().tipPercentage());
        assertEquals(new BigDecimal("20.00"), response.simulated().tipAmount());
        assertEquals(new BigDecimal("120.00"), response.simulated().totalAmount());

        // Difference checks
        assertEquals(new BigDecimal("5.00"), response.difference().percentagePointDifference());
        assertEquals(new BigDecimal("5.00"), response.difference().tipAmountDifference());
        assertEquals(new BigDecimal("5.00"), response.difference().totalAmountDifference());

        // Budget impact
        assertEquals("Leaves 130.00 USD remaining in monthly budget", response.budgetImpact());

        // Explanations
        assertEquals("This is more generous than the recommendation by 5.00 USD.", response.explanation());
        assertEquals("AI test explanation", response.aiExplanation());
    }

    @Test
    void simulate_ExceedsBudget_CalculatesCorrectly() {
        when(smartTipService.getSmartTip(eq("test@example.com"), any(SmartTipRequest.class)))
                .thenReturn(mockResponse);

        // Budget is 200, spent is 190, simulation tip is 20. Total = 210. Exceeds by 10.
        TipBudgetStatusResponse budgetStatus = new TipBudgetStatusResponse(
                "USD", new BigDecimal("200.00"), new BigDecimal("190.00"), new BigDecimal("10.00"),
                new BigDecimal("95.00"), new BigDecimal("80.00"), TipBudgetStatus.APPROACHING_LIMIT, 1,
                BudgetConfidence.LOW, "Message"
        );
        when(tipBudgetService.getBudgetStatus(eq("test@example.com"), eq("USD")))
                .thenReturn(budgetStatus);

        SmartTipSimulationResponse response = simulationService.simulate("test@example.com", request);

        assertEquals("Exceeds USD monthly budget by 10.00", response.budgetImpact());
    }
}
