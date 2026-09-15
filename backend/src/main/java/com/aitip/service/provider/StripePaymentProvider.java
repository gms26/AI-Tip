package com.aitip.service.provider;

import com.aitip.entity.PaymentSession;
import com.aitip.service.PaymentProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

public class StripePaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProvider.class);

    private final RestClient restClient;
    private final String secretKey;

    public StripePaymentProvider(RestClient.Builder restClientBuilder, String baseUrl, String secretKey) {
        this.secretKey = secretKey;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public String createSession(PaymentSession session) {
        log.info("Creating Stripe Payment Intent for session {}", session.getId());

        // Stripe expects amount in smallest currency unit (e.g., cents for USD)
        long amountInCents = session.getAmount().multiply(new BigDecimal("100")).longValue();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("amount", String.valueOf(amountInCents));
        body.add("currency", session.getCurrency().toLowerCase());
        body.add("metadata[session_id]", session.getId().toString());
        if (session.getRestaurantName() != null) {
            body.add("description", "Tip for " + session.getRestaurantName());
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/payment_intents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("id")) {
                return response.get("id").asText();
            } else {
                throw new RuntimeException("Invalid response from Stripe API");
            }

        } catch (RestClientException e) {
            log.error("Stripe API communication failed: {}", e.getMessage());
            throw new RuntimeException("Payment Provider failed to create session", e);
        }
    }

    @Override
    public boolean checkStatus(String providerSessionId) {
        try {
            JsonNode response = restClient.get()
                    .uri("/v1/payment_intents/{id}", providerSessionId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("status")) {
                String status = response.get("status").asText();
                return "succeeded".equals(status);
            }
            return false;
        } catch (RestClientException e) {
            log.error("Stripe API communication failed during status check: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void cancelSession(String providerSessionId) {
        try {
            restClient.post()
                    .uri("/v1/payment_intents/{id}/cancel", providerSessionId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Stripe API communication failed during cancellation: {}", e.getMessage());
            throw new RuntimeException("Payment Provider failed to cancel session", e);
        }
    }
}
