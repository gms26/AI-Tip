package com.aitip.service;

import com.aitip.dto.BudgetConfidence;
import com.aitip.dto.TipBudgetStatus;
import com.aitip.dto.TipForecastResponse;
import org.springframework.stereotype.Component;

/**
 * Constructs prompts for Groq to explain tip forecast facts conversationally.
 *
 * <p><b>Core principle:</b> Groq receives only BACKEND-CALCULATED FACTS.
 * It may explain those facts but must not:</p>
 * <ul>
 *   <li>Recalculate statistics</li>
 *   <li>Change forecast values</li>
 *   <li>Invent spending amounts</li>
 *   <li>Claim the forecast is guaranteed</li>
 *   <li>Provide tax/legal/financial advice</li>
 *   <li>Make financial promises</li>
 * </ul>
 */
@Component
public class TipForecastPromptBuilder {

    /**
     * Builds a prompt for Groq to explain forecast insights.
     *
     * @param response the backend-calculated forecast response
     * @return prompt string for Groq
     */
    public String buildPrompt(TipForecastResponse response) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a helpful tip spending assistant. Explain the following BACKEND-CALCULATED FORECAST FACTS ");
        sb.append("to the user in a friendly, conversational tone.\n\n");

        sb.append("== STRICT RULES ==\n");
        sb.append("1. Do NOT recalculate any statistics yourself.\n");
        sb.append("2. Do NOT alter any amounts or percentages provided below.\n");
        sb.append("3. Do NOT invent additional spending or savings figures.\n");
        sb.append("4. Do NOT claim the forecast is guaranteed or promised.\n");
        sb.append("5. Do NOT provide legal, tax, or financial advice.\n");
        sb.append("6. Do NOT make financial promises or commitments.\n");
        sb.append("7. Use language like 'estimated', 'projected', 'based on your recent history'.\n");
        sb.append("8. Clearly distinguish historical facts from projections.\n");
        sb.append("9. Return a plain text explanation, not JSON.\n\n");

        sb.append("== BACKEND-CALCULATED FACTS ==\n");
        sb.append("Currency: ").append(response.currency()).append("\n");
        sb.append("Forecast Period: ").append(response.forecastPeriod()).append("\n");
        sb.append("Forecast Days: ").append(response.forecastDays()).append("\n");
        sb.append("Lookback Days: ").append(response.lookbackDays()).append("\n");
        sb.append("Historical Tip Count: ").append(response.historicalTipCount()).append("\n");
        sb.append("Confidence: ").append(response.confidence()).append("\n");

        if (response.historicalAveragePercentage() != null) {
            sb.append("Historical Average Tip %: ").append(response.historicalAveragePercentage()).append("%\n");
        }
        if (response.historicalMedianPercentage() != null) {
            sb.append("Historical Median Tip %: ").append(response.historicalMedianPercentage()).append("%\n");
        }
        if (response.historicalAverageTipAmount() != null) {
            sb.append("Historical Average Tip Amount: ").append(response.currency()).append(" ")
                    .append(response.historicalAverageTipAmount()).append("\n");
        }
        sb.append("Estimated Tip Count: ").append(response.estimatedTipCount()).append("\n");
        if (response.estimatedMonthlyTipAmount() != null) {
            sb.append("Estimated Spending: ").append(response.currency()).append(" ")
                    .append(response.estimatedMonthlyTipAmount()).append("\n");
        }
        if (response.monthlyBudget() != null) {
            sb.append("Monthly Budget: ").append(response.currency()).append(" ")
                    .append(response.monthlyBudget()).append("\n");
        }
        if (response.projectedBudgetUsage() != null) {
            sb.append("Projected Budget Usage: ").append(response.projectedBudgetUsage()).append("%\n");
        }
        if (response.budgetStatus() != null) {
            sb.append("Budget Status: ").append(response.budgetStatus()).append("\n");
        }

        sb.append("\n== TASK ==\n");
        sb.append("Explain the above facts to the user in 2-3 sentences. ");
        sb.append("Clearly label projections as estimates. Keep it friendly and neutral. ");
        sb.append("Do not add any facts not listed above.\n");

        return sb.toString();
    }
}
