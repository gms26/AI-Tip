package com.aitip.service.provider;

import com.aitip.entity.PaymentSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(StripePaymentProvider.class)
class StripePaymentProviderTest {

    private MockRestServiceServer mockServer;
    private StripePaymentProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new StripePaymentProvider(builder, "https://api.stripe.com", "test_key");
    }

    @Test
    void createSession_ShouldReturnId_WhenSuccessful() {
        PaymentSession session = new PaymentSession();
        session.setId(UUID.randomUUID());
        session.setAmount(new BigDecimal("10.50"));
        session.setCurrency("USD");
        session.setRestaurantName("Test Place");

        String mockResponse = """
                {
                  "id": "pi_12345",
                  "object": "payment_intent",
                  "amount": 1050,
                  "currency": "usd"
                }
                """;

        mockServer.expect(requestTo("https://api.stripe.com/v1/payment_intents"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        String sessionId = provider.createSession(session);

        assertThat(sessionId).isEqualTo("pi_12345");
        mockServer.verify();
    }

    @Test
    void createSession_ShouldThrowException_WhenResponseIsInvalid() {
        PaymentSession session = new PaymentSession();
        session.setId(UUID.randomUUID());
        session.setAmount(new BigDecimal("10.50"));
        session.setCurrency("USD");

        String mockResponse = """
                {
                  "error": "Invalid API Key"
                }
                """;

        mockServer.expect(requestTo("https://api.stripe.com/v1/payment_intents"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.createSession(session))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid response from Stripe API");

        mockServer.verify();
    }
}
