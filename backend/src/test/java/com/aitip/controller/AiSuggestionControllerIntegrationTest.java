package com.aitip.controller;

import com.aitip.dto.AiSuggestionRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import com.aitip.service.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiSuggestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;
    
    // We mock AiProvider to avoid actual API calls during CI/tests
    @MockBean
    private AiProvider AiProvider;

    private String token;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        
        token = "Bearer " + jwtTokenProvider.generateToken(user.getEmail());
    }

    @Test
    void testAiRecommendationDoesNotSaveToDatabase() throws Exception {
        // Assert starting state
        assertEquals(0, tipRepository.count());

        // Setup mock response
        String validJson = """
                {
                  "recommendedPercentage": 15,
                  "minimumPercentage": 10,
                  "maximumPercentage": 20,
                  "reason": "Standard tip."
                }
                """;
        Mockito.when(AiProvider.getRecommendation(anyString())).thenReturn(validJson);

        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("50.00"), "DINER", ServiceQuality.AVERAGE, "USA", "USD", null, null);

        // Perform request
        mockMvc.perform(post("/api/ai/suggest")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPercentage").value(15))
                .andExpect(jsonPath("$.tipAmount").value(7.50));

        // CRITICAL CHECK: Ensure no tip was saved to the database.
        // The AI is only a recommendation engine. Saving requires explicit user action on Day 2 endpoints.
        assertEquals(0, tipRepository.count(), "AI recommendation should NOT save a Tip record to the database");
    }

    @Test
    void testAiEndpointRequiresAuthentication() throws Exception {
        AiSuggestionRequest request = new AiSuggestionRequest(
                new BigDecimal("50.00"), "DINER", ServiceQuality.AVERAGE, "USA", "USD", null, null);

        // Perform request WITHOUT token
        mockMvc.perform(post("/api/ai/suggest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // Or Forbidden depending on exact Spring Security config
    }
}
