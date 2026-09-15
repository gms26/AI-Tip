package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.util.CurrencyValidationUtil;
import com.aitip.util.TipCalculationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TipEvolutionService {

    private final UserRepository userRepository;
    private final TipRepository tipRepository;
    private final GenerosityScoreService generosityScoreService;
    private final Clock clock;

    @Autowired
    public TipEvolutionService(UserRepository userRepository,
                               TipRepository tipRepository,
                               GenerosityScoreService generosityScoreService) {
        this(userRepository, tipRepository, generosityScoreService, Clock.systemDefaultZone());
    }

    public TipEvolutionService(UserRepository userRepository,
                               TipRepository tipRepository,
                               GenerosityScoreService generosityScoreService,
                               Clock clock) {
        this.userRepository = userRepository;
        this.tipRepository = tipRepository;
        this.generosityScoreService = generosityScoreService;
        this.clock = clock;
    }

    public TipEvolutionResponse getEvolution(String email, String currencyFilter, TipEvolutionPeriod period) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (period == null) {
            period = TipEvolutionPeriod.LAST_6_MONTHS;
        }

        String normalizedCurrency = null;
        if (currencyFilter != null && !currencyFilter.isBlank()) {
            normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currencyFilter);
        }

        List<Tip> tips = loadTips(user.getId(), normalizedCurrency, period);

        if (tips.isEmpty()) {
            return new TipEvolutionResponse(
                    LocalDateTime.now(clock),
                    period,
                    normalizedCurrency,
                    0,
                    0,
                    TipEvolutionDirection.INSUFFICIENT_DATA,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    "Add some tips to start seeing how your tipping behavior changes over time.",
                    null
            );
        }

        // Active months chronologically sorted across all tips
        List<String> distinctMonths = tips.stream()
                .map(t -> YearMonth.from(t.getCreatedAt()).toString())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int activeMonthsCount = distinctMonths.size();

        // Overall root direction & current/historical percentage stats
        BigDecimal currentMedian = null;
        BigDecimal currentAverage = null;
        BigDecimal historicalMedian = null;
        BigDecimal historicalAverage = null;
        TipEvolutionDirection overallDirection;

        if (activeMonthsCount < 2) {
            overallDirection = TipEvolutionDirection.INSUFFICIENT_DATA;
            List<BigDecimal> allPercentages = tips.stream()
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());
            currentMedian = TipCalculationUtil.calculateMedian(allPercentages);
            currentAverage = TipCalculationUtil.calculateMean(allPercentages);
        } else {
            int olderCount = activeMonthsCount / 2;
            Set<String> olderMonthSet = new HashSet<>(distinctMonths.subList(0, olderCount));
            Set<String> recentMonthSet = new HashSet<>(distinctMonths.subList(olderCount, activeMonthsCount));

            List<BigDecimal> olderPercentages = tips.stream()
                    .filter(t -> olderMonthSet.contains(YearMonth.from(t.getCreatedAt()).toString()))
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());

            List<BigDecimal> recentPercentages = tips.stream()
                    .filter(t -> recentMonthSet.contains(YearMonth.from(t.getCreatedAt()).toString()))
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());

            historicalMedian = TipCalculationUtil.calculateMedian(olderPercentages);
            historicalAverage = TipCalculationUtil.calculateMean(olderPercentages);
            currentMedian = TipCalculationUtil.calculateMedian(recentPercentages);
            currentAverage = TipCalculationUtil.calculateMean(recentPercentages);

            BigDecimal diff = currentMedian.subtract(historicalMedian);
            if (diff.compareTo(new BigDecimal("3")) >= 0) {
                overallDirection = TipEvolutionDirection.MORE_GENEROUS;
            } else if (diff.compareTo(new BigDecimal("-3")) <= 0) {
                overallDirection = TipEvolutionDirection.MORE_CONSERVATIVE;
            } else {
                overallDirection = TipEvolutionDirection.STABLE;
            }
        }

        // Currency timelines
        Map<String, List<Tip>> byCurrency = tips.stream()
                .collect(Collectors.groupingBy(t -> t.getCurrency().toUpperCase()));

        List<TipCurrencyEvolution> currencyTimelines = new ArrayList<>();
        List<String> sortedCurrencies = byCurrency.keySet().stream().sorted().collect(Collectors.toList());

        for (String curr : sortedCurrencies) {
            List<Tip> currTips = byCurrency.get(curr);
            TipCurrencyEvolution currEvolution = calculateCurrencyEvolution(curr, currTips);
            currencyTimelines.add(currEvolution);
        }

        // Service quality evolution
        List<TipServiceQualityEvolution> serviceQualityEvolution = calculateServiceQualityEvolution(tips);

        String message = activeMonthsCount < 2
                ? "Keep tipping across multiple months to unlock your behavioral evolution."
                : "Your tipping evolution has been calculated.";

        return new TipEvolutionResponse(
                LocalDateTime.now(clock),
                period,
                normalizedCurrency,
                tips.size(),
                activeMonthsCount,
                overallDirection,
                currentMedian,
                currentAverage,
                historicalMedian,
                historicalAverage,
                currencyTimelines,
                serviceQualityEvolution,
                message,
                null
        );
    }

    private List<Tip> loadTips(UUID userId, String currency, TipEvolutionPeriod period) {
        if (period == TipEvolutionPeriod.ALL_TIME) {
            List<Tip> all = tipRepository.findAllByUserId(userId);
            if (currency != null) {
                return all.stream()
                        .filter(t -> t.getCurrency().equalsIgnoreCase(currency))
                        .collect(Collectors.toList());
            }
            return all;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime end = YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime start = YearMonth.from(now).minusMonths(period.getMonths() - 1).atDay(1).atStartOfDay();

        if (currency != null) {
            return tipRepository.findByUserIdAndCurrencyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    userId, currency, start, end);
        } else {
            return tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    userId, start, end);
        }
    }

    private TipCurrencyEvolution calculateCurrencyEvolution(String currency, List<Tip> currTips) {
        Map<String, List<Tip>> byMonth = currTips.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getCreatedAt()).toString()));

        List<String> sortedMonths = byMonth.keySet().stream().sorted().collect(Collectors.toList());
        List<TipEvolutionMonth> monthlyTimeline = new ArrayList<>();

        for (String m : sortedMonths) {
            monthlyTimeline.add(calculateMonthSnapshot(m, byMonth.get(m)));
        }

        int monthsWithActivity = monthlyTimeline.size();
        BigDecimal currMedian = null;
        BigDecimal currAverage = null;
        BigDecimal histMedian = null;
        BigDecimal histAverage = null;
        TipEvolutionDirection direction;

        if (monthsWithActivity < 2) {
            direction = TipEvolutionDirection.INSUFFICIENT_DATA;
            if (monthsWithActivity == 1) {
                currMedian = monthlyTimeline.get(0).medianTipPercentage();
                currAverage = monthlyTimeline.get(0).averageTipPercentage();
            }
        } else {
            int olderCount = monthsWithActivity / 2;
            Set<String> olderMonthSet = new HashSet<>(sortedMonths.subList(0, olderCount));
            Set<String> recentMonthSet = new HashSet<>(sortedMonths.subList(olderCount, monthsWithActivity));

            List<BigDecimal> olderPcts = currTips.stream()
                    .filter(t -> olderMonthSet.contains(YearMonth.from(t.getCreatedAt()).toString()))
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());

            List<BigDecimal> recentPcts = currTips.stream()
                    .filter(t -> recentMonthSet.contains(YearMonth.from(t.getCreatedAt()).toString()))
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());

            histMedian = TipCalculationUtil.calculateMedian(olderPcts);
            histAverage = TipCalculationUtil.calculateMean(olderPcts);
            currMedian = TipCalculationUtil.calculateMedian(recentPcts);
            currAverage = TipCalculationUtil.calculateMean(recentPcts);

            BigDecimal diff = currMedian.subtract(histMedian);
            if (diff.compareTo(new BigDecimal("3")) >= 0) {
                direction = TipEvolutionDirection.MORE_GENEROUS;
            } else if (diff.compareTo(new BigDecimal("-3")) <= 0) {
                direction = TipEvolutionDirection.MORE_CONSERVATIVE;
            } else {
                direction = TipEvolutionDirection.STABLE;
            }
        }

        return new TipCurrencyEvolution(
                currency,
                currTips.size(),
                monthsWithActivity,
                currMedian,
                currAverage,
                histMedian,
                histAverage,
                monthlyTimeline,
                direction
        );
    }

    public TipEvolutionMonth calculateMonthSnapshot(String month, List<Tip> tips) {
        if (tips == null || tips.isEmpty()) {
            return new TipEvolutionMonth(
                    month, 0, null, null, null, null, null, null, null, null, null, 0
            );
        }

        List<BigDecimal> percentages = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .collect(Collectors.toList());

        List<BigDecimal> amounts = tips.stream()
                .map(Tip::getTipAmount)
                .sorted()
                .collect(Collectors.toList());

        BigDecimal medianPct = TipCalculationUtil.calculateMedian(percentages);
        BigDecimal meanPct = TipCalculationUtil.calculateMean(percentages);
        BigDecimal minPct = percentages.get(0);
        BigDecimal maxPct = percentages.get(percentages.size() - 1);

        BigDecimal medianAmt = TipCalculationUtil.calculateMedian(amounts);
        BigDecimal meanAmt = TipCalculationUtil.calculateMean(amounts);

        Integer consistencyScore = null;
        TipBehaviorType behaviorType = null;
        if (tips.size() >= 5) {
            consistencyScore = TipCalculationUtil.calculateConsistencyScore(percentages, meanPct);
            behaviorType = TipCalculationUtil.determineBehaviorType(consistencyScore);
        }

        int score = generosityScoreService.calculateScore(medianPct);
        String category = generosityScoreService.determineCategory(score);
        TipStyle tipStyle = TipStyle.valueOf(category);

        int uniqueRestaurantCount = (int) tips.stream()
                .map(Tip::getRestaurantName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toLowerCase())
                .distinct()
                .count();

        return new TipEvolutionMonth(
                month,
                tips.size(),
                medianPct,
                meanPct,
                meanAmt,
                medianAmt,
                minPct,
                maxPct,
                consistencyScore,
                tipStyle,
                behaviorType,
                uniqueRestaurantCount
        );
    }

    private List<TipServiceQualityEvolution> calculateServiceQualityEvolution(List<Tip> tips) {
        List<TipServiceQualityEvolution> list = new ArrayList<>();

        for (ServiceQuality sq : ServiceQuality.values()) {
            List<Tip> sqTips = tips.stream()
                    .filter(t -> t.getServiceQuality() == sq)
                    .collect(Collectors.toList());

            if (!sqTips.isEmpty()) {
                List<BigDecimal> pcts = sqTips.stream()
                        .map(Tip::getTipPercentage)
                        .sorted()
                        .collect(Collectors.toList());
                BigDecimal overallAvg = TipCalculationUtil.calculateMean(pcts);

                Map<String, List<Tip>> byMonth = sqTips.stream()
                        .collect(Collectors.groupingBy(t -> YearMonth.from(t.getCreatedAt()).toString()));

                Map<String, BigDecimal> monthlyValues = new TreeMap<>();
                byMonth.keySet().stream().sorted().forEach(m -> {
                    List<BigDecimal> mPcts = byMonth.get(m).stream()
                            .map(Tip::getTipPercentage)
                            .sorted()
                            .collect(Collectors.toList());
                    monthlyValues.put(m, TipCalculationUtil.calculateMean(mPcts));
                });

                list.add(new TipServiceQualityEvolution(sq, monthlyValues, overallAvg));
            }
        }

        return list;
    }
}
