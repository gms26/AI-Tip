package com.aitip.service;

import com.aitip.dto.ReceiptMatchResponse;
import com.aitip.dto.ReceiptReconciliationRequest;
import com.aitip.dto.ReceiptReconciliationResponse;
import com.aitip.dto.ReceiptReconciliationStatus;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.util.CurrencyValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptReconciliationService {

    private final TipRepository tipRepository;
    private final UserService userService;

    public ReceiptReconciliationResponse reconcile(String email, ReceiptReconciliationRequest request) {
        User user = userService.getUserByEmail(email);
        
        // 1. Currency Normalization and Isolation
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(request.currency());

        log.debug("Reconciling receipt for user {} and currency {}", user.getId(), normalizedCurrency);

        // Fetch all tips for user (memory filtering for currency to preserve Day 1-18 strict repository scope)
        List<Tip> allTips = tipRepository.findAllByUserId(user.getId());
        
        List<ReceiptMatchResponse> scoredMatches = new ArrayList<>();

        for (Tip tip : allTips) {
            // Must exactly match currency to even be considered a candidate
            if (!normalizedCurrency.equals(tip.getCurrency())) {
                continue;
            }

            int rawScore = 0;
            List<String> reasons = new ArrayList<>();

            // Currency match (prerequisite)
            rawScore += 40;
            reasons.add("Currency matches");

            // Amount match
            BigDecimal diff = request.billAmount().subtract(tip.getBillAmount()).abs();
            if (diff.compareTo(new BigDecimal("0.01")) <= 0) {
                rawScore += 40;
                reasons.add(String.format("Bill amount matches within %s0.01", getCurrencySymbol(normalizedCurrency)));
            }

            // Restaurant match
            if (normalizeRestaurant(request.restaurantName()).equals(normalizeRestaurant(tip.getRestaurantName()))) {
                rawScore += 20;
                reasons.add("Restaurant name matches");
            }

            // Date match logic
            boolean hasReceiptDate = request.receiptDate() != null;
            if (hasReceiptDate) {
                if (request.receiptDate().equals(tip.getCreatedAt().toLocalDate())) {
                    rawScore += 10;
                    reasons.add("Receipt date matches");
                }
            }

            // Dynamic Normalization
            int maxPossibleScore = hasReceiptDate ? 110 : 100;
            int normalizedScore = (rawScore * 100) / maxPossibleScore;
            
            // Clamp 0-100
            int finalScore = Math.min(100, Math.max(0, normalizedScore));

            // Only return meaningful matches (e.g. at least possible match)
            if (finalScore >= 50) {
                scoredMatches.add(new ReceiptMatchResponse(
                        tip.getId(),
                        tip.getRestaurantName(),
                        tip.getBillAmount(),
                        tip.getTipAmount(),
                        tip.getTipPercentage(),
                        tip.getCurrency(),
                        tip.getCreatedAt(),
                        finalScore,
                        reasons
                ));
            }
        }

        scoredMatches.sort(Comparator
                .comparingInt(ReceiptMatchResponse::matchScore).reversed()
                .thenComparing(Comparator.comparing(ReceiptMatchResponse::createdAt).reversed())
        );

        ReceiptReconciliationStatus status = ReceiptReconciliationStatus.NO_MATCH;
        String message = "No matching tip found in your history.";

        if (!scoredMatches.isEmpty()) {
            int topScore = scoredMatches.get(0).matchScore();
            if (topScore >= 80) {
                status = ReceiptReconciliationStatus.STRONG_MATCH;
                message = "A likely matching tip already exists.";
            } else if (topScore >= 50) {
                status = ReceiptReconciliationStatus.POSSIBLE_MATCH;
                message = "A possible matching tip was found.";
            }
        }

        return new ReceiptReconciliationResponse(
                status,
                scoredMatches.size(),
                scoredMatches,
                message
        );
    }

    private String normalizeRestaurant(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }
    
    private String getCurrencySymbol(String currency) {
        return switch (currency) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "INR" -> "₹";
            default -> currency + " ";
        };
    }
}
