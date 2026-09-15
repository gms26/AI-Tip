package com.aitip.service.provider;

import com.aitip.service.MockPaymentProvider;
import com.aitip.service.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
    public PaymentProvider stripePaymentProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.payment.stripe.base-url}") String baseUrl,
            @Value("${app.payment.stripe.secret-key}") String secretKey) {
        log.info("Configuring Real Payment Provider (Stripe Sandbox)");
        return new StripePaymentProvider(restClientBuilder, baseUrl, secretKey);
    }

    @Bean
    @ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
    public PaymentProvider mockPaymentProvider() {
        log.info("Configuring Mock Payment Provider");
        return new MockPaymentProvider();
    }
}
