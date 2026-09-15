package com.aitip.controller;

import com.aitip.entity.User;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TipInsightControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User userA = new User();
        userA.setEmail("usera@example.com");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setName("User A");
        userRepository.save(userA);

        User userB = new User();
        userB.setEmail("userb@example.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setName("User B");
        userRepository.save(userB);

        userAToken = tokenProvider.generateToken(userA.getEmail());
        userBToken = tokenProvider.generateToken(userB.getEmail());
    }

    @Test
    void testGetInsights_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/insights/tips")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetInsights_EmptyState() throws Exception {
        mockMvc.perform(get("/api/insights/tips")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallTrend.trendDirection").value("INSUFFICIENT_DATA"))
                .andExpect(jsonPath("$.overallTrend.confidence").value("LOW"));
    }
}
