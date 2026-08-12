package com.aitip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Tip Assistant — Application Entry Point.
 *
 * <p><b>Purpose:</b> Bootstraps the Spring Boot application context.</p>
 *
 * <p><b>Why @SpringBootApplication?</b>
 * It combines @Configuration, @EnableAutoConfiguration, and @ComponentScan.
 * Spring Boot auto-configures DataSource, JPA, Security, and Flyway
 * based on the classpath dependencies and application.yml.</p>
 */
@SpringBootApplication
public class AiTipApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTipApplication.class, args);
    }
}
