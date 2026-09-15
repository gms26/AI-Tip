package com.aitip.controller;

import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipDataQualityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .name("User A")
                .email("qualitya@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenA = jwtTokenProvider.generateToken(userA.getEmail());

        userB = userRepository.save(User.builder()
                .name("User B")
                .email("qualityb@example.com")
                .password(passwordEncoder.encode("pwd"))
                .build());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());
    }

    private void createTipForUser(User user, String amount, String curr, String restaurant) {
        Tip tip = new Tip();
        tip.setUser(user);
        tip.setBillAmount(new BigDecimal("100.00"));
        tip.setTipAmount(new BigDecimal(amount));
        tip.setTipPercentage(new BigDecimal(amount));
        tip.setTotalAmount(new BigDecimal("100.00").add(new BigDecimal(amount)));
        tip.setCurrency(curr);
        tip.setRestaurantName(restaurant);
        tip.setCreatedAt(LocalDateTime.now());
        tipRepository.save(tip);
    }

    @Test
    void testJwtRequired() throws Exception {
        mockMvc.perform(get("/api/tips/data-quality"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testEmptyState() throws Exception {
        mockMvc.perform(get("/api/tips/data-quality")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTips", is(0)))
                .andExpect(jsonPath("$.cleanTips", is(0)));
    }

    @Test
    void testUserIsolation() throws Exception {
        // User A gets 1 normal tip
        createTipForUser(userA, "20", "USD", "Rest A");
        // User B gets 1 anomalous tip (Missing restaurant)
        createTipForUser(userB, "20", "USD", "");

        // User A should see 0 anomalies
        mockMvc.perform(get("/api/tips/data-quality")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalyCount", is(0)))
                .andExpect(jsonPath("$.cleanTips", is(1)));

        // User B should see 1 anomaly
        mockMvc.perform(get("/api/tips/data-quality")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalyCount", is(1)))
                .andExpect(jsonPath("$.cleanTips", is(0)));
    }

    @Test
    void testFiltering() throws Exception {
        // User A has 1 anomaly in USD, 1 anomaly in INR
        createTipForUser(userA, "20", "USD", "");
        createTipForUser(userA, "20", "INR", "");

        // All
        mockMvc.perform(get("/api/tips/data-quality")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalyCount", is(2)));

        // Filter by USD
        mockMvc.perform(get("/api/tips/data-quality")
                        .param("currency", "USD")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalies", hasSize(1)))
                .andExpect(jsonPath("$.anomalies[0].currency", is("USD")));

        // Filter by severity INFO (missing restaurant is INFO)
        mockMvc.perform(get("/api/tips/data-quality")
                        .param("severity", "INFO")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalies", hasSize(2)));

        // Filter by severity HIGH (should be 0)
        mockMvc.perform(get("/api/tips/data-quality")
                        .param("severity", "HIGH")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalies", hasSize(0)));

        // Filter by anomaly type MISSING_RESTAURANT
        mockMvc.perform(get("/api/tips/data-quality")
                        .param("anomalyType", "MISSING_RESTAURANT")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomalies", hasSize(2)));
    }

    @Test
    void testInvalidParameters() throws Exception {
        // Invalid Enum value
        mockMvc.perform(get("/api/tips/data-quality")
                        .param("severity", "INVALID_SEVERITY")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }
}
