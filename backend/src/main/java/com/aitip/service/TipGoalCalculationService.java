package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.TipGoalProgressResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.TipGoal;
import com.aitip.entity.TipGoalPeriod;
import com.aitip.entity.TipGoalStatus;
import com.aitip.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipGoalCalculationService {

    private final TipRepository tipRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public TipGoalProgressResponse calculateProgress(TipGoal goal) {
        // Resolve date boundaries
        LocalDateTime start = null;
        LocalDateTime end = null;
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        switch (goal.getPeriod()) {
            case CURRENT_MONTH -> {
                start = today.withDayOfMonth(1).atStartOfDay();
                end = today.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay().minusNanos(1);
            }
            case CURRENT_YEAR -> {
                start = today.withDayOfYear(1).atStartOfDay();
                end = today.with(TemporalAdjusters.lastDayOfYear()).plusDays(1).atStartOfDay().minusNanos(1);
            }
            case CUSTOM -> {
                start = goal.getStartDate().atStartOfDay();
                end = goal.getEndDate().plusDays(1).atStartOfDay().minusNanos(1);
            }
            case ALL_TIME -> {
                // null bounds
            }
        }

        // Fetch user's tip history (could be optimized with Specification, but keeping simple for stream processing as requested)
        List<Tip> allTips = tipRepository.findAllByUserId(goal.getUser().getId());

        // Filter by Date
        LocalDateTime finalStart = start;
        LocalDateTime finalEnd = end;
        List<Tip> filteredTips = allTips.stream()
                .filter(t -> finalStart == null || !t.getCreatedAt().isBefore(finalStart))
                .filter(t -> finalEnd == null || !t.getCreatedAt().isAfter(finalEnd))
                .collect(Collectors.toList());

        // Filter by Currency
        if (goal.getCurrency() != null && !goal.getCurrency().isBlank()) {
            filteredTips = filteredTips.stream()
                    .filter(t -> goal.getCurrency().equals(t.getCurrency()))
                    .collect(Collectors.toList());
        }

        // Filter by Service Quality
        if (goal.getServiceQuality() != null) {
            filteredTips = filteredTips.stream()
                    .filter(t -> goal.getServiceQuality() == t.getServiceQuality())
                    .collect(Collectors.toList());
        }

        // Filter by Restaurant (Normalized)
        if (goal.getRestaurantName() != null && !goal.getRestaurantName().isBlank()) {
            String targetNormalized = normalizeRestaurant(goal.getRestaurantName());
            filteredTips = filteredTips.stream()
                    .filter(t -> t.getRestaurantName() != null)
                    .filter(t -> targetNormalized.equals(normalizeRestaurant(t.getRestaurantName())))
                    .collect(Collectors.toList());
        }

        BigDecimal currentValue = BigDecimal.ZERO;
        int sampleSize = filteredTips.size();

        switch (goal.getGoalType()) {
            case TOTAL_TIP_AMOUNT -> {
                currentValue = filteredTips.stream()
                        .map(Tip::getTipAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case TIP_COUNT, SERVICE_QUALITY -> {
                currentValue = new BigDecimal(sampleSize);
            }
            case AVERAGE_TIP_PERCENTAGE -> {
                if (sampleSize > 0) {
                    BigDecimal sum = filteredTips.stream()
                            .map(Tip::getTipPercentage)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    currentValue = sum.divide(new BigDecimal(sampleSize), 2, RoundingMode.HALF_UP);
                }
            }
            case MEDIAN_TIP_PERCENTAGE -> {
                if (sampleSize > 0) {
                    List<BigDecimal> sortedPercentages = filteredTips.stream()
                            .map(Tip::getTipPercentage)
                            .sorted()
                            .toList();
                    if (sampleSize % 2 == 1) {
                        currentValue = sortedPercentages.get(sampleSize / 2);
                    } else {
                        BigDecimal middle1 = sortedPercentages.get(sampleSize / 2 - 1);
                        BigDecimal middle2 = sortedPercentages.get(sampleSize / 2);
                        currentValue = middle1.add(middle2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                    }
                }
            }
            case RESTAURANT_EXPLORATION -> {
                long uniqueRestaurants = filteredTips.stream()
                        .map(Tip::getRestaurantName)
                        .filter(name -> name != null && !name.isBlank())
                        .map(this::normalizeRestaurant)
                        .distinct()
                        .count();
                currentValue = new BigDecimal(uniqueRestaurants);
            }
        }

        BigDecimal targetValue = goal.getTargetValue();
        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (targetValue.compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = currentValue.divide(targetValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        // Clamp to 0-100
        if (progressPercentage.compareTo(BigDecimal.ZERO) < 0) {
            progressPercentage = BigDecimal.ZERO;
        } else if (progressPercentage.compareTo(new BigDecimal("100.00")) > 0) {
            progressPercentage = new BigDecimal("100.00");
        }

        BigDecimal remainingValue = targetValue.subtract(currentValue);
        if (remainingValue.compareTo(BigDecimal.ZERO) < 0) {
            remainingValue = BigDecimal.ZERO;
        }

        String confidence = "LOW";
        if (sampleSize >= 5) {
            confidence = "HIGH";
        } else if (sampleSize >= 2) {
            confidence = "MEDIUM";
        }

        // Determine effective status (did we cross the threshold?)
        TipGoalStatus effectiveStatus = goal.getStatus();
        if (effectiveStatus == TipGoalStatus.ACTIVE) {
            if (currentValue.compareTo(targetValue) >= 0) {
                effectiveStatus = TipGoalStatus.COMPLETED;
            } else if (end != null && now.isAfter(end)) {
                effectiveStatus = TipGoalStatus.EXPIRED;
            }
        }

        String message = "Goal is " + effectiveStatus;
        if (effectiveStatus == TipGoalStatus.COMPLETED) {
            message = "Congratulations! You have completed this goal.";
        }

        return new TipGoalProgressResponse(
                goal.getId(),
                goal.getGoalType(),
                goal.getTargetValue(),
                currentValue,
                progressPercentage,
                remainingValue,
                effectiveStatus,
                sampleSize,
                confidence,
                message,
                goal.getCurrency(),
                goal.getPeriod(),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.getRestaurantName(),
                goal.getServiceQuality()
        );
    }

    private String normalizeRestaurant(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
