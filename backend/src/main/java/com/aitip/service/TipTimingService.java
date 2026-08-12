package com.aitip.service;

import com.aitip.dto.TipTimingRequest;
import com.aitip.dto.TipTimingResponse;
import com.aitip.dto.TipTimingState;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TipTimingService {

    private final TipRepository tipRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public TipTimingResponse getTipTiming(String email, TipTimingRequest request) {
        User user = userService.getUserByEmail(email);

        // 1. Saved = true -> NOT_READY
        if (Boolean.TRUE.equals(request.getSaved())) {
            return buildNotReady("Tip is already saved.");
        }

        // 2. Incomplete context -> NOT_READY
        if (request.getBillAmount() == null || request.getTipPercentage() == null ||
            request.getBillAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return buildNotReady("Waiting for valid bill and tip calculation.");
        }

        if (request.getRestaurantName() == null || request.getRestaurantName().trim().isEmpty()) {
            return buildNotReady("Waiting for restaurant name.");
        }

        String restaurantName = request.getRestaurantName().trim();
        List<Tip> history = tipRepository.findByUserIdAndRestaurantNameIgnoreCase(user.getId(), restaurantName);

        // 3. Complete context + No restaurant history -> GOOD_TIME (LOW confidence)
        if (history == null || history.isEmpty()) {
            return TipTimingResponse.builder()
                    .state(TipTimingState.GOOD_TIME)
                    .message("New restaurant! You can save this tip for future reference.")
                    .confidence("LOW")
                    .restaurantName(restaurantName)
                    .visitCount(0)
                    .hasRestaurantHistory(false)
                    .reason("No previous visits recorded.")
                    .build();
        }

        // 4. Complete context + Restaurant history -> RECOMMENDED
        int visitCount = history.size();
        String confidence = getConfidenceLevel(visitCount);

        // Sort by created_at desc to find last visit
        // Make a mutable copy first, just in case findBy... returns an immutable list
        List<Tip> sortedHistory = new java.util.ArrayList<>(history);
        sortedHistory.sort(Comparator.comparing(Tip::getCreatedAt).reversed());
        Tip lastVisit = sortedHistory.get(0);

        BigDecimal medianTip = calculateMedian(sortedHistory);

        return TipTimingResponse.builder()
                .state(TipTimingState.RECOMMENDED)
                .message("You've visited this restaurant before. This is a good time to record your tip.")
                .confidence(confidence)
                .restaurantName(restaurantName)
                .visitCount(visitCount)
                .lastVisitAt(lastVisit.getCreatedAt())
                .lastTipPercentage(lastVisit.getTipPercentage())
                .medianTipPercentage(medianTip)
                .hasRestaurantHistory(true)
                .reason("Based on your " + visitCount + " previous visit(s).")
                .build();
    }

    private TipTimingResponse buildNotReady(String reason) {
        return TipTimingResponse.builder()
                .state(TipTimingState.NOT_READY)
                .message("Not ready for timing recommendation.")
                .confidence("LOW")
                .hasRestaurantHistory(false)
                .reason(reason)
                .build();
    }

    private String getConfidenceLevel(int visitCount) {
        if (visitCount <= 1) return "LOW";
        if (visitCount <= 4) return "MEDIUM";
        return "HIGH";
    }

    private BigDecimal calculateMedian(List<Tip> tips) {
        if (tips == null || tips.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> percentages = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();

        int size = percentages.size();
        if (size % 2 == 1) {
            return percentages.get(size / 2);
        } else {
            BigDecimal mid1 = percentages.get(size / 2 - 1);
            BigDecimal mid2 = percentages.get(size / 2);
            return mid1.add(mid2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }
}
