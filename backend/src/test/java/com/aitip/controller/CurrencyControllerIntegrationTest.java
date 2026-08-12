package com.aitip.controller;

import com.aitip.dto.CurrencyConversionRequest;
import com.aitip.dto.CurrencyConversionResponse;
import com.aitip.exception.CurrencyServiceException;
import com.aitip.service.CurrencyConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CurrencyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CurrencyConversionService currencyConversionService;

    @Test
    void convert_ShouldRequireJwt() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("100"), "USD", "INR"
        );

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void convert_ShouldReturnConvertedAmount_WhenValid() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("100"), "USD", "INR"
        );

        CurrencyConversionResponse mockResponse = new CurrencyConversionResponse(
                "USD", "INR", new BigDecimal("100"), new BigDecimal("8300.00"),
                new BigDecimal("83.00"), LocalDateTime.now(), "MockProvider"
        );

        when(currencyConversionService.convert(any(CurrencyConversionRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount").value("8300.0"))
                .andExpect(jsonPath("$.provider").value("MockProvider"));
    }

    @Test
    @WithMockUser
    void convert_ShouldReturn400_WhenAmountIsZero() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("0"), "USD", "INR" // 0 is invalid
        );

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @WithMockUser
    void convert_ShouldReturn503_WhenProviderFails() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("100"), "USD", "INR"
        );

        when(currencyConversionService.convert(any(CurrencyConversionRequest.class)))
                .thenThrow(new CurrencyServiceException("Provider timeout"));

        mockMvc.perform(post("/api/currency/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Currency conversion is temporarily unavailable."));
    }

    @Test
    @WithMockUser
    void getRate_ShouldReturnExchangeRate_WhenValid() throws Exception {
        CurrencyConversionResponse mockResponse = new CurrencyConversionResponse(
                "USD", "EUR", null, null,
                new BigDecimal("0.92"), LocalDateTime.now(), "MockProvider"
        );

        when(currencyConversionService.getExchangeRateOnly(anyString(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/currency/rate")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeRate").value("0.92"))
                .andExpect(jsonPath("$.provider").value("MockProvider"));
    }
}
