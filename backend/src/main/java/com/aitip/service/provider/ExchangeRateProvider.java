package com.aitip.service.provider;

import java.math.BigDecimal;

/**
 * Interface for providing exchange rates.
 * Abstracts the external API so we aren't coupled to a single vendor.
 */
public interface ExchangeRateProvider {

    /**
     * Retrieves the exchange rate to multiply the source amount by to get the target amount.
     *
     * @param sourceCurrency The 3-letter ISO 4217 source currency code (e.g. "USD")
     * @param targetCurrency The 3-letter ISO 4217 target currency code (e.g. "INR")
     * @return The exchange rate
     * @throws com.aitip.exception.CurrencyServiceException if the provider fails to return a valid rate
     */
    BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency);

    /**
     * Gets the name of the provider.
     */
    String getProviderName();
}
