package com.aitip.service;

import com.aitip.dto.BudgetConfidence;
import com.aitip.dto.TipOptimizationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Constructs prompts for Groq to explain tip optimization facts conversationally.
 *
 * <p><b>Core principle:</b> Groq receives only BACKEND-CALCULATED FACTS.
 * It may explain those facts but must not:</p>
 * <ul>
 *   <li>Calculate statistics</li>
 *   <li>Alter percentages</li>
 *   <li>Invent savings</li>
 *   <li>Generate confidence</li>
 *   <li>Provide legal/tax advice</li>
 *   <li>Claim a universal correct tipping percentage</li>
 * </ul>
 */
@Component
public class TipOptimizationPromptBuilder {

    /**
     * Builds a prompt for Groq to explain optimization insights.
     *
     * @param response the backend-calculated optimization response
     * @return prompt string for Groq
     */
    public String buildPrompt(TipOptimizationResponse response) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a helpful tip spending assistant. Explain the following BACKEND-CALCULATED FACTS ");
        sb.append("to the user in a friendly, conversational tone.\n\n");

        sb.append("== STRICT RULES ==\n");
        sb.append("1. Do NOT calculate any statistics yourself.\n");
        sb.append("2. Do NOT alter any percentages or amounts provided below.\n");
        sb.append("3. Do NOT invent additional savings or spending figures.\n");
        sb.append("4. Do NOT generate or modify confidence levels.\n");
        sb.append("5. Do NOT provide legal, tax, or financial advice.\n");
        sb.append("6. Do NOT claim any percentage is universally 'correct' or 'recommended'.\n");
        sb.append("7. Use neutral language: 'Your historical median is X%' not 'You should tip X%'.\n");
        sb.append("8. Present this as personalized spending insight, not financial advice.\n");
        sb.append("9. Return a plain text explanation, not JSON.\n\n");

        sb.append("== BACKEND-CALCULATED FACTS ==\n");
        sb.append("Currency: ").append(response.currency()).append("\n");
        sb.append("Current Tip Percentage: ").append(response.currentTipPercentage()).append("%\n");
        sb.append("Sample Size: ").append(response.sampleSize()).append(" tips\n");
        sb.append("Confidence: ").append(response.confidence()).append("\n");

        if (response.historicalMedianPercentage() != null) {
            sb.append("Historical Median: ").append(response.historicalMedianPercentage()).append("%\n");
        }
        if (response.historicalMeanPercentage() != null) {
            sb.append("Historical Mean: ").append(response.historicalMeanPercentage()).append("%\n");
        }
        if (response.recommendedMinimumPercentage() != null) {
            sb.append("Personal Historical Range: ")
                    .append(response.recommendedMinimumPercentage()).append("% â€“ ")
                    .append(response.recommendedMaximumPercentage()).append("%\n");
        }
        if (response.monthlyTipEstimate() != null) {
            sb.append("Estimated Monthly Tip (at current %): ")
                    .append(response.currency()).append(" ")
                    .append(response.monthlyTipEstimate()).append("\n");
        }
        if (response.optimizedMonthlyTipEstimate() != null) {
            sb.append("Estimated Monthly Tip (at median %): ")
                    .append(response.currency()).append(" ")
                    .append(response.optimizedMonthlyTipEstimate()).append("\n");
        }
        if (response.potentialMonthlyDifference() != null) {
            sb.append("Potential Monthly Difference: ")
                    .append(response.currency()).append(" ")
                    .append(response.potentialMonthlyDifference()).append("\n");
        }

        sb.append("\n== TASK ==\n");
        sb.append("Explain the above facts to the user in 2-3 sentences. ");
        sb.append("Keep it friendly and neutral. Do not add any facts not listed above.\n");

        return sb.toString();
    }
}
