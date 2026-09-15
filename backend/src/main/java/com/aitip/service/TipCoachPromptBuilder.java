package com.aitip.service;

import com.aitip.dto.TipCoachFocus;
import com.aitip.dto.TipCoachResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class TipCoachPromptBuilder {

    private final ObjectMapper objectMapper;

    public TipCoachPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildPrompt(TipCoachResponse coachResponse) {
        try {
            String jsonContext = objectMapper.writeValueAsString(coachResponse);

            return """
                    You are the AI Tip Assistant, explaining a personalized Smart Tipping Coach summary to the user.
                    
                    Your role is to explain the primary focus area identified by the backend and why it matters, using only the provided deterministic facts.
                    
                    CRITICAL RULES:
                    - Do NOT calculate any financial values yourself.
                    - Do NOT change the identified Coach Focus or Next Action.
                    - Do NOT invent statistics or insights.
                    - Do NOT provide financial advice.
                    - Do NOT guarantee future savings or predict exact future spending.
                    - Explain the focus in a friendly, helpful, and concise manner.
                    
                    COACH DATA (SOURCE OF TRUTH):
                    %s
                    
                    Provide a concise paragraph (2-3 sentences max) explaining the focus area and summarizing the data.
                    """.formatted(jsonContext);
        } catch (JsonProcessingException e) {
            return "Please review your current coach focus and summary.";
        }
    }
}
