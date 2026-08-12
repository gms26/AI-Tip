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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GenerosityScoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        userA = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userA);

        userB = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(userB);

        tokenA = "Bearer " + jwtTokenProvider.generateToken(userA.getEmail());
        tokenB = "Bearer " + jwtTokenProvider.generateToken(userB.getEmail());
    }

    private void createTipForUser(User user, double percentage) {
        Tip tip = Tip.builder()
                .user(user)
                .billAmount(new BigDecimal("100.00"))
                .tipPercentage(BigDecimal.valueOf(percentage))
                .tipAmount(BigDecimal.valueOf(percentage))
                .totalAmount(BigDecimal.valueOf(100 + percentage))
                .currency("USD")
                .restaurantName("Test Rest")
                .createdAt(LocalDateTime.now())
                .build();
        tipRepository.save(tip);
    }

    // 1. JWT required -> 401
    @Test
    void testGetScoreWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/generosity/score"))
                .andExpect(status().isUnauthorized());
    }

    // 2. Authenticated user -> 200
    // 3. New user -> empty state
    @Test
    void testGetScoreForNewUserReturnsEmptyState() throws Exception {
        mockMvc.perform(get("/api/generosity/score")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.confidence").value("LOW"))
                .andExpect(jsonPath("$.totalTips").value(0))
                .andExpect(jsonPath("$.message").value("Not enough tipping history to calculate a score."));
    }

    // 4. User A cannot see User B's statistics
    // 5. Correct score returned
    // 6. Correct category returned
    // 7. Correct confidence returned
    @Test
    void testUserIsolationAndScoreCalculation() throws Exception {
        // User A tips 20% x 3
        createTipForUser(userA, 20.0);
        createTipForUser(userA, 20.0);
        createTipForUser(userA, 20.0);

        // User B tips 5% x 3
        createTipForUser(userB, 5.0);
        createTipForUser(userB, 5.0);
        createTipForUser(userB, 5.0);

        // Fetch User A score (Median 20 -> 80/100, VERY_GENEROUS, MEDIUM confidence)
        mockMvc.perform(get("/api/generosity/score")
                .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(80))
                .andExpect(jsonPath("$.category").value("VERY_GENEROUS"))
                .andExpect(jsonPath("$.confidence").value("MEDIUM"))
                .andExpect(jsonPath("$.totalTips").value(3));

        // Fetch User B score (Median 5 -> 20/100, CONSERVATIVE, MEDIUM confidence)
        mockMvc.perform(get("/api/generosity/score")
                .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(20))
                .andExpect(jsonPath("$.category").value("CONSERVATIVE"))
                .andExpect(jsonPath("$.confidence").value("MEDIUM"))
                .andExpect(jsonPath("$.totalTips").value(3));
    }
}
