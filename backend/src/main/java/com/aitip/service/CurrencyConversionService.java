package com.aitip.service;

import com.aitip.dto.CurrencyConversionRequest;
import com.aitip.dto.CurrencyConversionResponse;
import com.aitip.service.provider.ExchangeRateProvider;
import com.aitip.util.CurrencyValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Service orchestrating the currency conversion business logic.
 */
@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);

    private final ExchangeRateProvider exchangeRateProvider;

    public CurrencyConversionService(ExchangeRateProvider exchangeRateProvider) {
        this.exchangeRateProvider = exchangeRateProvider;
    }

    /**
     * Converts a specific amount from one currency to another.
     */
    public CurrencyConversionResponse convert(CurrencyConversionRequest request) {
        String source = CurrencyValidationUtil.normalizeAndValidate(request.sourceCurrency());
        String target = CurrencyValidationUtil.normalizeAndValidate(request.targetCurrency());

        BigDecimal exchangeRate;
        
        // Fast path for identical currencies
        if (source.equals(target)) {
            exchangeRate = BigDecimal.ONE;
        } else {
            exchangeRate = exchangeRateProvider.getExchangeRate(source, target);
        }

        BigDecimal convertedAmount = calculateConvertedAmount(request.amount(), exchangeRate, target);

        return new CurrencyConversionResponse(
                source,
                target,
                request.amount(),
                convertedAmount,
                exchangeRate,
                LocalDateTime.now(),
                exchangeRateProvider.getProviderName()
        );
    }

    /**
     * Retrieves only the exchange rate (primarily for frontend calculators to batch-convert locally).
     */
    public CurrencyConversionResponse getExchangeRateOnly(String sourceCurrency, String targetCurrency) {
        String source = CurrencyValidationUtil.normalizeAndValidate(sourceCurrency);
        String target = CurrencyValidationUtil.normalizeAndValidate(targetCurrency);

        BigDecimal exchangeRate;
        
        if (source.equals(target)) {
            exchangeRate = BigDecimal.ONE;
        } else {
            exchangeRate = exchangeRateProvider.getExchangeRate(source, target);
        }

        return new CurrencyConversionResponse(
                source,
                target,
                null,
                null,
                exchangeRate,
                LocalDateTime.now(),
                exchangeRateProvider.getProviderName()
        );
    }

    /**
     * Calculates the converted amount and scales it appropriately based on the target currency.
     */
    private BigDecimal calculateConvertedAmount(BigDecimal amount, BigDecimal exchangeRate, String targetCurrency) {
        int fractionDigits = CurrencyValidationUtil.getFractionDigits(targetCurrency);
        // Fallback to 2 if the fraction digits is -1 (pseudo-currencies)
        if (fractionDigits < 0) {
            fractionDigits = 2;
        }

        return amount.multiply(exchangeRate).setScale(fractionDigits, RoundingMode.HALF_UP);
    }
}
