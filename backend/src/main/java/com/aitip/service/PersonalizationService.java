package com.aitip.service;

import com.aitip.dto.PersonalizationContext;
import com.aitip.dto.PersonalizationResponse;
import com.aitip.dto.RestaurantInsight;
import com.aitip.dto.ServiceQuality;
import com.aitip.dto.PersonalizationSource;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Calculates personalized tipping patterns from the user's actual history.
 *
 * <p><b>Design principles:</b></p>
 * <ul>
 *   <li>All statistics are deterministic and backend-calculated</li>
 *   <li>Median is used for "typical" claims (robust to outliers)</li>
 *   <li>Mean is reported separately for transparency</li>
 *   <li>This service is completely independent of Groq</li>
 * </ul>
 *
 * <p><b>Threshold constants:</b></p>
 * <ul>
 *   <li>{@code MIN_TIPS_FOR_AVERAGE = 2}: Minimum tips before reporting an average</li>
 *   <li>{@code MIN_TIPS_FOR_STRONG_CLAIM = 5}: Minimum tips before saying "you typically tip..."</li>
 *   <li>{@code MIN_TIPS_FOR_RESTAURANT_INSIGHT = 2}: Minimum visits before restaurant-specific claims</li>
 * </ul>
 */
@Service
public class PersonalizationService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationService.class);

    static final int MIN_TIPS_FOR_AVERAGE = 2;
    static final int MIN_TIPS_FOR_STRONG_CLAIM = 5;
    static final int MIN_TIPS_FOR_RESTAURANT_INSIGHT = 2;

    private final TipRepository tipRepository;
    private final UserRepository userRepository;

    public PersonalizationService(TipRepository tipRepository, UserRepository userRepository) {
        this.tipRepository = tipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the authenticated user's overall tipping summary.
     */
    @Transactional(readOnly = true)
    public PersonalizationResponse getSummary(String email) {
        UUID userId = resolveUserId(email);
        List<Tip> tips = tipRepository.findAllByUserId(userId);
        return buildResponse(tips, null);
    }

    /**
     * Returns restaurant-specific personalization for the authenticated user.
     */
    @Transactional(readOnly = true)
    public PersonalizationResponse getRestaurantPersonalization(String email, String restaurantName) {
        UUID userId = resolveUserId(email);

        List<Tip> allTips = tipRepository.findAllByUserId(userId);
        List<Tip> restaurantTips = tipRepository.findByUserIdAndRestaurantNameIgnoreCase(userId, restaurantName);

        RestaurantInsight insight = buildRestaurantInsight(restaurantName, restaurantTips);
        return buildResponse(allTips, insight);
    }

    /**
     * Builds a PersonalizationContext for the AI layer (Day 7).
     * Returns null if there is no personalization data at all.
     */
    @Transactional(readOnly = true)
    public PersonalizationContext buildContextForAi(String email, String restaurantName, ServiceQuality serviceQuality) {
        UUID userId = resolveUserId(email);
        List<Tip> allTips = tipRepository.findAllByUserId(userId);

        if (allTips.isEmpty()) {
            return null;
        }

        // Overall
        BigDecimal overallMedian = calculateMedian(allTips.stream().map(Tip::getTipPercentage).sorted().toList());
        BigDecimal overallMean = calculateMean(allTips.stream().map(Tip::getTipPercentage).toList());

        // Restaurant
        Integer restaurantVisitCount = null;
        BigDecimal restaurantMedian = null;
        BigDecimal restaurantMean = null;
        BigDecimal lastTipAtRestaurant = null;
        java.time.LocalDateTime lastVisitDate = null;
        ServiceQuality lastServiceQualityAtRestaurant = null;
        List<Tip> restaurantTips = List.of();

        if (restaurantName != null && !restaurantName.isBlank()) {
            restaurantTips = tipRepository.findByUserIdAndRestaurantNameIgnoreCase(userId, restaurantName);
            if (!restaurantTips.isEmpty()) {
                restaurantVisitCount = restaurantTips.size();
                restaurantMedian = calculateMedian(restaurantTips.stream().map(Tip::getTipPercentage).sorted().toList());
                restaurantMean = calculateMean(restaurantTips.stream().map(Tip::getTipPercentage).toList());
                Tip latest = restaurantTips.stream()
                        .max(Comparator.comparing(Tip::getCreatedAt))
                        .orElse(null);
                if (latest != null) {
                    lastTipAtRestaurant = latest.getTipPercentage();
                    lastVisitDate = latest.getCreatedAt();
                    lastServiceQualityAtRestaurant = latest.getServiceQuality();
                }
            }
        }

        // Service Quality
        Integer serviceQualityCount = null;
        BigDecimal serviceQualityMedian = null;
        BigDecimal serviceQualityMean = null;
        List<Tip> serviceQualityTips = List.of();

        if (serviceQuality != null) {
            serviceQualityTips = tipRepository.findByUserIdAndServiceQuality(userId, serviceQuality);
            if (!serviceQualityTips.isEmpty()) {
                serviceQualityCount = serviceQualityTips.size();
                serviceQualityMedian = calculateMedian(serviceQualityTips.stream().map(Tip::getTipPercentage).sorted().toList());
                serviceQualityMean = calculateMean(serviceQualityTips.stream().map(Tip::getTipPercentage).toList());
            }
        }

        // Restaurant + Service Quality
        Integer restAndSqCount = null;
        BigDecimal restAndSqMedian = null;
        BigDecimal restAndSqMean = null;
        List<Tip> restAndSqTips = List.of();

        if (restaurantName != null && !restaurantName.isBlank() && serviceQuality != null) {
            restAndSqTips = tipRepository.findByUserIdAndRestaurantNameIgnoreCaseAndServiceQuality(userId, restaurantName, serviceQuality);
            if (!restAndSqTips.isEmpty()) {
                restAndSqCount = restAndSqTips.size();
                restAndSqMedian = calculateMedian(restAndSqTips.stream().map(Tip::getTipPercentage).sorted().toList());
                restAndSqMean = calculateMean(restAndSqTips.stream().map(Tip::getTipPercentage).toList());
            }
        }

        // Determine strongest eligible context (minimum 2 tips)
        PersonalizationSource source = PersonalizationSource.NONE;
        int countForConfidence = 0;

        if (restAndSqTips.size() >= 2) {
            source = PersonalizationSource.RESTAURANT_AND_SERVICE;
            countForConfidence = restAndSqTips.size();
        } else if (restaurantTips.size() >= 2) {
            source = PersonalizationSource.RESTAURANT;
            countForConfidence = restaurantTips.size();
        } else if (serviceQualityTips.size() >= 2) {
            source = PersonalizationSource.SERVICE_QUALITY;
            countForConfidence = serviceQualityTips.size();
        } else if (allTips.size() >= 2) {
            source = PersonalizationSource.OVERALL;
            countForConfidence = allTips.size();
        }

        // Calculate confidence based on the chosen source's count
        String confidence = "LOW";
        if (countForConfidence >= 5) {
            confidence = "HIGH";
        } else if (countForConfidence >= 2) {
            confidence = "MEDIUM";
        }

        return new PersonalizationContext(
                source,
                confidence,
                allTips.size(),
                overallMedian,
                overallMean,
                restaurantName,
                restaurantVisitCount,
                restaurantMedian,
                restaurantMean,
                lastTipAtRestaurant,
                lastVisitDate,
                lastServiceQualityAtRestaurant,
                serviceQuality,
                serviceQualityCount,
                serviceQualityMedian,
                serviceQualityMean,
                restAndSqCount,
                restAndSqMedian,
                restAndSqMean
        );
    }

    // =============================================
    // Private helpers
    // =============================================

    private PersonalizationResponse buildResponse(List<Tip> tips, RestaurantInsight restaurantInsight) {
        if (tips.isEmpty()) {
            return new PersonalizationResponse(
                    0, null, null, null, null, null, null,
                    "No tipping history yet.",
                    restaurantInsight
            );
        }

        List<BigDecimal> percentages = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();

        List<BigDecimal> amounts = tips.stream()
                .map(Tip::getTipAmount)
                .toList();

        BigDecimal mean = calculateMean(percentages);
        BigDecimal median = calculateMedian(percentages);
        BigDecimal min = percentages.getFirst();
        BigDecimal max = percentages.getLast();
        BigDecimal avgAmount = calculateMean(amounts);

        // Personalized percentage = median, rounded to 1 decimal
        BigDecimal personalized = median.setScale(1, RoundingMode.HALF_UP);

        String message = buildMessage(tips.size(), personalized, min, max);

        return new PersonalizationResponse(
                tips.size(),
                mean.setScale(2, RoundingMode.HALF_UP),
                median.setScale(2, RoundingMode.HALF_UP),
                min,
                max,
                avgAmount.setScale(2, RoundingMode.HALF_UP),
                personalized,
                message,
                restaurantInsight
        );
    }

    private RestaurantInsight buildRestaurantInsight(String restaurantName, List<Tip> tips) {
        if (tips.isEmpty()) {
            return new RestaurantInsight(restaurantName, 0, null, null, null, null, null);
        }

        List<BigDecimal> percentages = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();

        BigDecimal avg = calculateMean(percentages);
        BigDecimal median = calculateMedian(percentages);

        Tip latest = tips.stream()
                .max(Comparator.comparing(Tip::getCreatedAt))
                .orElse(null);

        return new RestaurantInsight(
                restaurantName,
                tips.size(),
                avg.setScale(2, RoundingMode.HALF_UP),
                median.setScale(2, RoundingMode.HALF_UP),
                latest != null ? latest.getTipPercentage() : null,
                latest != null ? latest.getTipAmount() : null,
                latest != null ? latest.getCreatedAt() : null
        );
    }

    /**
     * Builds a human-readable message based on tip count thresholds.
     */
    private String buildMessage(int count, BigDecimal personalized, BigDecimal min, BigDecimal max) {
        if (count == 1) {
            return "You have 1 recorded tip.";
        } else if (count < MIN_TIPS_FOR_STRONG_CLAIM) {
            return String.format("Your recent median tip is %s%% (range: %s%%â€“%s%%).", personalized, min, max);
        } else {
            return String.format("You typically tip around %s%% (range: %s%%â€“%s%%, based on %d tips).", personalized, min, max, count);
        }
    }

    /**
     * Calculates the arithmetic mean of a list of BigDecimals.
     */
    BigDecimal calculateMean(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the median of a SORTED list of BigDecimals.
     *
     * <p><b>Why median over mean?</b>
     * Median is robust to outliers. A single 100% tip won't distort
     * the "typical" claim the way it would with a mean.</p>
     */
    BigDecimal calculateMedian(List<BigDecimal> sortedValues) {
        if (sortedValues.isEmpty()) return BigDecimal.ZERO;
        int size = sortedValues.size();
        if (size % 2 == 1) {
            return sortedValues.get(size / 2);
        } else {
            BigDecimal a = sortedValues.get(size / 2 - 1);
            BigDecimal b = sortedValues.get(size / 2);
            return a.add(b).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }
    }

    private UUID resolveUserId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
