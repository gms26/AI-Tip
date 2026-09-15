package com.aitip.service;

import com.aitip.dto.TipEvolutionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TipEvolutionPromptBuilder {

    private final ObjectMapper objectMapper;

    public TipEvolutionPromptBuilder(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            this.objectMapper = new ObjectMapper().findAndRegisterModules();
        } else {
            this.objectMapper = objectMapper.copy().findAndRegisterModules();
        }
    }

    public String buildPrompt(TipEvolutionResponse evolution) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are explaining a user's historical tipping evolution based on their actual tip data.\n\n");
        prompt.append("Use ONLY the supplied backend facts.\n\n");
        prompt.append("CRITICAL RULES:\n");
        prompt.append("- Do NOT calculate statistics or invent values.\n");
        prompt.append("- Do NOT change classifications or the deterministic direction.\n");
        prompt.append("- Do NOT predict future behavior or provide forecasts.\n");
        prompt.append("- Do NOT provide financial advice.\n");
        prompt.append("- Do NOT make guarantees.\n\n");
        prompt.append("You may explain:\n");
        prompt.append("- Whether tipping behavior became more or less generous over time, or stayed stable.\n");
        prompt.append("- Consistency changes across active months.\n");
        prompt.append("- Restaurant diversity exploration patterns.\n");
        prompt.append("- Service-quality tipping patterns across months.\n");
        prompt.append("- Notable historical shifts in behavior.\n\n");
        prompt.append("Keep the response concise, engaging, and friendly (2-3 paragraphs).\n\n");
        prompt.append("Backend Deterministic Evolution Facts:\n");

        try {
            // Exclude aiExplanation when serializing facts
            TipEvolutionResponse facts = new TipEvolutionResponse(
                    evolution.generatedAt(),
                    evolution.period(),
                    evolution.currency(),
                    evolution.totalTipCount(),
                    evolution.activeMonths(),
                    evolution.overallDirection(),
                    evolution.currentMedianTipPercentage(),
                    evolution.currentAverageTipPercentage(),
                    evolution.historicalMedianTipPercentage(),
                    evolution.historicalAverageTipPercentage(),
                    evolution.currencyTimelines(),
                    evolution.serviceQualityEvolution(),
                    evolution.message(),
                    null
            );
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(facts);
            prompt.append(json);
        } catch (JsonProcessingException e) {
            prompt.append("[Error serializing evolution facts]");
        }

        return prompt.toString();
    }
}
