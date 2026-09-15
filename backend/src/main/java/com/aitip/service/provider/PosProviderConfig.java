package com.aitip.service.provider;

import com.aitip.service.MockPosProvider;
import com.aitip.service.PosProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PosProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(PosProviderConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.pos.provider", havingValue = "square")
    public PosProvider squarePosProvider(
            RestClient.Builder restClientBuilder,
            @Value("${app.pos.square.base-url}") String baseUrl,
            @Value("${app.pos.square.access-token}") String accessToken) {
        log.info("Configuring Real POS Provider (Square Sandbox)");
        return new SquarePosProvider(restClientBuilder, baseUrl, accessToken);
    }

    @Bean
    @ConditionalOnProperty(name = "app.pos.provider", havingValue = "mock", matchIfMissing = true)
    public PosProvider mockPosProvider() {
        log.info("Configuring Mock POS Provider");
        return new MockPosProvider();
    }
}
