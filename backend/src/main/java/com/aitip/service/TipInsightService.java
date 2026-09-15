package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.insights.*;
import com.aitip.entity.Tip;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TipInsightService {

    private static final Logger log = LoggerFactory.getLogger(TipInsightService.class);

    private final TipRepository tipRepository;
    private final TipInsightPromptBuilder promptBuilder;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public TipInsightService(TipRepository tipRepository, TipInsightPromptBuilder promptBuilder, GeminiService geminiService, ObjectMapper objectMapper, UserRepository userRepository) {
        this.tipRepository = tipRepository;
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public TipInsightSummaryResponse getInsights(String email) {
        UUID userId = userRepository.findByEmail(email).orElseThrow().getId();
        List<Tip> allTips = tipRepository.findAllByUserId(userId);
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        List<Tip> recentTips = new ArrayList<>();
        List<Tip> previousTips = new ArrayList<>();

        for (Tip tip : allTips) {
            if (!tip.getCreatedAt().isBefore(thirtyDaysAgo) && tip.getCreatedAt().isBefore(now)) {
                recentTips.add(tip);
            } else if (!tip.getCreatedAt().isBefore(sixtyDaysAgo) && tip.getCreatedAt().isBefore(thirtyDaysAgo)) {
                previousTips.add(tip);
            }
        }

        TipInsightResponse overallTrend = calculateOverallTrend(recentTips, previousTips);
        List<RestaurantTrend> restaurantTrends = calculateRestaurantTrends(allTips);
        Map<ServiceQuality, ServiceQualityTrend> serviceQualityTrends = calculateServiceQualityTrends(allTips);
        Map<String, MonthlyTrend> monthlyTrends = calculateMonthlyTrends(allTips);

        String generatedInsightMessage = null;
        try {
            String prompt = promptBuilder.buildPrompt(overallTrend, restaurantTrends, serviceQualityTrends);
            java.util.concurrent.CompletableFuture<String> future = java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> geminiService.getRecommendation(prompt));
            String rawAiResponse = future.get(3, java.util.concurrent.TimeUnit.SECONDS);
            generatedInsightMessage = parseAndValidateInsightJson(rawAiResponse);
        } catch (Exception e) {
            log.warn("Insight generation via AI failed or timed out: {}", e.getMessage());
            // Safe fallback, do not return 503
        }

        return new TipInsightSummaryResponse(
                overallTrend,
                restaurantTrends,
                serviceQualityTrends,
                monthlyTrends,
                generatedInsightMessage
        );
    }

    private TipInsightResponse calculateOverallTrend(List<Tip> recentTips, List<Tip> previousTips) {
        BigDecimal recentAvg = calculateAverage(recentTips);
        BigDecimal previousAvg = calculateAverage(previousTips);

        String trendDirection = "INSUFFICIENT_DATA";
        BigDecimal percentageChange = null;

        if (recentTips.size() >= 2 && previousTips.size() >= 2) {
            if (previousAvg.compareTo(BigDecimal.ZERO) == 0) {
                trendDirection = "INSUFFICIENT_DATA";
            } else {
                percentageChange = recentAvg.subtract(previousAvg)
                        .divide(previousAvg, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);

                if (percentageChange.compareTo(new BigDecimal("5.00")) >= 0) {
                    trendDirection = "INCREASING";
                } else if (percentageChange.compareTo(new BigDecimal("-5.00")) <= 0) {
                    trendDirection = "DECREASING";
                } else {
                    trendDirection = "STABLE";
                }
            }
        }

        int totalSampleSize = recentTips.size() + previousTips.size();
        String confidence = "LOW";
        if (totalSampleSize >= 5) {
            confidence = "HIGH";
        } else if (totalSampleSize >= 2) {
            confidence = "MEDIUM";
        }

        return new TipInsightResponse(
                trendDirection,
                recentAvg,
                previousAvg,
                percentageChange,
                totalSampleSize,
                confidence,
                null
        );
    }

    private List<RestaurantTrend> calculateRestaurantTrends(List<Tip> allTips) {
        Map<String, List<Tip>> grouped = new HashMap<>();
        for (Tip t : allTips) {
            if (t.getRestaurantName() != null && t.getCurrency() != null) {
                String key = t.getRestaurantName() + "|||" + t.getCurrency();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }
        }

        List<RestaurantTrend> trends = new ArrayList<>();
        for (Map.Entry<String, List<Tip>> entry : grouped.entrySet()) {
            List<Tip> tips = entry.getValue();
            if (tips.size() < 2) continue;

            String[] parts = entry.getKey().split("\\|\\|\\|");
            String restaurantName = parts[0];
            String currency = parts[1];

            BigDecimal avg = calculateAverage(tips);
            BigDecimal median = calculateMedian(tips);
            BigDecimal totalAmount = tips.stream().map(Tip::getTipAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate trend for restaurant if enough data
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);

            List<Tip> recent = new ArrayList<>();
            List<Tip> previous = new ArrayList<>();
            for (Tip tip : tips) {
                if (!tip.getCreatedAt().isBefore(thirtyDaysAgo) && tip.getCreatedAt().isBefore(LocalDateTime.now())) {
                    recent.add(tip);
                } else if (!tip.getCreatedAt().isBefore(sixtyDaysAgo) && tip.getCreatedAt().isBefore(thirtyDaysAgo)) {
                    previous.add(tip);
                }
            }
            
            String trendDirection = "INSUFFICIENT_DATA";
            if (recent.size() >= 2 && previous.size() >= 2) {
                BigDecimal rAvg = calculateAverage(recent);
                BigDecimal pAvg = calculateAverage(previous);
                if (pAvg.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal change = rAvg.subtract(pAvg)
                            .divide(pAvg, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
                    if (change.compareTo(new BigDecimal("5.00")) >= 0) trendDirection = "INCREASING";
                    else if (change.compareTo(new BigDecimal("-5.00")) <= 0) trendDirection = "DECREASING";
                    else trendDirection = "STABLE";
                }
            }

            trends.add(new RestaurantTrend(restaurantName, tips.size(), avg, median, totalAmount, currency, trendDirection));
        }
        
        trends.sort((a, b) -> b.tipCount().compareTo(a.tipCount()));
        return trends;
    }

    private Map<ServiceQuality, ServiceQualityTrend> calculateServiceQualityTrends(List<Tip> allTips) {
        Map<ServiceQuality, List<Tip>> grouped = allTips.stream()
                .filter(t -> t.getServiceQuality() != null)
                .collect(Collectors.groupingBy(Tip::getServiceQuality));

        Map<ServiceQuality, ServiceQualityTrend> trends = new HashMap<>();
        for (Map.Entry<ServiceQuality, List<Tip>> entry : grouped.entrySet()) {
            List<Tip> tips = entry.getValue();
            BigDecimal avg = calculateAverage(tips);
            BigDecimal median = calculateMedian(tips);
            trends.put(entry.getKey(), new ServiceQualityTrend(tips.size(), avg, median));
        }
        return trends;
    }

    private Map<String, MonthlyTrend> calculateMonthlyTrends(List<Tip> allTips) {
        Map<String, List<Tip>> grouped = allTips.stream()
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getCreatedAt()).toString()));

        Map<String, MonthlyTrend> result = new TreeMap<>();
        for (Map.Entry<String, List<Tip>> entry : grouped.entrySet()) {
            List<Tip> tips = entry.getValue();
            BigDecimal avg = calculateAverage(tips);
            
            Map<String, BigDecimal> amounts = new HashMap<>();
            for (Tip t : tips) {
                if (t.getCurrency() != null) {
                    amounts.merge(t.getCurrency(), t.getTipAmount(), BigDecimal::add);
                }
            }
            result.put(entry.getKey(), new MonthlyTrend(tips.size(), avg, amounts));
        }
        return result;
    }

    private BigDecimal calculateAverage(List<Tip> tips) {
        if (tips.isEmpty()) return null;
        BigDecimal sum = tips.stream().map(Tip::getTipPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(tips.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMedian(List<Tip> tips) {
        if (tips.isEmpty()) return null;
        List<BigDecimal> sorted = tips.stream()
                .map(Tip::getTipPercentage)
                .sorted()
                .toList();
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        } else {
            BigDecimal m1 = sorted.get(size / 2 - 1);
            BigDecimal m2 = sorted.get(size / 2);
            return m1.add(m2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
    }

    private String parseAndValidateInsightJson(String rawAiResponse) throws JsonProcessingException {
        String cleanJson = rawAiResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        JsonNode root = objectMapper.readTree(cleanJson);
        if (!root.has("headline") || !root.has("explanation") || !root.has("suggestion")) {
            throw new IllegalArgumentException("AI response missing required fields");
        }
        return cleanJson;
    }
}
