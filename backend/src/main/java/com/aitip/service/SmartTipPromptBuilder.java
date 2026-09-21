package com.aitip.service;

import com.aitip.dto.SmartTipResponse;
import com.aitip.dto.SmartTipSuggestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Builds prompts for the optional Groq explanation in the Smart Tip Assistant.
 *
 * <p>Strict rule: Groq must only explain the backend-provided factual suggestions.
 * Groq must never calculate tip percentages or amounts, alter suggestions, or give financial advice.</p>
 */
@Component
public class SmartTipPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(SmartTipPromptBuilder.class);
    private final ObjectMapper objectMapper;

    public SmartTipPromptBuilder() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Constructs a structured prompt containing only verified backend facts.
     */
    public String buildPrompt(SmartTipResponse response) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are the AI Tip Assistant explaining personalized tipping options to a user before they log a tip.\n");
        sb.append("Explain the contextual rationale behind the suggested tip options clearly, concisely, and neutrally (2-3 short paragraphs).\n\n");

        sb.append("CRITICAL RULES:\n");
        sb.append("- Use ONLY the supplied backend facts below.\n");
        sb.append("- Do NOT calculate tip percentages or money amounts.\n");
        sb.append("- Do NOT invent or propose new percentages or suggestions.\n");
        sb.append("- Do NOT change budget status or historical classifications.\n");
        sb.append("- Do NOT invent restaurant behavior or service-quality patterns.\n");
        sb.append("- Do NOT provide financial advice or pressure the user to tip more.\n\n");

        sb.append("=== BACKEND FACTS ===\n");
        sb.append("Bill Amount: ").append(response.billAmount()).append(" ").append(response.currency()).append("\n");

        if (response.historicalMedianTipPercentage() != null) {
            sb.append("Historical Baseline: Median = ").append(response.historicalMedianTipPercentage())
                    .append("%, Average = ").append(response.historicalAverageTipPercentage()).append("%\n");
        } else {
            sb.append("Historical Baseline: No prior history for this currency.\n");
        }

        if (response.optimizedMinimumPercentage() != null && response.optimizedMaximumPercentage() != null) {
            sb.append("Optimized Range: ").append(response.optimizedMinimumPercentage())
                    .append("% - ").append(response.optimizedMaximumPercentage()).append("%\n");
        }

        if (response.restaurantTipCount() != null && response.restaurantTipCount() > 0) {
            sb.append("Restaurant History: ").append(response.restaurantTipCount()).append(" previous visits, Typical = ")
                    .append(response.restaurantMedianTipPercentage()).append("%\n");
        }

        if (response.serviceQualityTipCount() != null && response.serviceQualityTipCount() > 0) {
            sb.append("Service Quality History: ").append(response.serviceQualityTipCount()).append(" visits with this rating, Typical = ")
                    .append(response.serviceQualityAverageTipPercentage()).append("%\n");
        }

        if (response.budgetStatus() != null) {
            sb.append("Monthly Budget Status: ").append(response.budgetStatus())
                    .append(" (Current usage: ").append(response.budgetUsagePercentage()).append("%)\n");
        }

        sb.append("Recent Tipping Direction: ").append(response.recentDirection()).append("\n");

        if (Boolean.TRUE.equals(response.goalRelevant())) {
            sb.append("Active Goal: Option aligns with active tipping goal.\n");
        }

        sb.append("\n=== CALCULATED SUGGESTIONS ===\n");
        for (SmartTipSuggestion s : response.suggestions()) {
            sb.append("- ").append(s.label()).append(": ").append(s.tipPercentage()).append("% (")
                    .append(s.tipAmount()).append(" ").append(response.currency()).append(")")
                    .append(Boolean.TRUE.equals(s.isRecommended()) ? " [RECOMMENDED]" : "")
                    .append(" - Reason: ").append(s.reason());
            if (s.budgetImpact() != null) {
                sb.append(" | Budget Impact: ").append(s.budgetImpact());
            }
            sb.append("\n");
        }

        sb.append("\nProvide a warm, advisory summary explaining how these options relate to their past habits and context.");
        return sb.toString();
    }
}
