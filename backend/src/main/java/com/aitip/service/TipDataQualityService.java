package com.aitip.service;

import com.aitip.dto.TipAnomalyRecord;
import com.aitip.dto.TipAnomalySeverity;
import com.aitip.dto.TipAnomalyType;
import com.aitip.dto.TipDataQualityResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipDataQualityService {

    private final TipRepository tipRepository;

    @Transactional(readOnly = true)
    public TipDataQualityResponse analyzeDataQuality(User user, String currencyFilter, TipAnomalySeverity severityFilter, TipAnomalyType typeFilter) {
        List<Tip> allTips = tipRepository.findAllByUserId(user.getId());
        List<TipAnomalyRecord> anomalies = new ArrayList<>();
        Set<String> currencies = new HashSet<>();

        // Group tips by currency for statistical analysis and outlier detection
        Map<String, List<Tip>> tipsByCurrency = allTips.stream()
                .filter(t -> t.getCurrency() != null && !t.getCurrency().isBlank())
                .collect(Collectors.groupingBy(Tip::getCurrency));

        // Group by normalized restaurant to detect duplicates
        Map<String, List<Tip>> tipsByNormalizedSignature = allTips.stream()
                .collect(Collectors.groupingBy(this::getDuplicateSignature));

        for (Tip tip : allTips) {
            if (tip.getCurrency() != null && !tip.getCurrency().isBlank()) {
                currencies.add(tip.getCurrency());
            }

            // Rule D - Missing data
            if (tip.getRestaurantName() == null || tip.getRestaurantName().isBlank()) {
                anomalies.add(createAnomaly(tip, TipAnomalyType.MISSING_RESTAURANT, TipAnomalySeverity.INFO, "Missing restaurant name."));
            }
            if (tip.getCurrency() == null || tip.getCurrency().isBlank() || tip.getCurrency().length() != 3) {
                anomalies.add(createAnomaly(tip, TipAnomalyType.INVALID_CURRENCY, TipAnomalySeverity.HIGH, "Currency is missing or invalid."));
            }

            // Rule A - Extreme percentage (from the stored field if available)
            if (tip.getTipPercentage() != null) {
                BigDecimal pct = tip.getTipPercentage();
                if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
                    anomalies.add(createAnomaly(tip, TipAnomalyType.UNUSUAL_TIP_PERCENTAGE, TipAnomalySeverity.HIGH, "Tip percentage is < 0 or > 100."));
                } else if (pct.compareTo(new BigDecimal("50")) >= 0) {
                    anomalies.add(createAnomaly(tip, TipAnomalyType.UNUSUAL_TIP_PERCENTAGE, TipAnomalySeverity.WARNING, "Tip percentage is exceptionally high (>= 50%)."));
                }
            }

            // Rule B - Extreme tip amount (calculate derived percentage)
            if (tip.getTipAmount() != null && tip.getBillAmount() != null && tip.getBillAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal derivedPct = tip.getTipAmount().divide(tip.getBillAmount(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                if (derivedPct.compareTo(new BigDecimal("100")) > 0) {
                    anomalies.add(createAnomaly(tip, TipAnomalyType.EXTREME_TIP_AMOUNT, TipAnomalySeverity.HIGH, "Tip amount exceeds the bill amount."));
                }
            } else if (tip.getBillAmount() == null || tip.getTipAmount() == null) {
                anomalies.add(createAnomaly(tip, TipAnomalyType.UNUSUAL_BILL_AMOUNT, TipAnomalySeverity.HIGH, "Missing financial amounts."));
            }

            // Rule C - Duplicate-like records
            String signature = getDuplicateSignature(tip);
            List<Tip> similarTips = tipsByNormalizedSignature.getOrDefault(signature, Collections.emptyList());
            for (Tip other : similarTips) {
                if (!other.getId().equals(tip.getId())) {
                    long minutesDiff = Math.abs(Duration.between(tip.getCreatedAt(), other.getCreatedAt()).toMinutes());
                    if (minutesDiff <= 2) {
                        anomalies.add(createAnomaly(tip, TipAnomalyType.DUPLICATE_LIKE_RECORD, TipAnomalySeverity.WARNING, "Appears to be a duplicate of another tip within 2 minutes."));
                        break; // Only log one duplicate anomaly per tip
                    }
                }
            }
        }

        // Rule C continued (Statistical Outlier)
        for (Map.Entry<String, List<Tip>> entry : tipsByCurrency.entrySet()) {
            List<Tip> validTips = entry.getValue().stream()
                    .filter(t -> t.getTipPercentage() != null)
                    .toList();

            if (validTips.size() >= 5) {
                List<BigDecimal> pcts = validTips.stream()
                        .map(Tip::getTipPercentage)
                        .sorted()
                        .toList();

                BigDecimal median;
                int size = pcts.size();
                if (size % 2 == 1) {
                    median = pcts.get(size / 2);
                } else {
                    median = pcts.get(size / 2 - 1).add(pcts.get(size / 2)).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                }

                for (Tip tip : validTips) {
                    BigDecimal pct = tip.getTipPercentage();
                    if (pct.subtract(median).abs().compareTo(new BigDecimal("20")) >= 0) {
                        // Check if we already have an anomaly for this to avoid spamming
                        boolean alreadyFlagged = anomalies.stream()
                                .anyMatch(a -> a.tipId().equals(tip.getId()) && a.anomalyType() == TipAnomalyType.UNUSUAL_TIP_PERCENTAGE);
                        if (!alreadyFlagged) {
                            anomalies.add(createAnomaly(tip, TipAnomalyType.UNUSUAL_TIP_PERCENTAGE, TipAnomalySeverity.WARNING, "Statistical outlier: differs from median by >= 20 percentage points."));
                        }
                    }
                }
            }
        }

        // Apply Optional Filters
        List<TipAnomalyRecord> filteredAnomalies = anomalies.stream()
                .filter(a -> currencyFilter == null || (a.currency() != null && a.currency().equalsIgnoreCase(currencyFilter)))
                .filter(a -> severityFilter == null || a.severity() == severityFilter)
                .filter(a -> typeFilter == null || a.anomalyType() == typeFilter)
                // Sort anomalies by tip creation date descending
                .sorted((a1, a2) -> {
                    if (a1.createdAt() != null && a2.createdAt() != null) {
                        return a2.createdAt().compareTo(a1.createdAt());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        // Summarize
        Set<UUID> anomalousTipIds = filteredAnomalies.stream()
                .map(TipAnomalyRecord::tipId)
                .collect(Collectors.toSet());

        long totalTips = allTips.size();
        long cleanTips = totalTips - anomalousTipIds.size();
        long anomalyCount = filteredAnomalies.size();
        long highSeverityCount = filteredAnomalies.stream().filter(a -> a.severity() == TipAnomalySeverity.HIGH).count();
        long warningCount = filteredAnomalies.stream().filter(a -> a.severity() == TipAnomalySeverity.WARNING).count();
        long infoCount = filteredAnomalies.stream().filter(a -> a.severity() == TipAnomalySeverity.INFO).count();

        String message = String.format("%d / %d records look consistent.", cleanTips, totalTips);

        return new TipDataQualityResponse(
                totalTips,
                cleanTips,
                anomalyCount,
                highSeverityCount,
                warningCount,
                infoCount,
                filteredAnomalies,
                currencies,
                message
        );
    }

    private TipAnomalyRecord createAnomaly(Tip tip, TipAnomalyType type, TipAnomalySeverity severity, String reason) {
        return new TipAnomalyRecord(
                tip.getId(),
                tip.getRestaurantName(),
                tip.getBillAmount(),
                tip.getTipAmount(),
                tip.getTipPercentage(),
                tip.getCurrency(),
                tip.getCreatedAt(),
                type,
                severity,
                reason
        );
    }

    private String getDuplicateSignature(Tip tip) {
        String restaurant = tip.getRestaurantName() != null ? tip.getRestaurantName().trim().toLowerCase().replaceAll("\\s+", " ") : "";
        String currency = tip.getCurrency() != null ? tip.getCurrency().toUpperCase() : "";
        String bill = tip.getBillAmount() != null ? tip.getBillAmount().setScale(2, RoundingMode.HALF_UP).toString() : "";
        String tipAmt = tip.getTipAmount() != null ? tip.getTipAmount().setScale(2, RoundingMode.HALF_UP).toString() : "";
        return String.join("|", restaurant, currency, bill, tipAmt);
    }
}
