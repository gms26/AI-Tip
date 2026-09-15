package com.aitip.controller;

import com.aitip.dto.TipRecommendationAction;
import com.aitip.dto.TipRecommendationActionRequest;
import com.aitip.dto.TipRecommendationActionResponse;
import com.aitip.dto.TipRecommendationHistoryResponse;
import com.aitip.dto.TipRecommendationType;
import com.aitip.service.TipRecommendationActionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TipRecommendationActionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TipRecommendationActionService actionService;

    @Test
    void performAction_unauthenticated_returns401() throws Exception {
        TipRecommendationActionRequest req = new TipRecommendationActionRequest(TipRecommendationAction.REVIEWED, null);
        mockMvc.perform(post("/api/recommendations/BUDGET_WARNING/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void performAction_success_returns200() throws Exception {
        TipRecommendationActionRequest req = new TipRecommendationActionRequest(TipRecommendationAction.DISMISSED, null);
        
        TipRecommendationActionResponse mockResponse = new TipRecommendationActionResponse(
                TipRecommendationType.BUDGET_WARNING,
                "USD",
                TipRecommendationAction.DISMISSED,
                LocalDateTime.now(),
                null,
                "Success"
        );

        when(actionService.performAction(eq("user@example.com"), eq("BUDGET_WARNING"), eq("USD"), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/recommendations/BUDGET_WARNING/action?currency=USD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("DISMISSED"))
                .andExpect(jsonPath("$.recommendationType").value("BUDGET_WARNING"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getHistory_success_returns200() throws Exception {
        TipRecommendationHistoryResponse mockResponse = new TipRecommendationHistoryResponse(List.of(), 0);
        
        when(actionService.getHistory("user@example.com")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/recommendations/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
