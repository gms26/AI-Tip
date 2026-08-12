package com.aitip.service;

import com.aitip.dto.GenerosityScoreResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating a user's Generosity Score based on historical tipping.
 * This is purely deterministic and derived directly from Tip entities.
 */
@Service
@RequiredArgsConstructor
public class GenerosityScoreService {

    private final UserRepository userRepository;
    private final TipRepository tipRepository;

    /**
     * Calculates the Generosity Score for a given user.
     *
     * @param email The authenticated user's email
     * @return A response containing score, category, stats, and confidence
     */
    public GenerosityScoreResponse getScore(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Tip> allTips = tipRepository.findAllByUserId(user.getId());

        int totalTips = allTips.size();
        
        if (totalTips == 0) {
            return new GenerosityScoreResponse(
                    null,
                    null,
                    null,
                    null,
                    0,
                    0,
                    "LOW",
                    "Not enough tipping history to calculate a score."
            );
        }

        // We use all tips for the score (since we only look at tipPercentage, not serviceQuality for this phase)
        List<BigDecimal> tipPercentages = allTips.stream()
                .map(Tip::getTipPercentage)
                .sorted() // BigDecimal implements Comparable natively
                .collect(Collectors.toList());

        BigDecimal median = calculateMedian(tipPercentages);
        BigDecimal mean = calculateMean(tipPercentages);
        
        int score = calculateScore(median);
        String category = determineCategory(score);
        String confidence = determineConfidence(totalTips);
        
        String message = buildMessage(score, category, confidence, totalTips);

        return new GenerosityScoreResponse(
                score,
                category,
                median,
                mean,
                totalTips,
                totalTips,
                confidence,
                message
        );
    }

    private BigDecimal calculateMedian(List<BigDecimal> sortedPercentages) {
        int size = sortedPercentages.size();
        if (size == 1) {
            return sortedPercentages.get(0);
        }

        if (size % 2 == 1) {
            return sortedPercentages.get(size / 2);
        } else {
            BigDecimal mid1 = sortedPercentages.get((size / 2) - 1);
            BigDecimal mid2 = sortedPercentages.get(size / 2);
            return mid1.add(mid2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calculateMean(List<BigDecimal> percentages) {
        BigDecimal sum = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(percentages.size()), 2, RoundingMode.HALF_UP);
    }

    private int calculateScore(BigDecimal medianTipPercentage) {
        if (medianTipPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (medianTipPercentage.compareTo(new BigDecimal("25")) >= 0) {
            return 100;
        }

        // Linear interpolation: score = medianTipPercentage * 4
        BigDecimal rawScore = medianTipPercentage.multiply(new BigDecimal("4"));
        
        // Round to nearest integer and clamp
        int intScore = rawScore.setScale(0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(100, intScore));
    }

    private String determineCategory(int score) {
        if (score <= 39) {
            return "CONSERVATIVE";
        } else if (score <= 59) {
            return "MODERATE";
        } else if (score <= 79) {
            return "GENEROUS";
        } else {
            return "VERY_GENEROUS";
        }
    }

    private String determineConfidence(int totalTips) {
        if (totalTips <= 1) {
            return "LOW";
        } else if (totalTips <= 4) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
    
    private String buildMessage(int score, String category, String confidence, int tips) {
        if (tips == 1) {
            return "Your tipping pattern is " + category.toLowerCase().replace("_", " ") + " based on a single tip.";
        }
        return "Your tipping pattern is generally " + category.toLowerCase().replace("_", " ") + ".";
    }
}
