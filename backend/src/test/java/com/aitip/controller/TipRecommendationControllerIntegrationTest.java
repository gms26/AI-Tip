package com.aitip.controller;

import com.aitip.dto.*;
import com.aitip.service.AiRecommendationService;
import com.aitip.service.TipRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TipRecommendationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TipRecommendationService recommendationService;

    @MockBean
    private AiRecommendationService aiRecommendationService;

    @Test
    void getRecommendations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getRecommendations_success_returns200() throws Exception {
        TipRecommendationResponse mockResponse = new TipRecommendationResponse(
                LocalDateTime.now(),
                List.of(new TipRecommendation(
                        TipRecommendationType.BUDGET_WARNING,
                        TipRecommendationPriority.HIGH,
                        "Title",
                        "Message",
                        "USD",
                        "100",
                        "Label",
                        "/action"
                )),
                "Summary",
                null
        );

        when(recommendationService.getRecommendations(eq("user@example.com"), eq(null))).thenReturn(mockResponse);
        when(aiRecommendationService.generateExplanation(any())).thenReturn("AI Explanation");

        mockMvc.perform(get("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].type").value("BUDGET_WARNING"))
                .andExpect(jsonPath("$.aiExplanation").value("AI Explanation"));
    }
}
