package com.aitip.service.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GooglePlacesRestaurantProvider.class)
class GooglePlacesRestaurantProviderTest {

    private MockRestServiceServer mockServer;
    private GooglePlacesRestaurantProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new GooglePlacesRestaurantProvider(builder, "https://places.googleapis.com/v1/places:searchText", "test_key");
    }

    @Test
    void fetchRestaurantContext_ShouldReturnContext_WhenSuccessful() {
        String mockResponse = """
                {
                  "places": [
                    {
                      "displayName": {
                        "text": "Famous Italian Place"
                      },
                      "formattedAddress": "123 Main St, City, Country",
                      "priceLevel": "PRICE_LEVEL_MODERATE",
                      "rating": 4.5
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String context = provider.fetchRestaurantContext("Famous Italian", "City");

        assertThat(context).contains("Name: Famous Italian Place.");
        assertThat(context).contains("Location: 123 Main St, City, Country.");
        assertThat(context).contains("Price Level: PRICE_LEVEL_MODERATE.");
        assertThat(context).contains("Rating: 4.5 stars.");

        mockServer.verify();
    }

    @Test
    void fetchRestaurantContext_ShouldReturnNull_WhenEmptyResult() {
        String mockResponse = """
                {
                  "places": []
                }
                """;

        mockServer.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String context = provider.fetchRestaurantContext("Unknown Place", "");

        assertThat(context).isNull();
        mockServer.verify();
    }
}
