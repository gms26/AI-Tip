package com.aitip.controller;

import com.aitip.entity.User;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TipCoachControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("coach-test@example.com");
        user.setPassword("password");
        user.setName("Coach Test");
        userRepository.save(user);

        token = jwtTokenProvider.generateToken("coach-test@example.com");
    }

    @Test
    void getCoaching_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/tip-coach"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCoaching_Authenticated_ReturnsEmptyState() throws Exception {
        mockMvc.perform(get("/api/tip-coach")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.focus").value("NO_ACTION"))
                .andExpect(jsonPath("$.focusPriority").value(9));
    }

    @Test
    void getCoaching_WithCurrency_ReturnsEmptyState() throws Exception {
        mockMvc.perform(get("/api/tip-coach")
                        .param("currency", "USD")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.focus").value("NO_ACTION"));
    }
}
