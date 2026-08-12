package com.aitip.service.provider;

import com.aitip.exception.CurrencyServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Implementation of ExchangeRateProvider using the Frankfurter API (ECB reference rates).
 */
@Component
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(FrankfurterExchangeRateProvider.class);
    private static final String PROVIDER_NAME = "Frankfurter (ECB Reference)";

    private final RestClient restClient;

    public FrankfurterExchangeRateProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.currency.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public BigDecimal getExchangeRate(String sourceCurrency, String targetCurrency) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/latest")
                            .queryParam("from", sourceCurrency)
                            .queryParam("to", targetCurrency)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new CurrencyServiceException("Received empty response from Frankfurter API");
            }

            // Verify that the response actually contains the rates node (handles malformed 200 OK responses)
            JsonNode ratesNode = response.get("rates");
            if (ratesNode == null || ratesNode.isNull()) {
                throw new CurrencyServiceException("Malformed response: 'rates' node is missing");
            }

            JsonNode targetNode = ratesNode.get(targetCurrency);
            if (targetNode == null || targetNode.isNull()) {
                throw new CurrencyServiceException("Malformed response: target currency rate missing");
            }

            return new BigDecimal(targetNode.asText());

        } catch (RestClientException ex) {
            log.error("Failed to fetch exchange rate from Frankfurter API: {}", ex.getMessage());
            throw new CurrencyServiceException("Failed to connect to currency provider", ex);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
