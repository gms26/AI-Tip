package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipCoachService {

    private final TipBudgetService tipBudgetService;
    private final TipForecastCalculationService tipForecastService;
    private final TipGoalService tipGoalService;
    private final TipDataQualityService tipDataQualityService;
    private final TipProfileService tipProfileService;
    private final TipOptimizationService tipOptimizationService;
    private final TipRecommendationService tipRecommendationService;
    private final UserService userService;
    private final TipCoachPromptBuilder promptBuilder;
    private final AiProvider AiProvider;

    @Transactional(readOnly = true)
    public TipCoachResponse getCoaching(String email, String currency) {
        TipCoachResponse response = new TipCoachResponse();
        response.setGeneratedAt(LocalDateTime.now());

        // 1. Gather existing data defensively
        TipBudgetStatusResponse budget = getBudgetSafe(email, currency);
        TipForecastResponse forecast = getForecastSafe(email, currency);
        List<TipGoalProgressResponse> goals = getGoalsSafe(email);
        TipDataQualityResponse dataQuality = getDataQualitySafe(email, currency);
        TipProfileResponse profile = getProfileSafe(email, currency);
        TipOptimizationResponse optimization = getOptimizationSafe(email, currency, profile);
        TipRecommendationResponse recommendations = getRecommendationsSafe(email, currency);

        // 2. Map data to the nested summary structure
        if (profile != null) {
            response.setProfileSummary(profile);
        }
        
        if (goals != null && !goals.isEmpty()) {
            List<TipGoalProgressResponse> activeGoals = goals.stream()
                    .filter(g -> g.status() == TipGoalStatus.ACTIVE || g.status() == TipGoalStatus.COMPLETED)
                    .sorted((g1, g2) -> Double.compare(g2.progressPercentage().doubleValue(), g1.progressPercentage().doubleValue()))
                    .limit(3)
                    .toList();
            response.setGoalSummary(activeGoals);
        }
        
        if (budget != null && currency != null) {
            response.setBudgetSummary(budget);
        }
        
        if (forecast != null) {
            response.setForecastSummary(forecast);
        }

        if (recommendations != null && recommendations.recommendations() != null) {
            List<TipRecommendation> topRecs = recommendations.recommendations().stream()
                    .limit(3)
                    .toList();
            response.setRecommendations(topRecs);
        }

        if (dataQuality != null) {
            response.setDataQualitySummary(new TipCoachResponse.DataQualitySummary(
                    dataQuality.highSeverityCount(),
                    dataQuality.warningCount(),
                    dataQuality.infoCount()
            ));
        }

        // 3. Determine Focus
        determineFocus(response, budget, forecast, goals, dataQuality, profile, optimization);

        // 4. Generate AI Explanation
        generateAiExplanation(response);

        return response;
    }

    private void determineFocus(TipCoachResponse response, 
                                TipBudgetStatusResponse budget, 
                                TipForecastResponse forecast, 
                                List<TipGoalProgressResponse> goals, 
                                TipDataQualityResponse dataQuality, 
                                TipProfileResponse profile, 
                                TipOptimizationResponse optimization) {
                                    
        long totalTips = profile != null ? profile.totalTipCount() : 0;

        // 1. BUDGET
        if (budget != null && (budget.status() == TipBudgetStatus.OVER_BUDGET 
                || budget.status() == TipBudgetStatus.LIMIT_REACHED 
                || budget.status() == TipBudgetStatus.APPROACHING_LIMIT)) {
            setFocus(response, TipCoachFocus.BUDGET, 1, 
                    "Your tipping budget needs attention.", 
                    "Review your current budget.", 
                    "Your budget is currently " + budget.status().name().replace("_", " ").toLowerCase() + ".");
            return;
        }

        // 2. FORECAST
        if (forecast != null && forecast.budgetStatus() != null) {
            if (forecast.budgetStatus() == TipBudgetStatus.OVER_BUDGET 
                    || forecast.budgetStatus() == TipBudgetStatus.LIMIT_REACHED 
                    || forecast.budgetStatus() == TipBudgetStatus.APPROACHING_LIMIT) {
                setFocus(response, TipCoachFocus.FORECAST, 2, 
                        "Your recent tipping pattern may put pressure on your budget.", 
                        "Review your forecast.", 
                        "Forecast indicates potential budget pressure based on current trends.");
                return;
            }
        }

        // 3. GOAL
        if (goals != null) {
            Optional<TipGoalProgressResponse> nearGoal = goals.stream()
                    .filter(g -> g.status() == TipGoalStatus.ACTIVE && g.progressPercentage().doubleValue() >= 80.0)
                    .max(Comparator.comparingDouble(g -> g.progressPercentage().doubleValue()));
            
            if (nearGoal.isPresent()) {
                setFocus(response, TipCoachFocus.GOAL, 3, 
                        "You're close to completing a tipping goal.", 
                        "Review your active goal.", 
                        "You are " + String.format("%.1f%%", nearGoal.get().progressPercentage().doubleValue()) + " towards completing your " + nearGoal.get().goalType().name().toLowerCase() + " goal.");
                return;
            }
        }

        // 4. DATA QUALITY
        if (dataQuality != null && (dataQuality.highSeverityCount() > 0 || dataQuality.warningCount() > 0)) {
            setFocus(response, TipCoachFocus.DATA_QUALITY, 4, 
                    "A few historical records may need attention.", 
                    "Review flagged tip records.", 
                    "You have " + (dataQuality.highSeverityCount() + dataQuality.warningCount()) + " anomaly warnings in your data.");
            return;
        }

        // 5. CONSISTENCY
        if (profile != null) {
            if (profile.overallBehaviorType() == TipBehaviorType.HIGHLY_VARIABLE 
                    || profile.overallBehaviorType() == TipBehaviorType.VARIABLE) {
                setFocus(response, TipCoachFocus.CONSISTENCY, 5, 
                        "Your tipping pattern has some variation worth understanding.", 
                        "Review your tipping profile.", 
                        "Your tips show variable behavior which may impact accurate budgeting.");
                return;
            }
        }

        // 6. TIP OPTIMIZATION
        if (optimization != null && optimization.potentialMonthlyDifference() != null) {
             if (optimization.potentialMonthlyDifference().compareTo(BigDecimal.ZERO) > 0) {
                 setFocus(response, TipCoachFocus.TIP_OPTIMIZATION, 6, 
                         "Your typical tipping pattern differs from your historical optimized range.", 
                         "Review your optimized tipping range.", 
                         "You have potential optimization opportunities based on past data.");
                 return;
             }
        }

        // 7. EXPLORATION
        if (totalTips >= 10 && profile != null && profile.topRestaurants() != null) {
            if (profile.topRestaurants().size() < 3) {
                setFocus(response, TipCoachFocus.EXPLORATION, 7, 
                        "You may benefit from exploring more restaurants.", 
                        "Explore a different restaurant.", 
                        "Most of your tips are concentrated at a few locations.");
                return;
            }
        }

        // 8. POSITIVE PROGRESS (No actionable concerns)
        if (totalTips >= 5) {
            setFocus(response, TipCoachFocus.POSITIVE_PROGRESS, 8, 
                    "Your tipping habits are progressing well.", 
                    "Keep up the good work.", 
                    "You have no urgent budget, forecast, or data quality issues.");
            return;
        }

        // 9. NO ACTION
        setFocus(response, TipCoachFocus.NO_ACTION, 9, 
                "Keep building your tipping history.", 
                "Add more tips.", 
                "Keep recording a few more tips and your Smart Tipping Coach will become more personalized.");
    }

    private void setFocus(TipCoachResponse response, TipCoachFocus focus, int priority, String headline, String nextAction, String summary) {
        response.setFocus(focus);
        response.setFocusPriority(priority);
        response.setHeadline(headline);
        response.setNextAction(nextAction);
        response.setSummary(summary);
    }

    private void generateAiExplanation(TipCoachResponse response) {
        try {
            String prompt = promptBuilder.buildPrompt(response);
            java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> AiProvider.getRecommendation(prompt));
            String aiExplanation = future.get(3, java.util.concurrent.TimeUnit.SECONDS);
            response.setAiExplanation(aiExplanation);
        } catch (Exception e) {
            log.warn("Failed to generate AI explanation for Coach or timed out: {}", e.getMessage());
            response.setAiExplanation(null);
        }
    }

    // Safe Fetchers
    private TipBudgetStatusResponse getBudgetSafe(String email, String currency) {
        if (currency == null) return null;
        try { return tipBudgetService.getBudgetStatus(email, currency); } 
        catch (Exception e) { log.debug("Failed to load budget", e); return null; }
    }

    private TipForecastResponse getForecastSafe(String email, String currency) {
        if (currency == null) return null;
        try { return tipForecastService.forecast(email, currency, TipForecastPeriod.CURRENT_MONTH, null, null); } 
        catch (Exception e) { log.debug("Failed to load forecast", e); return null; }
    }

    private List<TipGoalProgressResponse> getGoalsSafe(String email) {
        try { return tipGoalService.getGoals(email); } 
        catch (Exception e) { log.debug("Failed to load goals", e); return null; }
    }

    private TipDataQualityResponse getDataQualitySafe(String email, String currency) {
        try { 
            User user = userService.getUserByEmail(email);
            return tipDataQualityService.analyzeDataQuality(user, currency, null, null); 
        } 
        catch (Exception e) { log.debug("Failed to load data quality", e); return null; }
    }

    private TipProfileResponse getProfileSafe(String email, String currency) {
        try { return tipProfileService.getProfile(email, currency); } 
        catch (Exception e) { log.debug("Failed to load profile", e); return null; }
    }

    private TipOptimizationResponse getOptimizationSafe(String email, String currency, TipProfileResponse profile) {
        if (currency == null || profile == null || profile.recentMedianTipPercentage() == null) return null;
        try { 
            TipOptimizationRequest req = new TipOptimizationRequest(currency, profile.recentMedianTipPercentage(), null, null);
            return tipOptimizationService.optimize(email, req); 
        } 
        catch (Exception e) { log.debug("Failed to load optimization", e); return null; }
    }

    private TipRecommendationResponse getRecommendationsSafe(String email, String currency) {
        try { return tipRecommendationService.getRecommendations(email, currency); } 
        catch (Exception e) { log.debug("Failed to load recommendations", e); return null; }
    }
}
