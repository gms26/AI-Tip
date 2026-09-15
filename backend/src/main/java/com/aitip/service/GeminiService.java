package com.aitip.service;

import com.aitip.dto.GeminiRequest;
import com.aitip.dto.GeminiResponse;
import com.aitip.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Handles communication with the Google Gemini API.
 * 
 * <p>Uses Spring's native RestClient. Responsibilities are limited to HTTP 
 * transmission, timeouts, and returning the raw string response. 
 * Does NOT contain business logic.</p>
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiService(
            RestClient.Builder restClientBuilder,
            @Value("${app.gemini.base-url}") String baseUrl,
            @Value("${app.gemini.api-key}") String apiKey,
            @Value("${app.gemini.model}") String model) {
            
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Sends the formatted prompt to Gemini and extracts the text response.
     * 
     * @param prompt The prompt to send
     * @return The raw JSON text response from Gemini
     * @throws AiServiceException if the API call fails or returns unexpected structure
     */
    public String getRecommendation(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException("Gemini API key is not configured.");
        }

        try {
            GeminiRequest requestPayload = GeminiRequest.forTextPrompt(prompt);
            
            // Expected URL: {baseUrl}/{model}:generateContent?key={apiKey}
            GeminiResponse response = restClient.post()
                    .uri("/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null) {
                throw new AiServiceException("Received empty response from Gemini API");
            }

            String text = response.extractText();
            if (text == null || text.isBlank()) {
                log.warn("Gemini returned a response but extraction yielded null/empty. Full response: {}", response);
                throw new AiServiceException("Failed to extract text from Gemini response");
            }

            return text;

        } catch (RestClientException ex) {
            log.error("Network or protocol error calling Gemini API: {}", ex.getMessage());
            throw new AiServiceException("Failed to communicate with Gemini API", ex);
        } catch (Exception ex) {
            log.error("Unexpected error parsing Gemini response: {}", ex.getMessage());
            throw new AiServiceException("An unexpected error occurred while processing AI request", ex);
        }
    }

    public String analyzeImage(String mimeType, String base64Data, String prompt) {
        return "{}";
    }

    public String generateLearningInsightsExplanation(int score, java.math.BigDecimal median, com.aitip.dto.SmartTipFeedbackDirection direction, com.aitip.dto.LearningStrength strength, com.aitip.dto.PersonalizationEffect effect) {
        return "{}";
    }

    public String generateSimulationExplanation(java.math.BigDecimal val1, java.math.BigDecimal val2, java.math.BigDecimal val3, String s1, String s2, String s3) {
        return "{}";
    }
}
