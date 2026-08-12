package com.aitip.service.provider;

import com.aitip.exception.CurrencyServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(FrankfurterExchangeRateProvider.class)
class FrankfurterExchangeRateProviderTest {

    @Autowired
    private MockRestServiceServer mockServer;

    private FrankfurterExchangeRateProvider provider;

    @BeforeEach
    void setUp() {
        // Build a fresh RestClient manually since we aren't autowiring the full context
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new FrankfurterExchangeRateProvider(builder, "https://api.frankfurter.dev");
    }

    @Test
    void getExchangeRate_ShouldReturnRate_WhenResponseIsValid() {
        String mockResponse = """
                {
                  "amount": 1.0,
                  "base": "USD",
                  "date": "2023-11-23",
                  "rates": {
                    "INR": 83.35
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.frankfurter.dev/latest?from=USD&to=INR"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        BigDecimal rate = provider.getExchangeRate("USD", "INR");

        assertThat(rate).isEqualByComparingTo("83.35");
        mockServer.verify();
    }

    @Test
    void getExchangeRate_ShouldThrowException_WhenRatesMissingFrom200Response() {
        String malformedResponse = """
                {
                  "amount": 1.0,
                  "base": "USD",
                  "date": "2023-11-23"
                }
                """;

        mockServer.expect(requestTo("https://api.frankfurter.dev/latest?from=USD&to=INR"))
                .andRespond(withSuccess(malformedResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.getExchangeRate("USD", "INR"))
                .isInstanceOf(CurrencyServiceException.class)
                .hasMessageContaining("Malformed response: 'rates' node is missing");
        
        mockServer.verify();
    }
}
