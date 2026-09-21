package com.aitip.service;

import com.aitip.dto.AiSuggestionRequest;
import com.aitip.dto.AiSuggestionResponse;
import com.aitip.dto.PersonalizationContext;
import com.aitip.exception.AiServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Orchestrator for AI tip recommendations.
 * 
 * <p>Delegates prompt building, network requests, JSON parsing, 
 * output validation, and final monetary calculations.</p>
 */
@Service
public class AiSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionService.class);
    
    private final PromptBuilder promptBuilder;
    private final AiProvider AiProvider;
    private final TipCalculationService tipCalculationService;
    private final ObjectMapper objectMapper;

    public AiSuggestionService(
            PromptBuilder promptBuilder, 
            AiProvider AiProvider, 
            TipCalculationService tipCalculationService,
            ObjectMapper objectMapper) {
        this.promptBuilder = promptBuilder;
        this.AiProvider = AiProvider;
        this.tipCalculationService = tipCalculationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Day 3 method â€” gets a recommendation WITHOUT personalization context.
     * Preserved for backward compatibility.
     */
    public AiSuggestionResponse getRecommendation(AiSuggestionRequest request) {
        return getRecommendation(request, null);
    }

    /**
     * Day 4 method â€” gets a recommendation WITH optional personalization context.
     * 
     * @param request Validated contextual details
     * @param context Backend-calculated personalization (nullable for graceful fallback)
     * @return Complete response including backend-calculated monetary totals
     * @throws AiServiceException if any step of the AI process fails
     */
    public AiSuggestionResponse getRecommendation(AiSuggestionRequest request, PersonalizationContext context) {
        // 1. Build prompt (with or without personalization)
        String prompt = promptBuilder.buildTipRecommendationPrompt(request, context);
        
        // 2. Call Groq
        String rawResponse = AiProvider.getRecommendation(prompt);
        
        // 3. Parse & Validate
        ParsedAiResult parsed = parseAndValidateJson(rawResponse);
        
        // 4. Calculate exact money (AI is not allowed to do math)
        BigDecimal tipAmount = tipCalculationService.calculateTipAmount(request.billAmount(), parsed.recommendedPercentage());
        BigDecimal totalAmount = tipCalculationService.calculateTotalAmount(request.billAmount(), tipAmount);
        
        // 5. Determine confidence and source from context
        String confidence = "LOW";
        com.aitip.dto.PersonalizationSource source = com.aitip.dto.PersonalizationSource.NONE;
        
        if (context != null) {
            confidence = context.confidence();
            source = context.source();
        }

        // 6. Return complete response
        return new AiSuggestionResponse(
                parsed.recommendedPercentage(),
                parsed.minimumPercentage(),
                parsed.maximumPercentage(),
                tipAmount,
                totalAmount,
                parsed.reason(),
                confidence,
                source
        );
    }

    /**
     * Parses the AI's string response and applies strict domain validation.
     */
    private ParsedAiResult parseAndValidateJson(String rawResponse) {
        try {
            // Clean markdown if Groq mistakenly includes it despite instructions
            String cleanJson = rawResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            JsonNode root = objectMapper.readTree(cleanJson);
            
            if (!root.has("recommendedPercentage") || !root.has("minimumPercentage") || 
                !root.has("maximumPercentage") || !root.has("reason")) {
                throw new AiServiceException("AI response is missing required fields");
            }
            
            BigDecimal rec = new BigDecimal(root.get("recommendedPercentage").asText());
            BigDecimal min = new BigDecimal(root.get("minimumPercentage").asText());
            BigDecimal max = new BigDecimal(root.get("maximumPercentage").asText());
            String reason = root.get("reason").asText();
            
            // Validate bounds
            if (rec.compareTo(BigDecimal.ZERO) < 0 || rec.compareTo(new BigDecimal("100")) > 0) {
                throw new AiServiceException("Recommended percentage out of bounds: " + rec);
            }
            if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(new BigDecimal("100")) > 0) {
                throw new AiServiceException("Min/Max percentages out of bounds");
            }
            if (min.compareTo(rec) > 0 || rec.compareTo(max) > 0) {
                throw new AiServiceException("Percentage range is illogical (min <= rec <= max violated)");
            }
            if (reason.isBlank()) {
                throw new AiServiceException("AI provided blank reason");
            }
            
            return new ParsedAiResult(rec, min, max, reason);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Groq JSON output: {}", rawResponse);
            throw new AiServiceException("AI returned malformed JSON", e);
        } catch (NumberFormatException e) {
            log.error("Failed to parse percentage numbers from Groq output: {}", rawResponse);
            throw new AiServiceException("AI returned invalid numeric format", e);
        }
    }
    
    // Internal record just for passing parsed data
    private record ParsedAiResult(
            BigDecimal recommendedPercentage,
            BigDecimal minimumPercentage,
            BigDecimal maximumPercentage,
            String reason
    ) {}
}
