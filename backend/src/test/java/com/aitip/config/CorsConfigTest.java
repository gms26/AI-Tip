package com.aitip.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.cors.frontend-url=https://ai-possibletip.vercel.app"
})
public class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCorsAllowsConfiguredFrontendUrl() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", "https://ai-possibletip.vercel.app")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type, baggage, sentry-trace"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://ai-possibletip.vercel.app"));
    }

    @Test
    public void testCorsAllowsLocalhost() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    public void testCorsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://evil-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                // Spring security will reject the preflight or CORS filter will deny it.
                // Usually CORS filter doesn't return 403 directly, it just doesn't attach the Allow-Origin header.
                // Or Spring Security rejects it. Let's expect either 401/403 or missing CORS header.
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
