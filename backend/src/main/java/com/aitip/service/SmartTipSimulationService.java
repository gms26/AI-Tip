package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class SmartTipSimulationService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipSimulationService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final SmartTipService smartTipService;
    private final TipBudgetService tipBudgetService;
    private final TipGoalService tipGoalService;
    private final AiProvider AiProvider;

    public SmartTipSimulationService(SmartTipService smartTipService,
                                     TipBudgetService tipBudgetService,
                                     TipGoalService tipGoalService,
                                     AiProvider AiProvider) {
        this.smartTipService = smartTipService;
        this.tipBudgetService = tipBudgetService;
        this.tipGoalService = tipGoalService;
        this.AiProvider = AiProvider;
    }

    public SmartTipSimulationResponse simulate(String email, SmartTipSimulationRequest request) {
        log.debug("Simulating tip for user: {}, request: {}", email, request);

        // 1. Get current contextual recommendation
        SmartTipRequest currentRequest = new SmartTipRequest(
                request.currency(),
                request.billAmount(),
                request.restaurantName(),
                request.serviceQuality(),
                request.tipPercentage()
        );
        SmartTipResponse currentResponse = smartTipService.getSmartTip(
                email, currentRequest
        );

        BigDecimal currentTipPct = currentResponse.primarySuggestion().tipPercentage();

        // 2. Calculate current state
        BigDecimal currentTipAmount = request.billAmount()
                .multiply(currentTipPct)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal currentTotal = request.billAmount().add(currentTipAmount);

        SmartTipSimulationState currentState = new SmartTipSimulationState(
                request.billAmount(),
                currentTipPct,
                currentTipAmount,
                currentTotal
        );

        // 3. Calculate simulated state
        BigDecimal simulatedTipAmount = request.billAmount()
                .multiply(request.tipPercentage())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal simulatedTotal = request.billAmount().add(simulatedTipAmount);

        SmartTipSimulationState simulatedState = new SmartTipSimulationState(
                request.billAmount(),
                request.tipPercentage(),
                simulatedTipAmount,
                simulatedTotal
        );

        // 4. Calculate differences
        BigDecimal tipAmountDiff = simulatedTipAmount.subtract(currentTipAmount);
        BigDecimal totalAmountDiff = simulatedTotal.subtract(currentTotal);
        BigDecimal pctPointDiff = request.tipPercentage().subtract(currentTipPct);

        SmartTipSimulationDifference difference = new SmartTipSimulationDifference(
                tipAmountDiff,
                totalAmountDiff,
                pctPointDiff
        );

        // 5. Budget impact analysis
        String budgetImpact = analyzeBudgetImpact(email, request.currency(), simulatedTipAmount);

        // 6. Goal impact analysis
        String goalImpact = analyzeGoalImpact(email, request.currency(), simulatedTipAmount, request.restaurantName(), request.serviceQuality());

        // 7. Deterministic explanation
        String explanation = buildExplanation(currentTipPct, request.tipPercentage(), tipAmountDiff, request.currency());

        // 8. Optional Groq explanation
        String aiExplanation = null;
        try {
            java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> AiProvider.generateSimulationExplanation(
                            currentTipPct,
                            request.tipPercentage(),
                            tipAmountDiff,
                            request.currency(),
                            budgetImpact,
                            goalImpact
                    ));
            aiExplanation = future.get(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to generate AI simulation explanation or timed out: {}", e.getMessage());
        }

        return new SmartTipSimulationResponse(
                request.currency(),
                currentState,
                simulatedState,
                difference,
                budgetImpact,
                goalImpact,
                explanation,
                aiExplanation
        );
    }

    private String analyzeBudgetImpact(String email, String currency, BigDecimal simulatedTipAmount) {
        try {
            TipBudgetStatusResponse status = tipBudgetService.getBudgetStatus(email, currency);
            BigDecimal newSpent = status.currentMonthTips().add(simulatedTipAmount);
            BigDecimal remaining = status.monthlyLimit().subtract(newSpent);

            if (newSpent.compareTo(status.monthlyLimit()) > 0) {
                return String.format("Exceeds %s monthly budget by %s", currency, remaining.abs().toPlainString());
            } else if (newSpent.compareTo(status.monthlyLimit()) == 0) {
                return String.format("Exactly reaches your %s monthly budget", currency);
            } else {
                return String.format("Leaves %s %s remaining in monthly budget", remaining.toPlainString(), currency);
            }
        } catch (ResourceNotFoundException e) {
            return "No active budget for " + currency;
        }
    }

    private String analyzeGoalImpact(String email, String currency, BigDecimal simulatedTipAmount, String restaurantName, ServiceQuality serviceQuality) {
        List<TipGoalProgressResponse> goals = tipGoalService.getGoals(email);
        
        long activeApplicableGoals = goals.stream()
                .filter(g -> g.status().name().equals("ACTIVE"))
                .filter(g -> isGoalApplicable(g, currency, restaurantName, serviceQuality))
                .count();

        if (activeApplicableGoals == 0) {
            return "Does not impact any active goals.";
        }

        return String.format("Impacts %d active goal(s). Details available in Goals dashboard.", activeApplicableGoals);
    }

    private boolean isGoalApplicable(TipGoalProgressResponse goal, String currency, String restaurantName, ServiceQuality serviceQuality) {
        if (goal.currency() != null && !goal.currency().equals(currency)) return false;
        if (goal.restaurantName() != null && !goal.restaurantName().equalsIgnoreCase(restaurantName)) return false;
        if (goal.serviceQuality() != null && goal.serviceQuality() != serviceQuality) return false;
        return true;
    }

    private String buildExplanation(BigDecimal currentPct, BigDecimal simPct, BigDecimal amountDiff, String currency) {
        int comparison = simPct.compareTo(currentPct);
        if (comparison == 0) {
            return "This matches the recommended tip exactly.";
        } else if (comparison > 0) {
            return String.format("This is more generous than the recommendation by %s %s.", amountDiff.toPlainString(), currency);
        } else {
            return String.format("This is more conservative than the recommendation, saving you %s %s.", amountDiff.abs().toPlainString(), currency);
        }
    }
}
