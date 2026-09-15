package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TipProfileService {

    private final TipRepository tipRepository;
    private final UserService userService;
    private final GenerosityScoreService generosityScoreService;

    public TipProfileService(TipRepository tipRepository, UserService userService, GenerosityScoreService generosityScoreService) {
        this.tipRepository = tipRepository;
        this.userService = userService;
        this.generosityScoreService = generosityScoreService;
    }

    @Transactional(readOnly = true)
    public TipProfileResponse getProfile(String email, String currencyFilter) {
        User user = userService.getUserByEmail(email);
        List<Tip> allTips = tipRepository.findAllByUserId(user.getId());

        if (allTips.isEmpty()) {
            return buildEmptyProfile();
        }

        List<Tip> filteredTips = allTips;
        if (currencyFilter != null && !currencyFilter.isBlank()) {
            String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currencyFilter);
            filteredTips = allTips.stream()
                    .filter(t -> t.getCurrency().equals(normalizedCurrency))
                    .collect(Collectors.toList());
            if (filteredTips.isEmpty()) {
                return buildEmptyProfile(); // Valid case: user has tips, but none in this currency
            }
        }

        // Currency Profiles
        Map<String, List<Tip>> tipsByCurrency = filteredTips.stream()
                .collect(Collectors.groupingBy(Tip::getCurrency));

        List<TipCurrencyProfile> currencyProfiles = new ArrayList<>();
        for (Map.Entry<String, List<Tip>> entry : tipsByCurrency.entrySet()) {
            currencyProfiles.add(buildCurrencyProfile(entry.getKey(), entry.getValue()));
        }

        // Sort currency profiles by count desc, then alphabetically
        currencyProfiles.sort(Comparator.comparing(TipCurrencyProfile::tipCount).reversed()
                .thenComparing(TipCurrencyProfile::currency));

        // Overall stats
        List<BigDecimal> allPercentages = filteredTips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .collect(Collectors.toList());

        BigDecimal median = com.aitip.util.TipCalculationUtil.calculateMedian(allPercentages);
        BigDecimal mean = com.aitip.util.TipCalculationUtil.calculateMean(allPercentages);
        BigDecimal min = allPercentages.get(0);
        BigDecimal max = allPercentages.get(allPercentages.size() - 1);

        Integer consistencyScore = null;
        TipBehaviorType behaviorType = null;
        if (filteredTips.size() >= 5) {
            consistencyScore = com.aitip.util.TipCalculationUtil.calculateConsistencyScore(allPercentages, mean);
            behaviorType = com.aitip.util.TipCalculationUtil.determineBehaviorType(consistencyScore);
        }

        int score = generosityScoreService.calculateScore(median);
        TipStyle style = TipStyle.valueOf(generosityScoreService.determineCategory(score));

        // Recent Trend (only if >= 10 tips)
        String recentTrend = null;
        BigDecimal recentMedian = null;
        if (filteredTips.size() >= 10) {
            // Sort by createdAt desc
            List<Tip> sortedByDate = filteredTips.stream()
                    .sorted(Comparator.comparing(Tip::getCreatedAt).thenComparing(Tip::getId).reversed())
                    .collect(Collectors.toList());

            List<BigDecimal> recentPercentages = sortedByDate.stream()
                    .limit(5)
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());

            List<BigDecimal> historicalPercentages = sortedByDate.stream()
                    .skip(5)
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());

            recentMedian = com.aitip.util.TipCalculationUtil.calculateMedian(recentPercentages);
            BigDecimal historicalMedian = com.aitip.util.TipCalculationUtil.calculateMedian(historicalPercentages);

            BigDecimal diff = recentMedian.subtract(historicalMedian);
            if (diff.compareTo(new BigDecimal("3")) >= 0) {
                recentTrend = "MORE_GENEROUS";
            } else if (diff.compareTo(new BigDecimal("-3")) <= 0) {
                recentTrend = "MORE_CONSERVATIVE";
            } else {
                recentTrend = "STABLE";
            }
        }

        // Restaurants
        List<RestaurantPattern> topRestaurants = buildRestaurantPatterns(filteredTips);
        
        // Service Quality
        List<ServiceQualityPattern> topServiceQualities = buildServiceQualityPatterns(filteredTips);

        String message = "Your personalized tipping profile has been generated.";
        if (filteredTips.size() < 5) {
            message = "Add a few more tips to unlock consistency and behavior insights.";
        } else if (filteredTips.size() < 10) {
            message = "More history is needed to identify a meaningful recent trend.";
        }

        return new TipProfileResponse(
                LocalDateTime.now(),
                filteredTips.size(),
                currencyProfiles,
                behaviorType,
                style,
                median,
                mean,
                min,
                max,
                consistencyScore,
                recentMedian,
                recentTrend,
                topRestaurants,
                topServiceQualities,
                message,
                null
        );
    }

    private TipCurrencyProfile buildCurrencyProfile(String currency, List<Tip> tips) {
        List<BigDecimal> percentages = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .collect(Collectors.toList());

        List<BigDecimal> amounts = tips.stream()
                .map(Tip::getTipAmount)
                .sorted()
                .collect(Collectors.toList());

        BigDecimal medianPct = com.aitip.util.TipCalculationUtil.calculateMedian(percentages);
        BigDecimal meanPct = com.aitip.util.TipCalculationUtil.calculateMean(percentages);
        BigDecimal minPct = percentages.get(0);
        BigDecimal maxPct = percentages.get(percentages.size() - 1);
        BigDecimal medianAmt = com.aitip.util.TipCalculationUtil.calculateMedian(amounts);
        BigDecimal meanAmt = com.aitip.util.TipCalculationUtil.calculateMean(amounts);

        TipBehaviorType behavior = null;
        if (tips.size() >= 5) {
            Integer score = com.aitip.util.TipCalculationUtil.calculateConsistencyScore(percentages, meanPct);
            behavior = com.aitip.util.TipCalculationUtil.determineBehaviorType(score);
        }

        int genScore = generosityScoreService.calculateScore(medianPct);
        TipStyle style = TipStyle.valueOf(generosityScoreService.determineCategory(genScore));

        return new TipCurrencyProfile(
                currency,
                tips.size(),
                medianPct,
                meanPct,
                meanAmt,
                medianAmt,
                minPct,
                maxPct,
                behavior,
                style
        );
    }

    private List<RestaurantPattern> buildRestaurantPatterns(List<Tip> tips) {
        Map<String, List<Tip>> byRestaurant = new HashMap<>();
        Map<String, String> originalNames = new HashMap<>();

        for (Tip tip : tips) {
            if (tip.getRestaurantName() != null && !tip.getRestaurantName().isBlank()) {
                String normalized = tip.getRestaurantName().trim().toLowerCase();
                byRestaurant.computeIfAbsent(normalized, k -> new ArrayList<>()).add(tip);
                // Keep the most recently encountered casing
                originalNames.putIfAbsent(normalized, tip.getRestaurantName().trim());
            }
        }

        List<RestaurantPattern> patterns = new ArrayList<>();
        for (Map.Entry<String, List<Tip>> entry : byRestaurant.entrySet()) {
            List<Tip> restaurantTips = entry.getValue();
            List<BigDecimal> pcts = restaurantTips.stream()
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());
            
            patterns.add(new RestaurantPattern(
                    originalNames.get(entry.getKey()),
                    restaurantTips.size(),
                    com.aitip.util.TipCalculationUtil.calculateMean(pcts),
                    com.aitip.util.TipCalculationUtil.calculateMedian(pcts)
            ));
        }

        // Sort by count desc, then name asc
        return patterns.stream()
                .sorted(Comparator.comparing(RestaurantPattern::tipCount).reversed()
                        .thenComparing(RestaurantPattern::restaurantName))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<ServiceQualityPattern> buildServiceQualityPatterns(List<Tip> tips) {
        Map<ServiceQuality, List<Tip>> bySq = tips.stream()
                .filter(t -> t.getServiceQuality() != null)
                .collect(Collectors.groupingBy(Tip::getServiceQuality));

        List<ServiceQualityPattern> patterns = new ArrayList<>();
        for (Map.Entry<ServiceQuality, List<Tip>> entry : bySq.entrySet()) {
            List<BigDecimal> pcts = entry.getValue().stream()
                    .map(Tip::getTipPercentage)
                    .sorted()
                    .collect(Collectors.toList());
            patterns.add(new ServiceQualityPattern(
                    entry.getKey(),
                    entry.getValue().size(),
                    com.aitip.util.TipCalculationUtil.calculateMean(pcts)
            ));
        }
        
        // Sort by count desc
        return patterns.stream()
                .sorted(Comparator.comparing(ServiceQualityPattern::count).reversed())
                .collect(Collectors.toList());
    }



    private TipProfileResponse buildEmptyProfile() {
        return new TipProfileResponse(
                LocalDateTime.now(),
                0,
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                "Add a few tips to start building your personalized tipping profile.",
                null
        );
    }
}
