package com.aitip.service;

import com.aitip.dto.CurrencyConversionRequest;
import com.aitip.dto.CurrencyConversionResponse;
import com.aitip.exception.CurrencyServiceException;
import com.aitip.service.provider.ExchangeRateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CurrencyConversionServiceTest {

    private ExchangeRateProvider mockProvider;
    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        mockProvider = mock(ExchangeRateProvider.class);
        when(mockProvider.getProviderName()).thenReturn("MockProvider");
        service = new CurrencyConversionService(mockProvider);
    }

    @Test
    void convert_ShouldUseProviderAndRoundCorrectly_ForStandardCurrency() {
        // Arrange
        when(mockProvider.getExchangeRate("USD", "INR")).thenReturn(new BigDecimal("83.50"));
        CurrencyConversionRequest req = new CurrencyConversionRequest(
                new BigDecimal("85.00"), "USD", "INR"
        );

        // Act
        CurrencyConversionResponse response = service.convert(req);

        // Assert
        assertThat(response.originalAmount()).isEqualByComparingTo("85.00");
        assertThat(response.convertedAmount()).isEqualByComparingTo("7097.50");
        assertThat(response.exchangeRate()).isEqualByComparingTo("83.50");
        assertThat(response.sourceCurrency()).isEqualTo("USD");
        assertThat(response.targetCurrency()).isEqualTo("INR");
        assertThat(response.provider()).isEqualTo("MockProvider");
        verify(mockProvider).getExchangeRate("USD", "INR");
    }

    @Test
    void convert_ShouldNormalizeCase() {
        // Arrange
        when(mockProvider.getExchangeRate("USD", "EUR")).thenReturn(new BigDecimal("0.92"));
        CurrencyConversionRequest req = new CurrencyConversionRequest(
                new BigDecimal("100"), "usd", "eUr" // mixed case
        );

        // Act
        CurrencyConversionResponse response = service.convert(req);

        // Assert
        assertThat(response.sourceCurrency()).isEqualTo("USD");
        assertThat(response.targetCurrency()).isEqualTo("EUR");
        verify(mockProvider).getExchangeRate("USD", "EUR");
    }

    @Test
    void convert_ShouldUseSameCurrencyFastPath_WithoutCallingProvider() {
        // Arrange
        CurrencyConversionRequest req = new CurrencyConversionRequest(
                new BigDecimal("50.00"), "USD", "USD"
        );

        // Act
        CurrencyConversionResponse response = service.convert(req);

        // Assert
        assertThat(response.originalAmount()).isEqualByComparingTo("50.00");
        assertThat(response.convertedAmount()).isEqualByComparingTo("50.00");
        assertThat(response.exchangeRate()).isEqualByComparingTo("1.0");
        verify(mockProvider, never()).getExchangeRate(anyString(), anyString());
    }

    @Test
    void convert_ShouldRoundToZeroDecimals_ForJPY() {
        // Arrange
        when(mockProvider.getExchangeRate("USD", "JPY")).thenReturn(new BigDecimal("149.33"));
        CurrencyConversionRequest req = new CurrencyConversionRequest(
                new BigDecimal("10.00"), "USD", "JPY"
        );

        // Act
        CurrencyConversionResponse response = service.convert(req);

        // Assert: 10 * 149.33 = 1493.3 -> rounds to 1493 for JPY
        assertThat(response.convertedAmount()).isEqualByComparingTo("1493");
        // Verify scale is 0
        assertThat(response.convertedAmount().scale()).isEqualTo(0);
    }

    @Test
    void convert_ShouldThrowIllegalArgumentException_ForUnsupportedCurrency() {
        // Arrange
        CurrencyConversionRequest req = new CurrencyConversionRequest(
                new BigDecimal("10.00"), "XYZ", "INR"
        );

        // Act & Assert
        assertThatThrownBy(() -> service.convert(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported or invalid currency code");
    }
    
    @Test
    void convert_ShouldPropagateProviderException() {
        // Arrange
        when(mockProvider.getExchangeRate("USD", "INR")).thenThrow(new CurrencyServiceException("Timeout"));
        CurrencyConversionRequest req = new CurrencyConversionRequest(
                new BigDecimal("100"), "USD", "INR"
        );

        // Act & Assert
        assertThatThrownBy(() -> service.convert(req))
                .isInstanceOf(CurrencyServiceException.class)
                .hasMessageContaining("Timeout");
    }
}
