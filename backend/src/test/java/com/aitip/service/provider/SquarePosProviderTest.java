package com.aitip.service.provider;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(SquarePosProvider.class)
class SquarePosProviderTest {

    private MockRestServiceServer mockServer;
    private SquarePosProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new SquarePosProvider(builder, "https://connect.squareupsandbox.com", "test_token");
    }

    @Test
    void getBill_ShouldReturnParsedBill_WhenSuccessful() {
        PosBillRequest request = new PosBillRequest();
        request.setExternalReference("order_123");
        request.setRestaurantName("Fallback Restaurant");

        String mockResponse = """
                {
                  "order": {
                    "id": "order_123",
                    "location_id": "L123",
                    "net_amounts": {
                      "total_money": {
                        "amount": 4500,
                        "currency": "USD"
                      }
                    }
                  }
                }
                """;

        mockServer.expect(requestTo("https://connect.squareupsandbox.com/v2/orders/order_123"))
                .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        PosBillResponse response = provider.getBill(request);

        assertThat(response.getBillId()).isEqualTo("order_123");
        assertThat(response.getBillAmount()).isEqualByComparingTo("45.00");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getRestaurantName()).contains("Square Merchant");

        mockServer.verify();
    }
}
