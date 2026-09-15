package com.aitip.service;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.insights.RestaurantTrend;
import com.aitip.dto.insights.ServiceQualityTrend;
import com.aitip.dto.insights.TipInsightResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TipInsightPromptBuilder {

    public String buildPrompt(TipInsightResponse overallTrend, List<RestaurantTrend> restaurantTrends, Map<ServiceQuality, ServiceQualityTrend> serviceQualityTrends) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the AI Tip Assistant. Analyze the user's tipping history below and provide insights.\n\n");
        
        sb.append("== USER CONTEXT ==\n");
        sb.append("The user is reviewing their tipping behavior and spending trends.\n\n");
        
        sb.append("== CALCULATED STATISTICS ==\n");
        sb.append("Overall Trend Direction: ").append(overallTrend.trendDirection()).append("\n");
        if (overallTrend.recentAveragePercentage() != null) {
            sb.append("Recent Average Percentage: ").append(overallTrend.recentAveragePercentage()).append("%\n");
        }
        if (overallTrend.historicalAveragePercentage() != null) {
            sb.append("Previous Average Percentage: ").append(overallTrend.historicalAveragePercentage()).append("%\n");
        }
        if (overallTrend.percentageChange() != null) {
            sb.append("Percentage Change: ").append(overallTrend.percentageChange()).append("%\n");
        }
        sb.append("Total Sample Size: ").append(overallTrend.sampleSize()).append("\n");
        sb.append("Confidence: ").append(overallTrend.confidence()).append("\n\n");
        
        sb.append("== RESTAURANT DATA ==\n");
        if (restaurantTrends.isEmpty()) {
            sb.append("No restaurant data available.\n");
        } else {
            for (RestaurantTrend rt : restaurantTrends) {
                sb.append(String.format("- %s: %d tips, Avg %s%%, Median %s%%, Total %s %s, Trend %s\n",
                        rt.restaurantName(), rt.tipCount(), rt.averageTipPercentage(), rt.medianTipPercentage(), rt.totalTipAmount(), rt.currency(), rt.trendDirection()));
            }
        }
        sb.append("\n");
        
        sb.append("== SERVICE QUALITY DATA ==\n");
        if (serviceQualityTrends.isEmpty()) {
            sb.append("No service quality data available.\n");
        } else {
            for (Map.Entry<ServiceQuality, ServiceQualityTrend> entry : serviceQualityTrends.entrySet()) {
                sb.append(String.format("- %s: %d tips, Avg %s%%, Median %s%%\n",
                        entry.getKey(), entry.getValue().tipCount(), entry.getValue().averageTipPercentage(), entry.getValue().medianTipPercentage()));
            }
        }
        sb.append("\n");
        
        sb.append("== RECOMMENDATION REQUEST ==\n");
        sb.append("Generate a helpful insight message for the user based strictly on these facts.\n");
        sb.append("Rules:\n");
        sb.append("1. Do not calculate statistics.\n");
        sb.append("2. Do not invent historical facts.\n");
        sb.append("3. Do not change percentages.\n");
        sb.append("4. Do not provide tax/legal advice.\n");
        sb.append("5. Do not claim certainty when confidence is LOW.\n");
        sb.append("6. Return pure JSON only.\n\n");
        
        sb.append("Expected JSON format:\n");
        sb.append("{\n");
        sb.append("  \"headline\": \"A short, catchy summary\",\n");
        sb.append("  \"explanation\": \"A clear explanation of their trends without repeating all numbers\",\n");
        sb.append("  \"suggestion\": \"A practical tip for their future tipping behavior\"\n");
        sb.append("}");

        return sb.toString();
    }
}
