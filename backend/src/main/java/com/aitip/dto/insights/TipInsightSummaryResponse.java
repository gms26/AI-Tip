package com.aitip.dto.insights;

import com.aitip.dto.ServiceQuality;

import java.util.List;
import java.util.Map;

public record TipInsightSummaryResponse(
        TipInsightResponse overallTrend,
        List<RestaurantTrend> topRestaurants,
        Map<ServiceQuality, ServiceQualityTrend> serviceQualityTrends,
        Map<String, MonthlyTrend> monthlyTrends,
        String generatedInsightMessage
) {}
