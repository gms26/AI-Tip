package com.aitip.service;

import com.aitip.dto.TipRecommendation;
import com.aitip.dto.TipRecommendationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TipRecommendationPromptBuilder {

    private final ObjectMapper objectMapper;

    public TipRecommendationPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildPrompt(List<TipRecommendation> recommendations, String summary) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are explaining recommendations already calculated by the backend.\n\n");
        prompt.append("Do not:\n");
        prompt.append("- recalculate values\n");
        prompt.append("- invent values\n");
        prompt.append("- create recommendations\n");
        prompt.append("- change priorities\n");
        prompt.append("- provide financial guarantees\n");
        prompt.append("- provide financial advice\n");
        prompt.append("- introduce facts not present in the supplied data\n\n");
        prompt.append("Use only the supplied facts.\n\n");
        prompt.append("Produce a concise, friendly explanation covering the most important recommendation, ");
        prompt.append("why it appeared, what the user may want to review, and positive progress where applicable. ");
        prompt.append("Keep the response to 2-3 short paragraphs at most.\n\n");
        prompt.append("Backend Deterministic Summary:\n").append(summary).append("\n\n");
        prompt.append("Backend Deterministic Recommendations:\n");
        
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(recommendations);
            prompt.append(json);
        } catch (JsonProcessingException e) {
            prompt.append("[Error serializing recommendations]");
        }

        return prompt.toString();
    }
}
