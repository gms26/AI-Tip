package com.aitip.service;

import com.aitip.dto.AiRequest;
import com.aitip.dto.AiResponse;
import com.aitip.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Handles communication with the Groq API (OpenAI compatible).
 * 
 * <p>Uses Spring's native RestClient. Responsibilities are limited to HTTP 
 * transmission, timeouts, and returning the raw string response. 
 * Does NOT contain business logic.</p>
 */
@Service
public class AiProvider {

    private static final Logger log = LoggerFactory.getLogger(AiProvider.class);
    
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.groq.base-url}") String baseUrl,
            @Value("${app.groq.api-key}") String apiKey,
            @Value("${app.groq.model}") String model) {
            
        this.apiKey = apiKey;
        this.model = model;
        
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Sends the formatted prompt to Groq and extracts the text response.
     * 
     * @param prompt The prompt to send
     * @return The raw JSON text response from Groq
     * @throws AiServiceException if the API call fails or returns unexpected structure
     */
    private String sendRequest(AiRequest requestPayload) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("dummy_key")) {
            throw new AiServiceException("Groq API key is not configured.");
        }

        try {
            AiResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(AiResponse.class);

            if (response == null) {
                throw new AiServiceException("Received empty response from Groq API");
            }

            String text = response.extractText();
            if (text == null || text.isBlank()) {
                log.warn("Groq returned a response but extraction yielded null/empty. Full response: {}", response);
                throw new AiServiceException("Failed to extract text from Groq response");
            }

            return text;

        } catch (RestClientException ex) {
            log.error("Network or protocol error calling Groq API: {}", ex.getMessage());
            throw new AiServiceException("Failed to communicate with Groq API", ex);
        } catch (Exception ex) {
            log.error("Unexpected error parsing Groq response: {}", ex.getMessage());
            throw new AiServiceException("An unexpected error occurred while processing AI request", ex);
        }
    }

    public String getRecommendation(String prompt) {
        return sendRequest(AiRequest.forTextPrompt(model, prompt));
    }

    public String analyzeImage(String mimeType, String base64Data, String prompt) {
        String visionModel = "llama-3.2-11b-vision-preview";
        return sendRequest(AiRequest.forImagePrompt(visionModel, prompt, mimeType, base64Data));
    }

    public String generateLearningInsightsExplanation(int score, java.math.BigDecimal median, com.aitip.dto.SmartTipFeedbackDirection direction, com.aitip.dto.LearningStrength strength, com.aitip.dto.PersonalizationEffect effect) {
        String prompt = String.format("Generate a short, concise, natural-language explanation for why this user's tipping behavior has a Generosity Score of %d out of 100, where their median tip is %s. " +
                "Their feedback direction is %s, learning strength is %s, and the expected personalization effect is %s. " +
                "Output ONLY valid JSON matching this schema: {\"insightExplanation\": \"<explanation string>\"}", 
                score, median, direction, strength, effect);
        
        return sendRequest(AiRequest.forTextPrompt(model, prompt));
    }

    public String generateSimulationExplanation(java.math.BigDecimal val1, java.math.BigDecimal val2, java.math.BigDecimal val3, String s1, String s2, String s3) {
        String prompt = String.format("Generate a short, concise JSON string analyzing three what-if tipping scenarios based on historical data. " +
                "Scenario 1 value: %s (%s). Scenario 2 value: %s (%s). Scenario 3 value: %s (%s). " +
                "Output ONLY valid JSON matching this schema: {\"simulationExplanation\": \"<explanation string>\"}",
                val1, s1, val2, s2, val3, s3);
                
        return sendRequest(AiRequest.forTextPrompt(model, prompt));
    }
}
