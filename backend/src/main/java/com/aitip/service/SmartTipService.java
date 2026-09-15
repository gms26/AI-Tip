package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.util.CurrencyValidationUtil;
import com.aitip.util.TipCalculationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.*;

/**
 * Context-Aware Smart Tip Assistant Service (Day 31).
 *
 * <p><b>Core Principle:</b> Provides purely advisory, personalized tip options
 * before the user saves a tip. It does NOT automatically save a tip, modify budgets,
 * change goals, or make financial decisions on the user's behalf.</p>
 *
 * <p><b>Stateless & Dynamic:</b> Computes all suggestions on the fly from existing
 * user history, budget status, goals, and optimization boundaries. Zero new database
 * tables or persisted suggestion snapshots.</p>
 */
@Service
public class SmartTipService {

    private static final Logger log = LoggerFactory.getLogger(SmartTipService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal MIN_VALID_BILL = new BigDecimal("0.01");

    private final TipRepository tipRepository;
    private final UserService userService;
    private final TipOptimizationService tipOptimizationService;
    private final TipBudgetService tipBudgetService;
    private final TipGoalService tipGoalService;
    private final TipEvolutionService tipEvolutionService;
    private final SmartTipAdaptationService smartTipAdaptationService;
    private final SmartTipDecisionMemoryService smartTipDecisionMemoryService;
    private final SmartTipPersonalizationService smartTipPersonalizationService;
    private final Clock clock;

    @Autowired
    public SmartTipService(TipRepository tipRepository,
                           UserService userService,
                           TipOptimizationService tipOptimizationService,
                           TipBudgetService tipBudgetService,
                           TipGoalService tipGoalService,
                           TipEvolutionService tipEvolutionService,
                           @Autowired(required = false) SmartTipAdaptationService smartTipAdaptationService,
                           @Autowired(required = false) SmartTipDecisionMemoryService smartTipDecisionMemoryService,
                           @Autowired(required = false) SmartTipPersonalizationService smartTipPersonalizationService) {
        this(tipRepository, userService, tipOptimizationService, tipBudgetService,
                tipGoalService, tipEvolutionService, smartTipAdaptationService, smartTipDecisionMemoryService, smartTipPersonalizationService, Clock.systemDefaultZone());
    }

    public SmartTipService(TipRepository tipRepository,
                           UserService userService,
                           TipOptimizationService tipOptimizationService,
                           TipBudgetService tipBudgetService,
                           TipGoalService tipGoalService,
                           TipEvolutionService tipEvolutionService) {
        this(tipRepository, userService, tipOptimizationService, tipBudgetService,
                tipGoalService, tipEvolutionService, null, null, null, Clock.systemDefaultZone());
    }

    public SmartTipService(TipRepository tipRepository,
                           UserService userService,
                           TipOptimizationService tipOptimizationService,
                           TipBudgetService tipBudgetService,
                           TipGoalService tipGoalService,
                           TipEvolutionService tipEvolutionService,
                           Clock clock) {
        this(tipRepository, userService, tipOptimizationService, tipBudgetService,
                tipGoalService, tipEvolutionService, null, null, null, clock);
    }

    public SmartTipService(TipRepository tipRepository,
                           UserService userService,
                           TipOptimizationService tipOptimizationService,
                           TipBudgetService tipBudgetService,
                           TipGoalService tipGoalService,
                           TipEvolutionService tipEvolutionService,
                           SmartTipAdaptationService smartTipAdaptationService,
                           SmartTipDecisionMemoryService smartTipDecisionMemoryService,
                           SmartTipPersonalizationService smartTipPersonalizationService,
                           Clock clock) {
        this.tipRepository = tipRepository;
        this.userService = userService;
        this.tipOptimizationService = tipOptimizationService;
        this.tipBudgetService = tipBudgetService;
        this.tipGoalService = tipGoalService;
        this.tipEvolutionService = tipEvolutionService;
        this.smartTipAdaptationService = smartTipAdaptationService;
        this.smartTipDecisionMemoryService = smartTipDecisionMemoryService;
        this.smartTipPersonalizationService = smartTipPersonalizationService;
        this.clock = clock;
    }

    /**
     * Generates context-aware smart tip suggestions for the authenticated user.
     *
     * @param email   the authenticated user's email
     * @param request the smart tip request context
     * @return advisory response containing context facts and up to 4 deterministic suggestions
     */
    @Transactional(readOnly = true)
    public SmartTipResponse getSmartTip(String email, SmartTipRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Smart tip request cannot be null");
        }
        if (request.billAmount() == null || request.billAmount().compareTo(MIN_VALID_BILL) <= 0) {
            throw new IllegalArgumentException("Bill amount must be greater than 0.01");
        }

        String currency = CurrencyValidationUtil.normalizeAndValidate(request.currency());
        User user = userService.getUserByEmail(email);

        BigDecimal billAmount = request.billAmount().setScale(2, RoundingMode.HALF_UP);

        // 1. Load user's tips strictly isolated to the requested currency
        List<Tip> allUserTips = tipRepository.findAllByUserId(user.getId());
        List<Tip> currencyTips = allUserTips.stream()
                .filter(t -> currency.equalsIgnoreCase(t.getCurrency()))
                .toList();

        int sampleSize = currencyTips.size();

        // 2. Historical Baseline
        BigDecimal historicalMedian = null;
        BigDecimal historicalAverage = null;
        if (sampleSize > 0) {
            List<BigDecimal> sortedPercentages = currencyTips.stream()
                    .map(Tip::getTipPercentage)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            historicalMedian = TipCalculationUtil.calculateMedian(sortedPercentages);
            historicalAverage = TipCalculationUtil.calculateMean(sortedPercentages);
        }

        // 3. Optimization Range (Day 18 reuse)
        BigDecimal optimizedMin = null;
        BigDecimal optimizedMax = null;
        if (sampleSize > 0 && historicalMedian != null) {
            try {
                TipOptimizationRequest optReq = new TipOptimizationRequest(currency, historicalMedian, null, billAmount);
                TipOptimizationResponse optRes = tipOptimizationService.optimize(email, optReq);
                optimizedMin = optRes.recommendedMinimumPercentage();
                optimizedMax = optRes.recommendedMaximumPercentage();
            } catch (Exception e) {
                log.warn("Failed to retrieve optimization for smart tip: {}", e.getMessage());
            }
        }

        // 4. Restaurant Context (Day 28 normalized matching)
        Integer restaurantTipCount = null;
        BigDecimal restaurantMedian = null;
        BigDecimal restaurantAverage = null;
        if (request.restaurantName() != null && !request.restaurantName().isBlank()) {
            String normReq = request.restaurantName().trim().toLowerCase();
            List<Tip> restTips = currencyTips.stream()
                    .filter(t -> t.getRestaurantName() != null && t.getRestaurantName().trim().toLowerCase().equals(normReq))
                    .toList();
            if (restTips.size() >= 2) {
                restaurantTipCount = restTips.size();
                List<BigDecimal> restPcts = restTips.stream()
                        .map(Tip::getTipPercentage)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();
                restaurantMedian = TipCalculationUtil.calculateMedian(restPcts);
                restaurantAverage = TipCalculationUtil.calculateMean(restPcts);
            }
        }

        // 5. Service Quality Context
        Integer serviceQualityTipCount = null;
        BigDecimal serviceQualityAverage = null;
        if (request.serviceQuality() != null) {
            List<Tip> sqTips = currencyTips.stream()
                    .filter(t -> t.getServiceQuality() == request.serviceQuality())
                    .toList();
            if (!sqTips.isEmpty()) {
                serviceQualityTipCount = sqTips.size();
                List<BigDecimal> sqPcts = sqTips.stream()
                        .map(Tip::getTipPercentage)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();
                serviceQualityAverage = TipCalculationUtil.calculateMean(sqPcts);
            }
        }

        // 6. Recent Direction (Day 26 / Day 30 reuse)
        TipEvolutionDirection recentDirection = TipEvolutionDirection.INSUFFICIENT_DATA;
        if (sampleSize >= 2) {
            try {
                TipEvolutionResponse evoRes = tipEvolutionService.getEvolution(email, currency, TipEvolutionPeriod.LAST_6_MONTHS);
                if (evoRes != null && evoRes.overallDirection() != null) {
                    recentDirection = evoRes.overallDirection();
                }
            } catch (Exception e) {
                log.debug("Failed to calculate direction for smart tip: {}", e.getMessage());
            }
        }

        // 7. Budget Context (Day 17 reuse)
        TipBudgetStatus budgetStatus = null;
        BigDecimal budgetUsagePct = null;
        BigDecimal monthlyLimit = null;
        BigDecimal currentSpent = null;
        try {
            TipBudgetStatusResponse budgetRes = tipBudgetService.getBudgetStatus(email, currency);
            if (budgetRes != null) {
                budgetStatus = budgetRes.status();
                budgetUsagePct = budgetRes.percentageUsed();
                monthlyLimit = budgetRes.monthlyLimit();
                currentSpent = budgetRes.currentMonthTips();
            }
        } catch (Exception e) {
            log.debug("No budget found for currency {}: {}", currency, e.getMessage());
        }

        // 8. Goal Context (Day 22 reuse)
        boolean goalRelevant = false;
        try {
            List<TipGoalProgressResponse> goals = tipGoalService.getGoals(email);
            if (goals != null && !goals.isEmpty()) {
                goalRelevant = goals.stream().anyMatch(g ->
                        g.status() == TipGoalStatus.ACTIVE &&
                        (g.currency() == null || currency.equalsIgnoreCase(g.currency())));
            }
        } catch (Exception e) {
            log.debug("Failed to check goals for smart tip: {}", e.getMessage());
        }

        // 9. Build Suggestions & Determine Primary
        List<SmartTipSuggestion> suggestions;
        SmartTipSuggestion primarySuggestion;
        String message;

        if (sampleSize == 0) {
            // No history state: neutral calculator suggestions
            List<SmartTipSuggestion> rawSuggestions = buildNoHistorySuggestions(billAmount, monthlyLimit, currentSpent, request.currentTipPercentage());
            SmartTipSuggestion candidate = rawSuggestions.stream()
                    .filter(s -> s.type() == SmartTipSuggestionType.MODERATE)
                    .findFirst()
                    .orElse(rawSuggestions.get(0));
            suggestions = rawSuggestions.stream().map(s -> {
                if (s.type() == candidate.type() && s.tipPercentage().compareTo(candidate.tipPercentage()) == 0) {
                    return new SmartTipSuggestion(
                            s.type(), s.tipPercentage(), s.tipAmount(), s.totalAmount(),
                            s.label(), s.reason(), s.historicalDifferencePercentagePoints(),
                            s.budgetImpact(), true
                    );
                }
                return s;
            }).toList();
            primarySuggestion = suggestions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.isRecommended()))
                    .findFirst()
                    .orElse(suggestions.get(0));
            message = "General tip options. Record tips to receive personalized suggestions.";
        } else {
            // Personalized suggestions
            suggestions = buildPersonalizedSuggestions(
                    billAmount, historicalMedian, optimizedMin, optimizedMax,
                    restaurantMedian, request.restaurantName(), sampleSize,
                    monthlyLimit, currentSpent, serviceQualityAverage, request.serviceQuality(),
                    request.currentTipPercentage()
            );

            primarySuggestion = selectPrimarySuggestion(
                    suggestions, restaurantMedian, optimizedMin, optimizedMax, historicalMedian,
                    budgetStatus, goalRelevant, request.serviceQuality()
            );

            // Mark recommended on the selected primary
            final SmartTipSuggestion finalPrimary = primarySuggestion;
            suggestions = suggestions.stream().map(s -> {
                if (s.type() == finalPrimary.type() && s.tipPercentage().compareTo(finalPrimary.tipPercentage()) == 0) {
                    return new SmartTipSuggestion(
                            s.type(), s.tipPercentage(), s.tipAmount(), s.totalAmount(),
                            s.label(), s.reason(), s.historicalDifferencePercentagePoints(),
                            s.budgetImpact(), true
                    );
                } else {
                    return s;
                }
            }).toList();

            primarySuggestion = suggestions.stream()
                    .filter(s -> Boolean.TRUE.equals(s.isRecommended()))
                    .findFirst()
                    .orElse(suggestions.get(0));

            message = sampleSize < 5
                    ? "Preliminary suggestions based on limited tipping history."
                    : "Personalized suggestions based on your tipping history.";
        }

        // Day 32: Bounded Behavioral Adaptation Layer
        // Day 36: Gate adaptation on personalization preference
        boolean personalizationEnabled = smartTipPersonalizationService == null
                || smartTipPersonalizationService.isPersonalizationEnabled(user, currency);

        SmartTipSuggestion baselinePrimary = primarySuggestion;
        SmartTipAdaptationResult adaptation = null;
        if (smartTipAdaptationService != null && personalizationEnabled) {
            try {
                adaptation = smartTipAdaptationService.getAdaptation(user, currency);
            } catch (Exception e) {
                log.debug("Smart tip adaptation analysis skipped: {}", e.getMessage());
            }
        }

        if (adaptation != null && adaptation.adaptationApplied() && adaptation.adaptationAdjustment() != null) {
            BigDecimal adjustment = adaptation.adaptationAdjustment();
            BigDecimal adaptedPct = baselinePrimary.tipPercentage().add(adjustment)
                    .max(BigDecimal.ZERO).min(HUNDRED).setScale(2, RoundingMode.HALF_UP);
            BigDecimal adaptedTip = billAmount.multiply(adaptedPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal adaptedTot = billAmount.add(adaptedTip);
            BigDecimal comparisonBase = request.currentTipPercentage() != null
                    ? request.currentTipPercentage().setScale(2, RoundingMode.HALF_UP)
                    : (historicalMedian != null ? historicalMedian.setScale(2, RoundingMode.HALF_UP) : adaptedPct);
            BigDecimal diff = adaptedPct.subtract(comparisonBase).setScale(2, RoundingMode.HALF_UP);
            String impact = calculateBudgetImpact(adaptedTip, monthlyLimit, currentSpent);

            SmartTipSuggestion adaptedSuggestion = new SmartTipSuggestion(
                    baselinePrimary.type(),
                    adaptedPct,
                    adaptedTip,
                    adaptedTot,
                    baselinePrimary.label() + " (Personalized)",
                    baselinePrimary.reason() + " " + adaptation.adaptationMessage(),
                    diff,
                    impact,
                    true
            );

            List<SmartTipSuggestion> updatedSuggestions = new ArrayList<>();
            for (SmartTipSuggestion s : suggestions) {
                if (s.type() == baselinePrimary.type() && s.tipPercentage().compareTo(baselinePrimary.tipPercentage()) == 0) {
                    updatedSuggestions.add(adaptedSuggestion);
                } else {
                    if (Boolean.TRUE.equals(s.isRecommended())) {
                        updatedSuggestions.add(new SmartTipSuggestion(
                                s.type(), s.tipPercentage(), s.tipAmount(), s.totalAmount(),
                                s.label(), s.reason(), s.historicalDifferencePercentagePoints(),
                                s.budgetImpact(), false
                        ));
                    } else {
                        updatedSuggestions.add(s);
                    }
                }
            }
            updatedSuggestions.sort(Comparator.comparing(SmartTipSuggestion::tipPercentage));
            suggestions = updatedSuggestions;
            primarySuggestion = adaptedSuggestion;
        }

        SmartTipResponse response = new SmartTipResponse(
                currency,
                billAmount,
                historicalMedian,
                historicalAverage,
                optimizedMin,
                optimizedMax,
                budgetStatus,
                budgetUsagePct,
                goalRelevant,
                recentDirection,
                restaurantTipCount,
                restaurantMedian,
                restaurantAverage,
                serviceQualityTipCount,
                serviceQualityAverage,
                suggestions,
                primarySuggestion,
                message,
                null,
                adaptation != null ? adaptation.feedbackCount() : 0L,
                adaptation != null ? adaptation.direction() : SmartTipFeedbackDirection.INSUFFICIENT_DATA,
                adaptation != null ? adaptation.averageDifferencePercentagePoints() : null,
                adaptation != null ? adaptation.adaptationApplied() : false,
                adaptation != null ? adaptation.adaptationAdjustment() : null,
                adaptation != null ? adaptation.adaptationMessage() : null,
                baselinePrimary,
                null,
                personalizationEnabled
        );

        if (smartTipDecisionMemoryService != null) {
            SmartTipDecisionExplanation explanation = smartTipDecisionMemoryService.buildDecisionExplanation(response, adaptation, personalizationEnabled);
            response = response.withDecisionExplanation(explanation);
        }

        return response;
    }

    private List<SmartTipSuggestion> buildNoHistorySuggestions(BigDecimal billAmount,
                                                               BigDecimal monthlyLimit,
                                                               BigDecimal currentSpent,
                                                               BigDecimal currentTipPct) {
        List<BigDecimal> defaultPcts = List.of(
                new BigDecimal("10.00"),
                new BigDecimal("15.00"),
                new BigDecimal("18.00"),
                new BigDecimal("20.00")
        );

        List<SmartTipSuggestion> list = new ArrayList<>();
        for (BigDecimal pct : defaultPcts) {
            BigDecimal tipAmt = billAmount.multiply(pct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal totAmt = billAmount.add(tipAmt);

            SmartTipSuggestionType type;
            String label;
            String reason;

            if (pct.compareTo(new BigDecimal("10.00")) == 0) {
                type = SmartTipSuggestionType.LOWER;
                label = "Lower";
                reason = "Standard baseline option (10%)";
            } else if (pct.compareTo(new BigDecimal("15.00")) == 0) {
                type = SmartTipSuggestionType.MODERATE;
                label = "Standard";
                reason = "Common standard tip (15%)";
            } else if (pct.compareTo(new BigDecimal("18.00")) == 0) {
                type = SmartTipSuggestionType.OPTIMIZED;
                label = "Recommended Standard";
                reason = "Standard dining service tip (18%)";
            } else {
                type = SmartTipSuggestionType.HIGHER;
                label = "Generous";
                reason = "Generous dining tip (20%)";
            }

            BigDecimal diff = currentTipPct != null ? pct.subtract(currentTipPct).setScale(2, RoundingMode.HALF_UP) : null;
            String budgetImpact = calculateBudgetImpact(tipAmt, monthlyLimit, currentSpent);

            list.add(new SmartTipSuggestion(
                    type, pct, tipAmt, totAmt, label, reason, diff, budgetImpact, false
            ));
        }

        return list;
    }

    private List<SmartTipSuggestion> buildPersonalizedSuggestions(
            BigDecimal billAmount,
            BigDecimal median,
            BigDecimal optMin,
            BigDecimal optMax,
            BigDecimal restMedian,
            String restName,
            int sampleSize,
            BigDecimal monthlyLimit,
            BigDecimal currentSpent,
            BigDecimal sqAverage,
            ServiceQuality sq,
            BigDecimal currentTipPct) {

        List<SmartTipSuggestion> list = new ArrayList<>();
        Set<BigDecimal> usedPercentages = new HashSet<>();

        // 1. Historical Typical (User's historical median)
        BigDecimal typicalPct = median.setScale(2, RoundingMode.HALF_UP);
        BigDecimal comparisonBase = currentTipPct != null ? currentTipPct.setScale(2, RoundingMode.HALF_UP) : typicalPct;

        BigDecimal typicalTip = billAmount.multiply(typicalPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal typicalTot = billAmount.add(typicalTip);
        String typicalReason;
        if (restMedian != null && restMedian.compareTo(typicalPct) == 0 && restName != null) {
            typicalReason = String.format("Matches your typical for %s (%s%%)", restName.trim(), typicalPct.toPlainString());
        } else if (sampleSize < 5) {
            typicalReason = String.format("Matches your median based on %d recorded tip%s", sampleSize, sampleSize == 1 ? "" : "s");
        } else {
            typicalReason = String.format("Matches your overall historical median (%s%%)", typicalPct.toPlainString());
        }
        String typicalImpact = calculateBudgetImpact(typicalTip, monthlyLimit, currentSpent);
        BigDecimal typicalDiff = typicalPct.subtract(comparisonBase).setScale(2, RoundingMode.HALF_UP);

        list.add(new SmartTipSuggestion(
                SmartTipSuggestionType.HISTORICAL_TYPICAL,
                typicalPct, typicalTip, typicalTot,
                "Typical", typicalReason,
                typicalDiff,
                typicalImpact, false
        ));
        usedPercentages.add(typicalPct);

        // 2. Optimized option / Restaurant Typical
        BigDecimal optPct;
        String optLabel;
        String optReason;

        if (restMedian != null && !usedPercentages.contains(restMedian.setScale(2, RoundingMode.HALF_UP))) {
            optPct = restMedian.setScale(2, RoundingMode.HALF_UP);
            optLabel = "Restaurant Typical";
            optReason = String.format("Typical for %s based on your visits (%s%%)",
                    restName != null ? restName.trim() : "this restaurant", optPct.toPlainString());
        } else {
            optLabel = "Optimized";
            BigDecimal candidateOpt = optMax != null ? optMax.setScale(2, RoundingMode.HALF_UP) : typicalPct.add(new BigDecimal("2.00"));
            if (candidateOpt.compareTo(typicalPct) == 0 || usedPercentages.contains(candidateOpt)) {
                candidateOpt = typicalPct.add(new BigDecimal("1.50")).min(new BigDecimal("100.00"));
            }
            optPct = candidateOpt.setScale(2, RoundingMode.HALF_UP);
            optReason = String.format("Balanced option within your optimized range (%s%% - %s%%)",
                    optMin != null ? optMin.toPlainString() : typicalPct.subtract(new BigDecimal("2")).toPlainString(),
                    optMax != null ? optMax.toPlainString() : typicalPct.add(new BigDecimal("2")).toPlainString());
        }

        if (!usedPercentages.contains(optPct)) {
            BigDecimal optTip = billAmount.multiply(optPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal optTot = billAmount.add(optTip);
            BigDecimal diff = optPct.subtract(comparisonBase).setScale(2, RoundingMode.HALF_UP);
            String impact = calculateBudgetImpact(optTip, monthlyLimit, currentSpent);

            list.add(new SmartTipSuggestion(
                    SmartTipSuggestionType.OPTIMIZED,
                    optPct, optTip, optTot,
                    optLabel, optReason, diff, impact, false
            ));
            usedPercentages.add(optPct);
        }

        // 3. Lower option (e.g. optMin or median - 2.5%, clamped to >= 0)
        BigDecimal lowerPct = optMin != null ? optMin.setScale(2, RoundingMode.HALF_UP) : typicalPct.subtract(new BigDecimal("2.00")).max(BigDecimal.ZERO);
        if (usedPercentages.contains(lowerPct)) {
            lowerPct = typicalPct.subtract(new BigDecimal("3.00")).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        if (!usedPercentages.contains(lowerPct)) {
            BigDecimal lowerTip = billAmount.multiply(lowerPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal lowerTot = billAmount.add(lowerTip);
            BigDecimal diff = lowerPct.subtract(comparisonBase).setScale(2, RoundingMode.HALF_UP);
            String impact = calculateBudgetImpact(lowerTip, monthlyLimit, currentSpent);
            String lowerReason = String.format("A conservative option below your typical (%s%%)", lowerPct.toPlainString());

            list.add(new SmartTipSuggestion(
                    SmartTipSuggestionType.LOWER,
                    lowerPct, lowerTip, lowerTot,
                    "Lower", lowerReason, diff, impact, false
            ));
            usedPercentages.add(lowerPct);
        }

        // 4. Higher option (e.g. median + 3.0% or 4.0%, clamped to <= 100)
        BigDecimal higherPct = typicalPct.add(new BigDecimal("4.00")).min(new BigDecimal("100.00")).setScale(2, RoundingMode.HALF_UP);
        if (usedPercentages.contains(higherPct)) {
            higherPct = typicalPct.add(new BigDecimal("5.00")).min(new BigDecimal("100.00")).setScale(2, RoundingMode.HALF_UP);
        }
        if (!usedPercentages.contains(higherPct)) {
            BigDecimal higherTip = billAmount.multiply(higherPct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal higherTot = billAmount.add(higherTip);
            BigDecimal diff = higherPct.subtract(comparisonBase).setScale(2, RoundingMode.HALF_UP);
            String impact = calculateBudgetImpact(higherTip, monthlyLimit, currentSpent);
            String higherReason = (sq != null && sqAverage != null && sqAverage.compareTo(typicalPct) > 0)
                    ? String.format("More generous for %s service (your rating avg: %s%%)", sq.name().toLowerCase(), sqAverage.toPlainString())
                    : String.format("A more generous option above your typical (%s%%)", higherPct.toPlainString());

            list.add(new SmartTipSuggestion(
                    SmartTipSuggestionType.HIGHER,
                    higherPct, higherTip, higherTot,
                    "Higher", higherReason, diff, impact, false
            ));
            usedPercentages.add(higherPct);
        }

        // Sort suggestions deterministically by percentage ascending
        list.sort(Comparator.comparing(SmartTipSuggestion::tipPercentage));
        return list;
    }

    private SmartTipSuggestion selectPrimarySuggestion(List<SmartTipSuggestion> suggestions,
                                                      BigDecimal restMedian,
                                                      BigDecimal optMin,
                                                      BigDecimal optMax,
                                                      BigDecimal histMedian,
                                                      TipBudgetStatus budgetStatus,
                                                      boolean goalRelevant,
                                                      ServiceQuality sq) {
        // 1. Budget warning or limit reached: prioritize LOWER
        if (budgetStatus == TipBudgetStatus.OVER_BUDGET
                || budgetStatus == TipBudgetStatus.LIMIT_REACHED
                || budgetStatus == TipBudgetStatus.APPROACHING_LIMIT) {
            for (SmartTipSuggestion s : suggestions) {
                if (s.type() == SmartTipSuggestionType.LOWER) {
                    return s;
                }
            }
        }

        // 2. Service Quality POOR: prioritize LOWER
        if (sq == ServiceQuality.POOR) {
            for (SmartTipSuggestion s : suggestions) {
                if (s.type() == SmartTipSuggestionType.LOWER) {
                    return s;
                }
            }
        }

        // 3. Restaurant-specific history exists: prioritize restaurant typical
        if (restMedian != null) {
            for (SmartTipSuggestion s : suggestions) {
                if (s.tipPercentage().compareTo(restMedian.setScale(2, RoundingMode.HALF_UP)) == 0) {
                    return s;
                }
            }
        }

        // 4. Historical median when available
        if (histMedian != null) {
            for (SmartTipSuggestion s : suggestions) {
                if (s.type() == SmartTipSuggestionType.HISTORICAL_TYPICAL) {
                    return s;
                }
            }
        }

        // 5. Otherwise: use OPTIMIZED or first suggestion
        for (SmartTipSuggestion s : suggestions) {
            if (s.type() == SmartTipSuggestionType.OPTIMIZED) {
                return s;
            }
        }

        return suggestions.get(0);
    }

    private String calculateBudgetImpact(BigDecimal tipAmount, BigDecimal monthlyLimit, BigDecimal currentSpent) {
        if (monthlyLimit == null || monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal impactPct = tipAmount.multiply(HUNDRED).divide(monthlyLimit, 1, RoundingMode.HALF_UP);
        BigDecimal projectedTotal = (currentSpent != null ? currentSpent : BigDecimal.ZERO).add(tipAmount);

        if (projectedTotal.compareTo(monthlyLimit) > 0) {
            return String.format("+%s%% (Exceeds monthly budget limit)", impactPct.toPlainString());
        }

        return String.format("+%s%% of monthly budget", impactPct.toPlainString());
    }
}
