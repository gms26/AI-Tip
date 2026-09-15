package com.aitip.controller;

import com.aitip.dto.ReceiptAnalysisResponse;
import com.aitip.exception.ReceiptOcrException;
import com.aitip.exception.ReceiptOcrUnavailableException;
import com.aitip.service.ReceiptOcrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReceiptControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReceiptOcrService receiptOcrService;

    @Test
    void analyzeReceipt_Unauthenticated_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpeg", "image/jpeg", "dummy".getBytes());

        mockMvc.perform(multipart("/api/receipts/analyze")
                .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser("test@example.com")
    void analyzeReceipt_AuthenticatedValidRequest_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpeg", "image/jpeg", "dummy".getBytes());
        ReceiptAnalysisResponse mockResponse = new ReceiptAnalysisResponse(new BigDecimal("12.50"), "Test Rest", "USD");

        when(receiptOcrService.analyzeReceipt(any())).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/receipts/analyze")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billAmount").value(12.50))
                .andExpect(jsonPath("$.restaurantName").value("Test Rest"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @WithMockUser("test@example.com")
    void analyzeReceipt_OcrException_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", "dummy".getBytes());

        when(receiptOcrService.analyzeReceipt(any())).thenThrow(new ReceiptOcrException("Unsupported file type. Only JPEG, PNG, and WebP are allowed."));

        mockMvc.perform(multipart("/api/receipts/analyze")
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type. Only JPEG, PNG, and WebP are allowed."));
    }

    @Test
    @WithMockUser("test@example.com")
    void analyzeReceipt_UnavailableException_Returns503() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());

        when(receiptOcrService.analyzeReceipt(any())).thenThrow(new ReceiptOcrUnavailableException("Receipt analysis is temporarily unavailable."));

        mockMvc.perform(multipart("/api/receipts/analyze")
                .file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Receipt analysis is temporarily unavailable."));
    }
}
