package com.aitip.service;

import com.aitip.dto.AiSuggestionRequest;
import com.aitip.dto.AiSuggestionResponse;
import com.aitip.dto.ServiceQuality;
import com.aitip.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AiSuggestionServiceTest {

    private AiSuggestionService aiSuggestionService;
    private GeminiService geminiService;
    
    @BeforeEach
    void setUp() {
        geminiService = Mockito.mock(GeminiService.class);
        PromptBuilder promptBuilder = new PromptBuilder();
        TipCalculationService tipCalculationService = new TipCalculationService();
        ObjectMapper objectMapper = new ObjectMapper();
        
        aiSuggestionService = new AiSuggestionService(
                promptBuilder, geminiService, tipCalculationService, objectMapper);
    }

    @Test
    void testSuccessfulRecommendation() {
        // Mock a valid Gemini JSON response (Day 7: confidence removed from AI)
        String validJson = """
                {
                  "recommendedPercentage": 20,
                  "minimumPercentage": 18,
                  "maximumPercentage": 22,
                  "reason": "Great service justifies a higher tip."
                }
                """;
        when(geminiService.getRecommendation(anyString())).thenReturn(validJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("100.00"), "FINE_DINING", ServiceQuality.EXCELLENT, "USA", "USD", null, null);

        AiSuggestionResponse response = aiSuggestionService.getRecommendation(request);

        // Assert AI extraction
        assertEquals(new BigDecimal("20"), response.recommendedPercentage());
        assertEquals("Great service justifies a higher tip.", response.reason());
        
        // Assert Day 7 Default logic (null context)
        assertEquals("LOW", response.confidence());
        assertEquals(com.aitip.dto.PersonalizationSource.NONE, response.personalizationSource());
        
        // Assert mathematical calculations (100 * 20% = 20 tip, 120 total)
        assertEquals(new BigDecimal("20.00"), response.tipAmount());
        assertEquals(new BigDecimal("120.00"), response.totalAmount());
    }

    @Test
    void testSuccessfulRecommendationWithContext() {
        String validJson = """
                {
                  "recommendedPercentage": 20,
                  "minimumPercentage": 18,
                  "maximumPercentage": 22,
                  "reason": "Based on your history."
                }
                """;
        when(geminiService.getRecommendation(anyString())).thenReturn(validJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("100.00"), "FINE_DINING", ServiceQuality.EXCELLENT, "USA", "USD", null, null);

        com.aitip.dto.PersonalizationContext context = new com.aitip.dto.PersonalizationContext(
                com.aitip.dto.PersonalizationSource.RESTAURANT_AND_SERVICE, "HIGH", 10,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        AiSuggestionResponse response = aiSuggestionService.getRecommendation(request, context);

        assertEquals("HIGH", response.confidence());
        assertEquals(com.aitip.dto.PersonalizationSource.RESTAURANT_AND_SERVICE, response.personalizationSource());
    }

    @Test
    void testMalformedJsonThrowsException() {
        // Missing comma, invalid JSON
        String invalidJson = """
                {
                  "recommendedPercentage": 20
                  "reason": "Missing comma before this"
                }
                """;
        when(geminiService.getRecommendation(anyString())).thenReturn(invalidJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("100.00"), "CAFE", ServiceQuality.GOOD, "USA", "USD", null, null);

        assertThrows(AiServiceException.class, () -> aiSuggestionService.getRecommendation(request));
    }

    @Test
    void testMissingFieldsThrowsException() {
        // Valid JSON but missing required schema fields
        String incompleteJson = """
                {
                  "recommendedPercentage": 20
                }
                """;
        when(geminiService.getRecommendation(anyString())).thenReturn(incompleteJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("100.00"), "CAFE", ServiceQuality.GOOD, "USA", "USD", null, null);

        assertThrows(AiServiceException.class, () -> aiSuggestionService.getRecommendation(request));
    }

    @Test
    void testOutOfBoundsPercentageThrowsException() {
        // AI hallucinates a 150% tip
        String hallucinatedJson = """
                {
                  "recommendedPercentage": 150,
                  "minimumPercentage": 10,
                  "maximumPercentage": 200,
                  "reason": "You should tip a lot."
                }
                """;
        when(geminiService.getRecommendation(anyString())).thenReturn(hallucinatedJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("100.00"), "CAFE", ServiceQuality.GOOD, "USA", "USD", null, null);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> aiSuggestionService.getRecommendation(request));
        assertTrue(ex.getMessage().contains("out of bounds"));
    }

    @Test
    void testIllogicalPercentageRangeThrowsException() {
        // Min is higher than Max
        String illogicalJson = """
                {
                  "recommendedPercentage": 15,
                  "minimumPercentage": 20,
                  "maximumPercentage": 10,
                  "reason": "Math is hard."
                }
                """;
        when(geminiService.getRecommendation(anyString())).thenReturn(illogicalJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("100.00"), "CAFE", ServiceQuality.GOOD, "USA", "USD", null, null);

        AiServiceException ex = assertThrows(AiServiceException.class, () -> aiSuggestionService.getRecommendation(request));
        assertTrue(ex.getMessage().contains("illogical"));
    }
}
