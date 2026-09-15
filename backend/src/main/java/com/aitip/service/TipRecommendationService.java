package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.util.CurrencyValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipRecommendationService {

    private final UserService userService;
    private final TipRepository tipRepository;
    private final TipBudgetService tipBudgetService;
    private final TipForecastCalculationService tipForecastCalculationService;
    private final TipOptimizationService tipOptimizationService;
    private final TipGoalService tipGoalService;
    private final TipDataQualityService tipDataQualityService;
    private final TipRecommendationActionService tipRecommendationActionService;

    @Transactional(readOnly = true)
    public TipRecommendationResponse getRecommendations(String email, String currencyParam) {
        User user = userService.getUserByEmail(email);
        List<Tip> allTips = tipRepository.findAllByUserId(user.getId());

        String targetCurrency = null;
        Set<String> currenciesToAnalyze = new HashSet<>();
        
        if (currencyParam != null && !currencyParam.isBlank()) {
            targetCurrency = CurrencyValidationUtil.normalizeAndValidate(currencyParam);
            currenciesToAnalyze.add(targetCurrency);
        } else {
            // Find all currencies from tips
            for (Tip tip : allTips) {
                if (tip.getCurrency() != null && !tip.getCurrency().isBlank()) {
                    currenciesToAnalyze.add(tip.getCurrency());
                }
            }
            // Find all currencies from budgets
            tipBudgetService.getBudgets(email).forEach(b -> currenciesToAnalyze.add(b.currency()));
        }

        List<TipRecommendation> recommendations = new ArrayList<>();

        // Generate cross-currency or global rules (like Data Quality & Goals)
        generateDataQualityRecommendations(user, targetCurrency, recommendations);
        generateGoalRecommendations(email, targetCurrency, recommendations);

        // Generate currency-specific rules
        for (String currency : currenciesToAnalyze) {
            generateBudgetRecommendations(email, currency, recommendations);
            generateForecastRecommendations(email, currency, recommendations);
            generateOptimizationRecommendations(email, currency, allTips, recommendations);
            generateGenerosityTrendRecommendations(allTips, currency, recommendations);
        }

        // Determine if there is positive progress to highlight (simplified check for now)
        generatePositiveProgressRecommendations(recommendations);

        // Filter out dismissed/snoozed and annotate reviewed recommendations
        recommendations = tipRecommendationActionService.filterAndAnnotateRecommendations(email, recommendations);

        // Sort and limit
        recommendations.sort(Comparator
                .comparing(TipRecommendation::priority) // HIGH, MEDIUM, LOW (ordinal)
                .thenComparing(TipRecommendation::type));
                
        if (recommendations.size() > 5) {
            recommendations = recommendations.subList(0, 5);
        }

        int highCount = (int) recommendations.stream().filter(r -> r.priority() == TipRecommendationPriority.HIGH).count();
        String summary = String.format("%d recommendations\n%d high priority", recommendations.size(), highCount);

        return new TipRecommendationResponse(
                java.time.LocalDateTime.now(),
                recommendations,
                summary,
                null
        );
    }

    private void generateBudgetRecommendations(String email, String currency, List<TipRecommendation> recommendations) {
        try {
            TipBudgetStatusResponse statusRes = tipBudgetService.getBudgetStatus(email, currency);
            if (statusRes.status() == TipBudgetStatus.OVER_BUDGET || 
                statusRes.status() == TipBudgetStatus.LIMIT_REACHED) {
                recommendations.add(new TipRecommendation(
                        TipRecommendationType.BUDGET_WARNING,
                        TipRecommendationPriority.HIGH,
                        "Budget Limit Reached",
                        "Your current monthly tip spending has reached your budget limit.",
                        currency,
                        statusRes.currentMonthTips().toString(),
                        "Current Spending",
                        "/budgets"
                ));
            } else if (statusRes.status() == TipBudgetStatus.APPROACHING_LIMIT) {
                recommendations.add(new TipRecommendation(
                        TipRecommendationType.BUDGET_WARNING,
                        TipRecommendationPriority.MEDIUM,
                        "Approaching Budget Limit",
                        "You are getting close to your monthly tip budget limit.",
                        currency,
                        statusRes.currentMonthTips().toString(),
                        "Current Spending",
                        "/budgets"
                ));
            }
        } catch (Exception e) {
            // Ignore if no budget exists
        }
    }

    private void generateForecastRecommendations(String email, String currency, List<TipRecommendation> recommendations) {
        try {
            // Check if there is a budget first
            TipBudgetStatusResponse budget = tipBudgetService.getBudgetStatus(email, currency);
            if (budget == null || budget.monthlyLimit() == null) return;
            
            TipForecastResponse forecast = tipForecastCalculationService.forecast(
                    email, currency, TipForecastPeriod.CURRENT_MONTH, budget.monthlyLimit(), 90);
                    
            if (forecast.estimatedMonthlyTipAmount() != null) {
                if (forecast.estimatedMonthlyTipAmount().compareTo(budget.monthlyLimit()) > 0) {
                    recommendations.add(new TipRecommendation(
                            TipRecommendationType.FORECAST_WARNING,
                            TipRecommendationPriority.HIGH,
                            "Projected Over Budget",
                            "Your estimated tipping this month is projected to exceed your budget limit.",
                            currency,
                            forecast.estimatedMonthlyTipAmount().toString(),
                            "Projected Spending",
                            "/tip-forecast"
                    ));
                } else if (budget.warningThreshold() != null) {
                    BigDecimal warningAmt = budget.monthlyLimit().multiply(budget.warningThreshold()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    if (forecast.estimatedMonthlyTipAmount().compareTo(warningAmt) >= 0) {
                        recommendations.add(new TipRecommendation(
                                TipRecommendationType.FORECAST_WARNING,
                                TipRecommendationPriority.MEDIUM,
                                "Projected Near Budget Limit",
                                "Your estimated tipping this month is projected to approach your budget limit.",
                                currency,
                                forecast.estimatedMonthlyTipAmount().toString(),
                                "Projected Spending",
                                "/tip-forecast"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if no budget or forecast fails due to lack of history
        }
    }

    private void generateOptimizationRecommendations(String email, String currency, List<Tip> allTips, List<TipRecommendation> recommendations) {
        List<Tip> currencyTips = allTips.stream()
                .filter(t -> currency.equals(t.getCurrency()) && t.getTipPercentage() != null)
                .sorted(Comparator.comparing(Tip::getCreatedAt).reversed().thenComparing(Tip::getId).reversed())
                .toList();

        if (currencyTips.isEmpty()) return;

        BigDecimal recentTipPercentage = currencyTips.get(0).getTipPercentage(); // Or average of recent? Let's use the most recent or median of recent 5? 
        // Spec says: "If the user's recent/historical behavior is materially outside their calculated optimized range"
        // Let's use median of last 5 for "recent" behavior to be consistent with Generosity Trend.
        int recentCount = Math.min(5, currencyTips.size());
        List<BigDecimal> recentPercentages = currencyTips.subList(0, recentCount).stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();
        
        if (recentPercentages.isEmpty()) return;
        
        BigDecimal recentMedian = calculateMedian(recentPercentages);
        
        TipOptimizationRequest req = new TipOptimizationRequest(currency, recentMedian, null, null);
        try {
            TipOptimizationResponse opt = tipOptimizationService.optimize(email, req);
            if (opt.recommendedMinimumPercentage() != null && opt.recommendedMaximumPercentage() != null) {
                if (recentMedian.compareTo(opt.recommendedMaximumPercentage()) > 0) {
                    recommendations.add(new TipRecommendation(
                            TipRecommendationType.TIP_OPTIMIZATION,
                            TipRecommendationPriority.MEDIUM,
                            "Above Optimized Range",
                            "Your recent tipping pattern is above your calculated typical range.",
                            currency,
                            recentMedian.toString(),
                            "Recent Median",
                            "/tip-optimization"
                    ));
                } else if (recentMedian.compareTo(opt.recommendedMinimumPercentage()) < 0) {
                    recommendations.add(new TipRecommendation(
                            TipRecommendationType.TIP_OPTIMIZATION,
                            TipRecommendationPriority.MEDIUM,
                            "Below Optimized Range",
                            "Your recent tipping pattern is below your historical range.",
                            currency,
                            recentMedian.toString(),
                            "Recent Median",
                            "/tip-optimization"
                    ));
                }
            }
        } catch (Exception e) {}
    }

    private void generateGenerosityTrendRecommendations(List<Tip> allTips, String currency, List<TipRecommendation> recommendations) {
        List<Tip> currencyTips = allTips.stream()
                .filter(t -> currency.equals(t.getCurrency()) && t.getTipPercentage() != null)
                .sorted(Comparator.comparing(Tip::getCreatedAt).reversed().thenComparing(Tip::getId).reversed())
                .toList();

        if (currencyTips.size() < 10) return; // Must have at least 10 tips (5 recent + 5 historical)

        List<BigDecimal> recentPcts = currencyTips.subList(0, 5).stream()
                .map(Tip::getTipPercentage).sorted().toList();
        List<BigDecimal> historicalPcts = currencyTips.subList(5, currencyTips.size()).stream()
                .map(Tip::getTipPercentage).sorted().toList();

        BigDecimal recentMedian = calculateMedian(recentPcts);
        BigDecimal historicalMedian = calculateMedian(historicalPcts);
        
        BigDecimal diff = recentMedian.subtract(historicalMedian);
        BigDecimal threshold = new BigDecimal("3");
        
        if (diff.compareTo(threshold) >= 0) {
            recommendations.add(new TipRecommendation(
                    TipRecommendationType.GENEROUS_TREND,
                    TipRecommendationPriority.MEDIUM,
                    "Generous Trend Detected",
                    "Your recent tipping behavior is noticeably more generous than your historical baseline.",
                    currency,
                    "+" + diff.toString() + " pp",
                    "Difference",
                    "/analytics"
            ));
        } else if (diff.compareTo(threshold.negate()) <= 0) {
            recommendations.add(new TipRecommendation(
                    TipRecommendationType.CONSERVATIVE_TREND,
                    TipRecommendationPriority.MEDIUM,
                    "Conservative Trend Detected",
                    "Your recent tipping behavior is noticeably lower than your historical baseline.",
                    currency,
                    diff.toString() + " pp",
                    "Difference",
                    "/analytics"
            ));
        }
    }

    private void generateGoalRecommendations(String email, String targetCurrency, List<TipRecommendation> recommendations) {
        List<TipGoalProgressResponse> goals = tipGoalService.getGoals(email);
        for (TipGoalProgressResponse goal : goals) {
            if (com.aitip.entity.TipGoalStatus.ACTIVE.equals(goal.status())) {
                if (targetCurrency != null && goal.currency() != null && !targetCurrency.equals(goal.currency())) {
                    continue;
                }
                BigDecimal progress = goal.progressPercentage();
                if (progress != null) {
                    if (progress.compareTo(new BigDecimal("95")) >= 0 && progress.compareTo(new BigDecimal("100")) < 0) {
                        recommendations.add(new TipRecommendation(
                                TipRecommendationType.GOAL_PROGRESS,
                                TipRecommendationPriority.HIGH,
                                "Goal Almost Complete",
                                "You are very close to completing your goal: " + formatGoalType(goal.goalType()),
                                goal.currency(),
                                progress.toString() + "%",
                                "Progress",
                                "/tip-goals"
                        ));
                    } else if (progress.compareTo(new BigDecimal("80")) >= 0 && progress.compareTo(new BigDecimal("95")) < 0) {
                        recommendations.add(new TipRecommendation(
                                TipRecommendationType.GOAL_PROGRESS,
                                TipRecommendationPriority.MEDIUM,
                                "Making Good Progress",
                                "You have made significant progress toward your goal: " + formatGoalType(goal.goalType()),
                                goal.currency(),
                                progress.toString() + "%",
                                "Progress",
                                "/tip-goals"
                        ));
                    }
                }
            }
        }
    }

    private String formatGoalType(Object goalType) {
        if (goalType == null) return "Goal";
        return goalType.toString().replace("_", " ").toLowerCase();
    }

    private void generateDataQualityRecommendations(User user, String targetCurrency, List<TipRecommendation> recommendations) {
        TipDataQualityResponse dq = tipDataQualityService.analyzeDataQuality(user, targetCurrency, null, null);
        if (dq != null && dq.anomalies() != null && !dq.anomalies().isEmpty()) {
            boolean hasHigh = dq.anomalies().stream().anyMatch(a -> TipAnomalySeverity.HIGH == a.severity());
            boolean hasWarning = dq.anomalies().stream().anyMatch(a -> TipAnomalySeverity.WARNING == a.severity());
            
            TipRecommendationPriority priority = TipRecommendationPriority.LOW;
            if (hasHigh) priority = TipRecommendationPriority.HIGH;
            else if (hasWarning) priority = TipRecommendationPriority.MEDIUM;
            
            String msg = priority == TipRecommendationPriority.HIGH ? 
                "You have high-severity anomalies in your tip history that may affect accuracy." :
                "You have some anomalies in your tip history that you may want to review.";

            recommendations.add(new TipRecommendation(
                    TipRecommendationType.DATA_QUALITY,
                    priority,
                    "Data Quality Issues",
                    msg,
                    targetCurrency,
                    String.valueOf(dq.anomalies().size()),
                    "Anomalies Found",
                    "/data-quality"
            ));
        }
    }

    private void generatePositiveProgressRecommendations(List<TipRecommendation> recommendations) {
        // If there's an optimization recommendation and it's positive?
        // Actually, positive progress is defined as "budget status improved, goal reached milestone, tipping more consistent".
        // For simplicity: if the user has no HIGH/MEDIUM budget/forecast warnings, and has made progress on a goal, add a POSITIVE_PROGRESS.
        boolean hasWarnings = recommendations.stream().anyMatch(r -> 
            (r.type() == TipRecommendationType.BUDGET_WARNING || r.type() == TipRecommendationType.FORECAST_WARNING) &&
            (r.priority() == TipRecommendationPriority.HIGH || r.priority() == TipRecommendationPriority.MEDIUM));
            
        if (!hasWarnings && recommendations.stream().anyMatch(r -> r.type() == TipRecommendationType.GOAL_PROGRESS)) {
            recommendations.add(new TipRecommendation(
                    TipRecommendationType.POSITIVE_PROGRESS,
                    TipRecommendationPriority.LOW,
                    "Great Job!",
                    "You are managing your tipping budget well and making progress on your goals.",
                    null,
                    null,
                    null,
                    "/dashboard"
            ));
        }
    }

    private BigDecimal calculateMedian(List<BigDecimal> sortedValues) {
        if (sortedValues == null || sortedValues.isEmpty()) return BigDecimal.ZERO;
        int size = sortedValues.size();
        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        } else {
            BigDecimal left = sortedValues.get((size / 2) - 1);
            BigDecimal right = sortedValues.get(size / 2);
            return left.add(right).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
    }
}
