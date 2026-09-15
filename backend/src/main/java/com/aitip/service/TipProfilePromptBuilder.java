package com.aitip.service;

import com.aitip.dto.TipProfileResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TipProfilePromptBuilder {

    private final ObjectMapper objectMapper;

    public TipProfilePromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildPrompt(TipProfileResponse profile) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are explaining a user's tipping profile based on their historical data.\n\n");
        prompt.append("Use ONLY the supplied backend facts.\n\n");
        prompt.append("CRITICAL RULES:\n");
        prompt.append("- Do NOT calculate values.\n");
        prompt.append("- Do NOT invent statistics.\n");
        prompt.append("- Do NOT change classifications or styles (e.g., if behaviorType is VERY_CONSISTENT, keep it that way).\n");
        prompt.append("- Do NOT change consistency scores.\n");
        prompt.append("- Do NOT provide financial advice.\n");
        prompt.append("- Do NOT make guarantees.\n\n");
        prompt.append("Explain the user's general tipping style, consistency, notable patterns (e.g. top restaurants or service quality patterns), ");
        prompt.append("recent changes, and highlight positive observations.\n");
        prompt.append("Keep the response to 2-3 concise, friendly paragraphs.\n\n");
        prompt.append("Backend Deterministic Profile:\n");

        try {
            // Exclude the AI explanation field from the JSON to avoid confusion
            TipProfileResponse profileToSerialize = new TipProfileResponse(
                    profile.generatedAt(),
                    profile.totalTipCount(),
                    profile.currencyProfiles(),
                    profile.overallBehaviorType(),
                    profile.overallTipStyle(),
                    profile.historicalMedianTipPercentage(),
                    profile.historicalAverageTipPercentage(),
                    profile.tipPercentageMin(),
                    profile.tipPercentageMax(),
                    profile.consistencyScore(),
                    profile.recentMedianTipPercentage(),
                    profile.recentTrend(),
                    profile.topRestaurants(),
                    profile.topServiceQualities(),
                    profile.message(),
                    null // clear AI explanation
            );
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profileToSerialize);
            prompt.append(json);
        } catch (JsonProcessingException e) {
            prompt.append("[Error serializing profile facts]");
        }

        return prompt.toString();
    }
}
