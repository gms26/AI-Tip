package com.aitip.service.provider;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

public class GooglePlacesRestaurantProvider implements RestaurantProvider {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesRestaurantProvider.class);

    private final RestClient restClient;
    private final String apiKey;

    public GooglePlacesRestaurantProvider(RestClient.Builder restClientBuilder, String baseUrl, String apiKey) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public String fetchRestaurantContext(String restaurantName, String location) {
        if (restaurantName == null || restaurantName.trim().isEmpty()) {
            return null;
        }

        String query = restaurantName;
        if (location != null && !location.trim().isEmpty()) {
            query += " " + location;
        }

        log.info("Fetching Google Places metadata for: {}", query);

        Map<String, String> body = new HashMap<>();
        body.put("textQuery", query);

        try {
            JsonNode response = restClient.post()
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.priceLevel,places.rating,places.types")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("places") && response.get("places").isArray() && response.get("places").size() > 0) {
                JsonNode place = response.get("places").get(0);
                
                StringBuilder contextBuilder = new StringBuilder();
                if (place.has("displayName") && place.get("displayName").has("text")) {
                    contextBuilder.append("Name: ").append(place.get("displayName").get("text").asText()).append(". ");
                }
                if (place.has("formattedAddress")) {
                    contextBuilder.append("Location: ").append(place.get("formattedAddress").asText()).append(". ");
                }
                if (place.has("priceLevel")) {
                    contextBuilder.append("Price Level: ").append(place.get("priceLevel").asText()).append(". ");
                }
                if (place.has("rating")) {
                    contextBuilder.append("Rating: ").append(place.get("rating").asText()).append(" stars. ");
                }
                
                return contextBuilder.toString().trim();
            }
            return null;
        } catch (RestClientException e) {
            log.error("Google Places API communication failed: {}", e.getMessage());
            return null; // Gracefully fallback to no external context
        }
    }
}
