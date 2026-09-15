package com.aitip.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestaurantProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(RestaurantProviderConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.restaurant.provider", havingValue = "google")
    public RestaurantProvider googleRestaurantProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.restaurant.google.base-url}") String baseUrl,
            @Value("${app.restaurant.google.api-key}") String apiKey) {
        log.info("Configuring Real Restaurant Provider (Google Places)");
        return new GooglePlacesRestaurantProvider(restClientBuilder, baseUrl, apiKey);
    }

    @Bean
    @ConditionalOnProperty(name = "app.restaurant.provider", havingValue = "mock", matchIfMissing = true)
    public RestaurantProvider mockRestaurantProvider() {
        log.info("Configuring Mock Restaurant Provider");
        return new MockRestaurantProvider();
    }
}
