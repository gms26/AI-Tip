package com.aitip.service;

import com.aitip.dto.AnalyticsPeriod;
import com.aitip.dto.AnalyticsRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.dto.analytics.*;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic analytics service for tip history.
 *
 * <p><b>Design principles:</b></p>
 * <ul>
 *   <li>All statistics are calculated by this service, never delegated to AI.</li>
 *   <li>Monetary values are never mixed across currencies.</li>
 *   <li>Tip percentages (dimensionless) may be aggregated across currencies.</li>
 *   <li>Every query is scoped to the authenticated user.</li>
 *   <li>Date filtering uses [startDate, endDate) semantics.</li>
 *   <li>Restaurant and service-quality filters are applied in memory after date filtering.</li>
 *   <li>Monthly trend is calculated after all filters are applied.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TipAnalyticsService {

    private final TipRepository tipRepository;
    private final UserService userService;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public TipAnalyticsResponse getAnalytics(String email, AnalyticsRequest request) {
        User user = userService.getUserByEmail(email);

        // 1. Validate request
        validateRequest(request);

        // 2. Resolve date range
        LocalDate resolvedStart;
        LocalDate resolvedEnd;
        LocalDate now = LocalDate.now();

        switch (request.period()) {
            case CURRENT_MONTH -> {
                resolvedStart = now.withDayOfMonth(1);
                resolvedEnd = YearMonth.from(now).atEndOfMonth().plusDays(1);
            }
            case PREVIOUS_MONTH -> {
                LocalDate prevMonth = now.minusMonths(1);
                resolvedStart = prevMonth.withDayOfMonth(1);
                resolvedEnd = YearMonth.from(prevMonth).atEndOfMonth().plusDays(1);
            }
            case CURRENT_YEAR -> {
                resolvedStart = now.withDayOfYear(1);
                resolvedEnd = LocalDate.of(now.getYear(), 12, 31).plusDays(1);
            }
            case LAST_30_DAYS -> {
                resolvedStart = now.minusDays(30);
                resolvedEnd = now.plusDays(1);
            }
            case LAST_90_DAYS -> {
                resolvedStart = now.minusDays(90);
                resolvedEnd = now.plusDays(1);
            }
            case CUSTOM -> {
                resolvedStart = request.startDate();
                resolvedEnd = request.endDate().plusDays(1); // exclusive end
            }
            case ALL_TIME -> {
                resolvedStart = null;
                resolvedEnd = null;
            }
            default -> throw new IllegalArgumentException("Invalid analytics period");
        }

        // 3. Fetch tips from database
        List<Tip> tips;
        if (request.period() == AnalyticsPeriod.ALL_TIME) {
            tips = tipRepository.findAllByUserId(user.getId());
        } else {
            LocalDateTime startDateTime = resolvedStart.atStartOfDay();
            LocalDateTime endDateTime = resolvedEnd.atStartOfDay();
            tips = tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    user.getId(), startDateTime, endDateTime);
        }

        // 4. Apply in-memory filters
        if (request.restaurantName() != null && !request.restaurantName().isBlank()) {
            tips = tips.stream()
                    .filter(t -> t.getRestaurantName() != null &&
                            t.getRestaurantName().equalsIgnoreCase(request.restaurantName()))
                    .collect(Collectors.toList());
        }

        if (request.serviceQuality() != null) {
            tips = tips.stream()
                    .filter(t -> request.serviceQuality().equals(t.getServiceQuality()))
                    .collect(Collectors.toList());
        }

        // 5. Handle empty state
        if (tips.isEmpty()) {
            return new TipAnalyticsResponse(
                    request.period(),
                    resolvedStart,
                    resolvedEnd != null ? resolvedEnd.minusDays(1) : null,
                    0,
                    null, null, null, null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        // 6. Calculate percentage statistics (cross-currency safe)
        BigDecimal avgTipPct = calculateAveragePercentage(tips);
        BigDecimal medianTipPct = calculateMedianPercentage(tips);
        BigDecimal highestTipPct = tips.stream().map(Tip::getTipPercentage).max(Comparator.naturalOrder()).orElse(null);
        BigDecimal lowestTipPct = tips.stream().map(Tip::getTipPercentage).min(Comparator.naturalOrder()).orElse(null);

        // 7. Currency breakdown (monetary values per currency)
        List<CurrencyAnalytics> currencyBreakdown = buildCurrencyBreakdown(tips);

        // 8. Restaurant insights
        List<RestaurantAnalytics> restaurantInsights = buildRestaurantInsights(tips);

        // 9. Service quality insights
        List<ServiceQualityAnalytics> serviceQualityInsights = buildServiceQualityInsights(tips);

        // 10. Monthly trend (calculated AFTER filters)
        List<MonthlyTipTrend> monthlyTrend = buildMonthlyTrend(tips);

        return new TipAnalyticsResponse(
                request.period(),
                resolvedStart,
                resolvedEnd != null ? resolvedEnd.minusDays(1) : null,
                tips.size(),
                avgTipPct,
                medianTipPct,
                highestTipPct,
                lowestTipPct,
                restaurantInsights,
                serviceQualityInsights,
                currencyBreakdown,
                monthlyTrend
        );
    }

    // =============================================
    // Validation
    // =============================================

    private void validateRequest(AnalyticsRequest request) {
        if (request.period() == null) {
            throw new IllegalArgumentException("Analytics period is required");
        }
        if (request.period() == AnalyticsPeriod.CUSTOM) {
            if (request.startDate() == null || request.endDate() == null) {
                throw new IllegalArgumentException("Start date and end date are required for CUSTOM period");
            }
            if (request.startDate().isAfter(request.endDate())) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        }
    }

    // =============================================
    // Percentage calculations (cross-currency safe)
    // =============================================

    private BigDecimal calculateAveragePercentage(List<Tip> tips) {
        BigDecimal sum = tips.stream()
                .map(Tip::getTipPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(tips.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMedianPercentage(List<Tip> tips) {
        List<BigDecimal> sorted = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();
        return computeMedian(sorted);
    }

    // =============================================
    // Currency breakdown
    // =============================================

    private List<CurrencyAnalytics> buildCurrencyBreakdown(List<Tip> tips) {
        Map<String, List<Tip>> byCurrency = tips.stream()
                .collect(Collectors.groupingBy(Tip::getCurrency));

        List<CurrencyAnalytics> result = new ArrayList<>();
        for (Map.Entry<String, List<Tip>> entry : byCurrency.entrySet()) {
            String currency = entry.getKey();
            List<Tip> currencyTips = entry.getValue();
            int count = currencyTips.size();

            BigDecimal totalAmount = currencyTips.stream()
                    .map(Tip::getTipAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avgAmount = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

            List<BigDecimal> sortedAmounts = currencyTips.stream()
                    .map(Tip::getTipAmount)
                    .sorted()
                    .toList();
            BigDecimal medianAmount = computeMedian(sortedAmounts);

            result.add(new CurrencyAnalytics(currency, count, totalAmount, avgAmount, medianAmount));
        }

        result.sort(Comparator.comparing(CurrencyAnalytics::currency));
        return result;
    }

    // =============================================
    // Restaurant insights
    // =============================================

    private List<RestaurantAnalytics> buildRestaurantInsights(List<Tip> tips) {
        Map<String, List<Tip>> byRestaurant = tips.stream()
                .filter(t -> t.getRestaurantName() != null && !t.getRestaurantName().isBlank())
                .collect(Collectors.groupingBy(t -> t.getRestaurantName().toLowerCase(),
                        LinkedHashMap::new, Collectors.toList()));

        List<RestaurantAnalytics> result = new ArrayList<>();
        for (Map.Entry<String, List<Tip>> entry : byRestaurant.entrySet()) {
            List<Tip> restaurantTips = entry.getValue();
            // Use the original restaurant name from the first tip for display
            String displayName = restaurantTips.get(0).getRestaurantName();
            int visitCount = restaurantTips.size();

            BigDecimal avgPct = calculateAveragePercentage(restaurantTips);
            BigDecimal medianPct = calculateMedianPercentage(restaurantTips);

            // Average tip amount â€” only safe for single currency
            BigDecimal avgAmount = restaurantTips.stream()
                    .map(Tip::getTipAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(visitCount), 2, RoundingMode.HALF_UP);

            // Last visit info
            Tip lastTip = restaurantTips.stream()
                    .max(Comparator.comparing(Tip::getCreatedAt))
                    .orElse(null);

            LocalDateTime lastVisit = lastTip != null ? lastTip.getCreatedAt() : null;
            BigDecimal lastTipPct = lastTip != null ? lastTip.getTipPercentage() : null;

            // Most common service quality
            ServiceQuality mostCommon = restaurantTips.stream()
                    .map(Tip::getServiceQuality)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(sq -> sq, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            result.add(new RestaurantAnalytics(
                    displayName, visitCount, avgPct, medianPct, avgAmount,
                    lastVisit, lastTipPct, mostCommon
            ));
        }

        // Sort by visit count descending
        result.sort(Comparator.comparingInt(RestaurantAnalytics::visitCount).reversed());
        return result;
    }

    // =============================================
    // Service quality insights
    // =============================================

    private List<ServiceQualityAnalytics> buildServiceQualityInsights(List<Tip> tips) {
        List<ServiceQualityAnalytics> result = new ArrayList<>();

        for (ServiceQuality sq : ServiceQuality.values()) {
            List<Tip> sqTips = tips.stream()
                    .filter(t -> sq.equals(t.getServiceQuality()))
                    .toList();

            if (sqTips.isEmpty()) continue;

            BigDecimal avgPct = calculateAveragePercentage(sqTips);
            BigDecimal medianPct = calculateMedianPercentage(sqTips);

            BigDecimal avgAmount = sqTips.stream()
                    .map(Tip::getTipAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(sqTips.size()), 2, RoundingMode.HALF_UP);

            result.add(new ServiceQualityAnalytics(sq, sqTips.size(), avgPct, medianPct, avgAmount));
        }

        return result;
    }

    // =============================================
    // Monthly trend (calculated after all filters)
    // =============================================

    private List<MonthlyTipTrend> buildMonthlyTrend(List<Tip> tips) {
        Map<String, List<Tip>> byMonth = tips.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().format(MONTH_FORMATTER),
                        TreeMap::new, Collectors.toList()
                ));

        List<MonthlyTipTrend> result = new ArrayList<>();
        for (Map.Entry<String, List<Tip>> entry : byMonth.entrySet()) {
            String month = entry.getKey();
            List<Tip> monthTips = entry.getValue();
            BigDecimal avgPct = calculateAveragePercentage(monthTips);
            result.add(new MonthlyTipTrend(month, monthTips.size(), avgPct));
        }

        return result;
    }

    // =============================================
    // Median utility
    // =============================================

    /**
     * Computes the median of a sorted list of BigDecimal values.
     *
     * <p><b>Rules:</b></p>
     * <ul>
     *   <li>Odd count: middle value</li>
     *   <li>Even count: (lower middle + upper middle) / 2, rounded HALF_UP scale 2</li>
     * </ul>
     */
    private BigDecimal computeMedian(List<BigDecimal> sorted) {
        int size = sorted.size();
        if (size == 0) return BigDecimal.ZERO;
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        } else {
            BigDecimal mid1 = sorted.get(size / 2 - 1);
            BigDecimal mid2 = sorted.get(size / 2);
            return mid1.add(mid2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }
}
