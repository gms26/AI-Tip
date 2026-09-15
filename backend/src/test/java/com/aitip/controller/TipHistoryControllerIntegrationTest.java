package com.aitip.controller;

import com.aitip.dto.ServiceQuality;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TipHistoryControllerIntegrationTest {

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
                .email("usera@example.com")
                .password(passwordEncoder.encode("password"))
                .build());

        userB = userRepository.save(User.builder()
                .name("User B")
                .email("userb@example.com")
                .password(passwordEncoder.encode("password"))
                .build());

        tokenA = jwtTokenProvider.generateToken(userA.getEmail());
        tokenB = jwtTokenProvider.generateToken(userB.getEmail());

        // Seed some tips for user A
        tipRepository.save(Tip.builder()
                .user(userA)
                .restaurantName("Italian Place")
                .billAmount(new BigDecimal("100.00"))
                .tipAmount(new BigDecimal("20.00"))
                .tipPercentage(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .serviceQuality(ServiceQuality.GOOD)
                .build());

        tipRepository.save(Tip.builder()
                .user(userA)
                .restaurantName("Sushi Spot")
                .billAmount(new BigDecimal("50.00"))
                .tipAmount(new BigDecimal("5.00"))
                .tipPercentage(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("55.00"))
                .currency("EUR")
                .serviceQuality(ServiceQuality.EXCELLENT)
                .build());

        // Seed some tips for user B
        tipRepository.save(Tip.builder()
                .user(userB)
                .restaurantName("Italian Place")
                .billAmount(new BigDecimal("200.00"))
                .tipAmount(new BigDecimal("30.00"))
                .tipPercentage(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("230.00"))
                .currency("USD")
                .serviceQuality(ServiceQuality.AVERAGE)
                .build());
    }

    @AfterEach
    void cleanup() {
        tipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void searchHistory_NoFilters_ReturnsAllUserATips() throws Exception {
        String body = """
                {"page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void searchHistory_UserIsolation_UserBCannotSeeUserATips() throws Exception {
        String body = """
                {"page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))) // Only User B's 1 tip
                .andExpect(jsonPath("$.content[0].billAmount", is(200.00)));
    }

    @Test
    void searchHistory_FilterRestaurant_ReturnsMatches() throws Exception {
        String body = """
                {"restaurantName":"sushi","page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].restaurantName", is("Sushi Spot")));
    }

    @Test
    void searchHistory_FilterCurrencyAndQuality_ReturnsMatches() throws Exception {
        String body = """
                {"currency":"USD","serviceQuality":"GOOD","page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].restaurantName", is("Italian Place")));
    }
    
    @Test
    void searchHistory_FilterBillAmount_ReturnsMatches() throws Exception {
        String body = """
                {"minBillAmount":60.00,"page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].billAmount", is(100.00)));
    }

    @Test
    void searchHistory_Unauthenticated_Returns401() throws Exception {
        String body = """
                {"page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchHistory_InvalidPercentageRange_Returns400() throws Exception {
        String body = """
                {"minTipPercentage":50,"maxTipPercentage":10,"page":0,"size":10}
                """;
        mockMvc.perform(post("/api/tips/history/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
