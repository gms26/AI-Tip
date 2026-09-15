package com.aitip.controller;

import com.aitip.dto.PosBillRequest;
import com.aitip.entity.User;
import com.aitip.repository.UserRepository;
import com.aitip.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PosControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(testUser);
        token = "Bearer " + jwtTokenProvider.generateToken(testUser.getEmail());
    }

    @Test
    void testGetBill_ValidRequest_ReturnsBill() throws Exception {
        PosBillRequest request = new PosBillRequest();
        request.setRestaurantName("Integration Rest");
        request.setBillAmount(new BigDecimal("120.00"));
        request.setCurrency("USD");

        mockMvc.perform(post("/api/pos/bill")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").exists())
                .andExpect(jsonPath("$.restaurantName").value("Integration Rest"))
                .andExpect(jsonPath("$.billAmount").value(120.00))
                .andExpect(jsonPath("$.source").value("Mock POS Sandbox"));
    }

    @Test
    void testGetBill_InvalidRequest_Returns400() throws Exception {
        PosBillRequest request = new PosBillRequest();
        request.setRestaurantName(""); // Invalid, must not be blank
        request.setBillAmount(new BigDecimal("120.00"));
        request.setCurrency("USD");

        mockMvc.perform(post("/api/pos/bill")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetBill_WithoutJWT_Returns401() throws Exception {
        PosBillRequest request = new PosBillRequest();
        request.setRestaurantName("Integration Rest");
        request.setBillAmount(new BigDecimal("120.00"));
        request.setCurrency("USD");

        mockMvc.perform(post("/api/pos/bill")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
