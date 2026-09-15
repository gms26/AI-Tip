package com.aitip.service.provider;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;
import com.aitip.service.PosProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SquarePosProvider implements PosProvider {

    private static final Logger log = LoggerFactory.getLogger(SquarePosProvider.class);

    private final RestClient restClient;
    private final String accessToken;

    public SquarePosProvider(RestClient.Builder restClientBuilder, String baseUrl, String accessToken) {
        this.accessToken = accessToken;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public PosBillResponse getBill(PosBillRequest request) {
        String orderId = request.getExternalReference();
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Square POS requires an Order ID as external reference");
        }

        log.info("Fetching Square order details for order ID: {}", orderId);

        try {
            JsonNode response = restClient.get()
                    .uri("/v2/orders/{orderId}", orderId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("order")) {
                JsonNode order = response.get("order");
                
                PosBillResponse posResponse = new PosBillResponse();
                posResponse.setBillId(order.has("id") ? order.get("id").asText() : orderId);
                
                // Square might provide location/merchant info if requested, but typically it's location_id
                if (order.has("location_id")) {
                    posResponse.setRestaurantName("Square Merchant (" + order.get("location_id").asText() + ")");
                } else {
                    posResponse.setRestaurantName(request.getRestaurantName());
                }
                
                if (order.has("net_amounts") && order.get("net_amounts").has("total_money")) {
                    JsonNode totalMoney = order.get("net_amounts").get("total_money");
                    long amountInCents = totalMoney.get("amount").asLong();
                    String currency = totalMoney.get("currency").asText();
                    
                    posResponse.setBillAmount(BigDecimal.valueOf(amountInCents, 2));
                    posResponse.setCurrency(currency);
                } else {
                    // Fallback to request if API didn't return money amounts
                    posResponse.setBillAmount(request.getBillAmount());
                    posResponse.setCurrency(request.getCurrency());
                }

                posResponse.setSource("Square POS");
                posResponse.setTimestamp(LocalDateTime.now());
                
                return posResponse;
            } else {
                throw new RuntimeException("Invalid response from Square API");
            }
        } catch (RestClientException e) {
            log.error("Square API communication failed: {}", e.getMessage());
            throw new RuntimeException("POS Provider failed to fetch bill details", e);
        }
    }
}
