package com.aitip.service;

import com.aitip.dto.TipScenarioResponse;
import com.aitip.dto.TipScenarioResult;
import org.springframework.stereotype.Component;

/**
 * Constructs prompts for Groq to explain what-if scenario facts.
 *
 * <p><b>Core principle:</b> Groq receives only BACKEND-CALCULATED FACTS.
 * It may explain those facts but must not:</p>
 * <ul>
 *   <li>Recalculate any values</li>
 *   <li>Invent tip amounts or percentages</li>
 *   <li>Change scenario results</li>
 *   <li>Provide financial/tax/legal advice</li>
 *   <li>Make financial promises</li>
 *   <li>Claim scenarios are guaranteed outcomes</li>
 * </ul>
 */
@Component
public class TipScenarioPromptBuilder {

    /**
     * Builds a prompt for Groq to explain scenario comparison insights.
     *
     * @param response the backend-calculated scenario response
     * @return prompt string for Groq
     */
    public String buildPrompt(TipScenarioResponse response) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a helpful tip spending assistant. Explain the following ");
        sb.append("BACKEND-CALCULATED WHAT-IF SCENARIO FACTS to the user.\n\n");

        sb.append("== STRICT RULES ==\n");
        sb.append("1. Use ONLY the supplied values. Do NOT perform financial calculations.\n");
        sb.append("2. Do NOT invent values, amounts, or percentages.\n");
        sb.append("3. Do NOT alter any supplied facts.\n");
        sb.append("4. Do NOT provide legal, tax, or financial advice.\n");
        sb.append("5. Do NOT claim scenarios are guaranteed outcomes.\n");
        sb.append("6. Clearly state these are HYPOTHETICAL what-if scenarios.\n");
        sb.append("7. Use language like 'hypothetical', 'if you were to tip', 'would result in'.\n");
        sb.append("8. Return plain text, not JSON.\n\n");

        sb.append("== BACKEND-CALCULATED FACTS ==\n");
        sb.append("Currency: ").append(response.currency()).append("\n");
        sb.append("Bill Amount: ").append(response.currency()).append(" ")
                .append(response.billAmount()).append("\n");
        sb.append("Historical Tip Count: ").append(response.historicalTipCount()).append("\n");

        if (response.historicalMedianTipPercentage() != null) {
            sb.append("Historical Median Tip %: ")
                    .append(response.historicalMedianTipPercentage()).append("%\n");
        }
        if (response.historicalAverageTipPercentage() != null) {
            sb.append("Historical Average Tip %: ")
                    .append(response.historicalAverageTipPercentage()).append("%\n");
        }
        if (response.historicalMonthlyTipAmount() != null) {
            sb.append("Historical Monthly Spending: ").append(response.currency()).append(" ")
                    .append(response.historicalMonthlyTipAmount()).append("\n");
        }
        if (response.monthlyBudget() != null) {
            sb.append("Monthly Budget: ").append(response.currency()).append(" ")
                    .append(response.monthlyBudget()).append("\n");
        }

        sb.append("\n== SCENARIOS ==\n");
        for (int i = 0; i < response.scenarioResults().size(); i++) {
            TipScenarioResult r = response.scenarioResults().get(i);
            sb.append(String.format("Scenario %d: %s%% tip â†’ %s %s tip, %s %s total",
                    i + 1, r.tipPercentage(), response.currency(), r.tipAmount(),
                    response.currency(), r.totalAmount()));
            if (r.differenceFromHistorical() != null) {
                sb.append(String.format(" (%s%s pp from median)",
                        r.differenceFromHistorical().signum() >= 0 ? "+" : "",
                        r.differenceFromHistorical()));
            }
            if (r.budgetStatus() != null) {
                sb.append(String.format(" [Budget: %s]", r.budgetStatus()));
            }
            sb.append("\n");
        }

        sb.append("\n== TASK ==\n");
        sb.append("In 2-4 sentences, explain which scenarios are more conservative ");
        sb.append("versus more generous compared to the user's historical behavior. ");
        sb.append("If budget data is present, mention the budget impact. ");
        sb.append("Keep it friendly and neutral. Do not add any facts not listed above.\n");

        return sb.toString();
    }
}
