package com.aitip.service;

import com.aitip.dto.AiSuggestionRequest;
import com.aitip.dto.PersonalizationContext;
import org.springframework.stereotype.Component;

/**
 * Constructs the prompt for the Gemini LLM.
 * 
 * <p>Separating this logic from the AI orchestrator keeps the service layer
 * free of string manipulation and prompt engineering, following SRP.</p>
 */
@Component
public class PromptBuilder {

    /**
     * Builds a strict prompt asking for a JSON response containing specific keys.
     * Day 3 method — unchanged.
     */
    public String buildTipRecommendationPrompt(AiSuggestionRequest request) {
        return buildTipRecommendationPrompt(request, null);
    }

    /**
     * Builds a prompt with optional personalization context (Day 4).
     *
     * <p>When personalization data is available, it is appended as a
     * separate "PERSONALIZED USER CONTEXT" section. Gemini is instructed
     * to treat these as backend-verified facts, not to invent history.</p>
     */
    public String buildTipRecommendationPrompt(AiSuggestionRequest request, PersonalizationContext context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful and culturally-aware tip recommendation assistant.\n");
        prompt.append("Analyze the following service context and recommend a tip percentage.\n\n");
        
        prompt.append("--- USER INPUT ---\n");
        prompt.append("Bill Amount: ").append(request.billAmount()).append(" ").append(request.currency()).append("\n");
        prompt.append("Restaurant / Service Type: ").append(request.restaurantType()).append("\n");
        prompt.append("Service Quality: ").append(request.serviceQuality()).append("\n");
        prompt.append("Country: ").append(request.country()).append("\n");
        if (request.occasion() != null && !request.occasion().isBlank()) {
            prompt.append("Occasion: ").append(request.occasion()).append("\n");
        }
        if (request.restaurantName() != null && !request.restaurantName().isBlank()) {
            prompt.append("Restaurant Name: ").append(request.restaurantName()).append("\n");
        }
        
        // Append personalization if available
        if (context != null && context.hasPersonalizationData()) {
            prompt.append("\n--- HISTORICAL FACTS ---\n");
            prompt.append("The following statistics are calculated from the user's actual tip history.\n");
            prompt.append("Treat these as factual. Do NOT invent additional history.\n\n");
            
            prompt.append("AUTHORITATIVE PERSONALIZATION SOURCE:\n");
            prompt.append(context.source().name()).append("\n");
            prompt.append("Use this layer as the primary historical basis for your recommendation.\n");
            prompt.append("Other statistical layers are supporting context only.\n");
            prompt.append("Do not override the authoritative source with another layer.\n\n");

            prompt.append("--- CALCULATED STATISTICS ---\n");
            
            if (context.hasRestaurantAndServiceData()) {
                prompt.append("Restaurant + Service Quality (").append(request.restaurantName()).append(" + ").append(request.serviceQuality()).append("):\n");
                prompt.append("  Visits: ").append(context.restaurantAndServiceCount()).append("\n");
                prompt.append("  Median tip: ").append(context.restaurantAndServiceMedianTipPercentage()).append("%\n");
                prompt.append("  Average tip: ").append(context.restaurantAndServiceAverageTipPercentage()).append("%\n\n");
            }

            if (context.hasRestaurantData()) {
                prompt.append("Restaurant (").append(request.restaurantName()).append("):\n");
                prompt.append("  Visits: ").append(context.restaurantVisitCount()).append("\n");
                prompt.append("  Median tip: ").append(context.restaurantMedianTipPercentage()).append("%\n");
                prompt.append("  Average tip: ").append(context.restaurantAverageTipPercentage()).append("%\n\n");
            }

            if (context.hasServiceQualityData()) {
                prompt.append("Service Quality (").append(request.serviceQuality()).append("):\n");
                prompt.append("  Visits: ").append(context.serviceQualityCount()).append("\n");
                prompt.append("  Median tip: ").append(context.serviceQualityMedianTipPercentage()).append("%\n");
                prompt.append("  Average tip: ").append(context.serviceQualityAverageTipPercentage()).append("%\n\n");
            }

            prompt.append("Overall History:\n");
            prompt.append("  Total tips recorded: ").append(context.overallTipCount()).append("\n");
            prompt.append("  Median tip: ").append(context.overallMedianTipPercentage()).append("%\n");
            prompt.append("  Average tip: ").append(context.overallAverageTipPercentage()).append("%\n");
            
            if (context.hasRestaurantData() && context.lastTipPercentageAtRestaurant() != null) {
                prompt.append("\n--- LAST VISIT ---\n");
                prompt.append("Tip: ").append(context.lastTipPercentageAtRestaurant()).append("%\n");
                if (context.lastServiceQualityAtRestaurant() != null) {
                    prompt.append("Service: ").append(context.lastServiceQualityAtRestaurant()).append("\n");
                }
                prompt.append("Date: ").append(context.lastVisitDateAtRestaurant()).append("\n");
            }
        }
        
        prompt.append("\n--- RECOMMENDATION REQUEST ---\n");
        prompt.append("1. Consider the customary tipping etiquette for the provided country and service type.\n");
        prompt.append("2. Adjust the recommendation based on the service quality (POOR, AVERAGE, GOOD, EXCELLENT).\n");
        if (context != null && context.hasPersonalizationData()) {
            prompt.append("3. Consider the user's personal tipping history when making your recommendation, prioritizing the authoritative source.\n");
            prompt.append("4. Use the median tip (not average) from the authoritative source as your primary benchmark.\n");
            prompt.append("5. If a historical context is used, reference it accurately in your reason.\n");
        }
        prompt.append("6. Provide a recommended percentage, as well as a minimum and maximum acceptable percentage.\n");
        prompt.append("7. Provide a brief 1-2 sentence reason for your recommendation.\n");
        prompt.append("8. DO NOT calculate any monetary amounts. Just provide percentages.\n");
        prompt.append("9. DO NOT calculate confidence scores.\n");
        prompt.append("10. RETURN ONLY PURE JSON. Do NOT wrap the JSON in markdown code blocks like ```json ... ```. Just return the JSON object directly.\n");
        
        prompt.append("\n--- EXPECTED JSON SCHEMA ---\n");
        prompt.append("{\n");
        prompt.append("  \"recommendedPercentage\": 20,\n");
        prompt.append("  \"minimumPercentage\": 18,\n");
        prompt.append("  \"maximumPercentage\": 20,\n");
        prompt.append("  \"reason\": \"...\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }
}

