package com.aitip.controller;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.SmartTipSimulationRequest;
import com.aitip.dto.SmartTipSimulationResponse;
import com.aitip.dto.SmartTipSimulationState;
import com.aitip.dto.SmartTipSimulationDifference;
import com.aitip.service.SmartTipSimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SmartTipSimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SmartTipSimulationService simulationService;

    private SmartTipSimulationRequest validRequest;
    private SmartTipSimulationResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = new SmartTipSimulationRequest(
                "USD",
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                "Test Restaurant",
                ServiceQuality.EXCELLENT
        );

        SmartTipSimulationState current = new SmartTipSimulationState(
                new BigDecimal("100.00"), new BigDecimal("15.00"), new BigDecimal("15.00"), new BigDecimal("115.00")
        );
        SmartTipSimulationState simulated = new SmartTipSimulationState(
                new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("120.00")
        );
        SmartTipSimulationDifference difference = new SmartTipSimulationDifference(
                new BigDecimal("5.00"), new BigDecimal("5.00"), new BigDecimal("5.00")
        );

        validResponse = new SmartTipSimulationResponse(
                "USD",
                current,
                simulated,
                difference,
                "Leaves $5.00 remaining",
                "Impacts 1 goal",
                "This is more generous than the recommendation by 5.00 USD.",
                "Summary."
        );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void simulate_ValidRequest_ReturnsOk() throws Exception {
        when(simulationService.simulate(eq("test@example.com"), any(SmartTipSimulationRequest.class)))
                .thenReturn(validResponse);

        mockMvc.perform(post("/api/smart-tip/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.current.tipPercentage").value(15.0))
                .andExpect(jsonPath("$.simulated.tipPercentage").value(20.0))
                .andExpect(jsonPath("$.difference.tipAmountDifference").value(5.0));
    }

    @Test
    void simulate_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/smart-tip/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void simulate_MissingCurrency_ReturnsBadRequest() throws Exception {
        SmartTipSimulationRequest badRequest = new SmartTipSimulationRequest(
                "", new BigDecimal("100.00"), new BigDecimal("20.00"), null, null
        );

        mockMvc.perform(post("/api/smart-tip/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void simulate_NegativeTip_ReturnsBadRequest() throws Exception {
        SmartTipSimulationRequest badRequest = new SmartTipSimulationRequest(
                "USD", new BigDecimal("100.00"), new BigDecimal("-5.00"), null, null
        );

        mockMvc.perform(post("/api/smart-tip/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }
}
