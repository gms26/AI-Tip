package com.aitip.service;

import com.aitip.dto.ReceiptAnalysisResponse;
import com.aitip.exception.AiServiceException;
import com.aitip.exception.ReceiptOcrException;
import com.aitip.exception.ReceiptOcrUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReceiptOcrServiceTest {

    private AiProvider AiProvider;
    private ObjectMapper objectMapper;
    private ReceiptOcrService service;

    @BeforeEach
    void setUp() {
        AiProvider = Mockito.mock(AiProvider.class);
        objectMapper = new ObjectMapper();
        service = new ReceiptOcrService(AiProvider, objectMapper);
    }

    @Test
    void analyzeReceipt_ValidJpeg_ExtractsSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpeg", "image/jpeg", "dummy_bytes".getBytes());
        String fakeAiResponse = "{\"billAmount\": 45.67, \"restaurantName\": \"The Eatery\", \"currency\": \"USD\"}";
        when(AiProvider.analyzeImage(anyString(), eq("image/jpeg"), anyString())).thenReturn(fakeAiResponse);

        ReceiptAnalysisResponse response = service.analyzeReceipt(file);

        assertThat(response.billAmount()).isEqualTo(new BigDecimal("45.67"));
        assertThat(response.restaurantName()).isEqualTo("The Eatery");
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void analyzeReceipt_ValidPng_ExtractsSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "dummy_bytes".getBytes());
        String fakeAiResponse = "{\"billAmount\": 10.0, \"restaurantName\": \"Cafe\", \"currency\": \"EUR\"}";
        when(AiProvider.analyzeImage(anyString(), eq("image/png"), anyString())).thenReturn(fakeAiResponse);

        ReceiptAnalysisResponse response = service.analyzeReceipt(file);

        assertThat(response.billAmount()).isEqualTo(new BigDecimal("10.0"));
        assertThat(response.restaurantName()).isEqualTo("Cafe");
        assertThat(response.currency()).isEqualTo("EUR");
    }

    @Test
    void analyzeReceipt_ValidWebP_ExtractsSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", "dummy_bytes".getBytes());
        String fakeAiResponse = "{\"billAmount\": 25.5, \"restaurantName\": \"Diner\", \"currency\": \"GBP\"}";
        when(AiProvider.analyzeImage(anyString(), eq("image/webp"), anyString())).thenReturn(fakeAiResponse);

        ReceiptAnalysisResponse response = service.analyzeReceipt(file);

        assertThat(response.billAmount()).isEqualTo(new BigDecimal("25.5"));
        assertThat(response.restaurantName()).isEqualTo("Diner");
        assertThat(response.currency()).isEqualTo("GBP");
    }

    @Test
    void analyzeReceipt_FileTooLarge_Throws400() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", largeBytes);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("5 MB limit");
    }

    @Test
    void analyzeReceipt_UnsupportedMimeType_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", "dummy".getBytes());

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("Unsupported file type");
    }

    @Test
    void analyzeReceipt_EmptyFile_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("missing or empty");
    }

    @Test
    void analyzeReceipt_MarkdownWrappedJson_ExtractsSuccessfully() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "```json\n{\"billAmount\": 15.0, \"restaurantName\": \"Pizza Hut\", \"currency\": \"USD\"}\n```";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptAnalysisResponse response = service.analyzeReceipt(file);

        assertThat(response.restaurantName()).isEqualTo("Pizza Hut");
    }

    @Test
    void analyzeReceipt_MalformedJson_Throws503() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{ billAmount: 15.0, broken: yes";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrUnavailableException ex = assertThrows(ReceiptOcrUnavailableException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("temporarily unavailable");
    }

    @Test
    void analyzeReceipt_MissingBillAmount_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{\"restaurantName\": \"Pizza Hut\", \"currency\": \"USD\"}";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("valid positive bill amount");
    }

    @Test
    void analyzeReceipt_MissingRestaurantName_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{\"billAmount\": 15.0, \"currency\": \"USD\"}";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("restaurant name");
    }

    @Test
    void analyzeReceipt_MissingCurrency_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{\"billAmount\": 15.0, \"restaurantName\": \"Pizza Hut\"}";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("valid currency");
    }

    @Test
    void analyzeReceipt_InvalidCurrency_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{\"billAmount\": 15.0, \"restaurantName\": \"Pizza Hut\", \"currency\": \"XYZ\"}";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("valid currency");
    }

    @Test
    void analyzeReceipt_NegativeBillAmount_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{\"billAmount\": -5.0, \"restaurantName\": \"Pizza Hut\", \"currency\": \"USD\"}";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("positive bill amount");
    }

    @Test
    void analyzeReceipt_ZeroBillAmount_Throws400() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        String fakeResponse = "{\"billAmount\": 0.0, \"restaurantName\": \"Pizza Hut\", \"currency\": \"USD\"}";
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenReturn(fakeResponse);

        ReceiptOcrException ex = assertThrows(ReceiptOcrException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("positive bill amount");
    }

    @Test
    void analyzeReceipt_GroqTimeout_Throws503() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "dummy".getBytes());
        when(AiProvider.analyzeImage(anyString(), anyString(), anyString())).thenThrow(new AiServiceException("Timeout"));

        ReceiptOcrUnavailableException ex = assertThrows(ReceiptOcrUnavailableException.class, () -> service.analyzeReceipt(file));
        assertThat(ex.getMessage()).contains("temporarily unavailable");
    }
}
