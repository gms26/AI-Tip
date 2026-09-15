package com.aitip.controller;

import com.aitip.dto.TipProfileResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserRepository;
import com.aitip.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TipProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeminiService geminiService;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("profile@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setCreatedAt(LocalDateTime.now());
        otherUser.setUpdatedAt(LocalDateTime.now());
        otherUser = userRepository.save(otherUser);
    }

    @AfterEach
    void tearDown() {
        tipRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void createTip(User user, String currency, String restaurant, double amount, double percentage) {
        Tip tip = new Tip();
        tip.setUser(user);
        tip.setCurrency(currency);
        tip.setRestaurantName(restaurant);
        tip.setBillAmount(BigDecimal.valueOf(50));
        tip.setTipAmount(BigDecimal.valueOf(amount));
        tip.setTipPercentage(BigDecimal.valueOf(percentage));
        tip.setTotalAmount(BigDecimal.valueOf(50 + amount));
        tip.setCreatedAt(LocalDateTime.now());
        tip.setUpdatedAt(LocalDateTime.now());
        tipRepository.save(tip);
    }

    @Test
    @WithMockUser(username = "profile@example.com")
    void testGetProfile_Success() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 20.0);
        createTip(testUser, "USD", "Diner", 5.0, 15.0);
        
        when(geminiService.getRecommendation(anyString())).thenReturn("AI explanation.");

        mockMvc.perform(get("/api/tip-profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(2)))
                .andExpect(jsonPath("$.currencyProfiles", hasSize(1)))
                .andExpect(jsonPath("$.currencyProfiles[0].currency", is("USD")))
                .andExpect(jsonPath("$.aiExplanation", is("AI explanation.")));
    }

    @Test
    @WithMockUser(username = "profile@example.com")
    void testGetProfile_GeminiFailure() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 20.0);
        
        when(geminiService.getRecommendation(anyString())).thenThrow(new RuntimeException("API down"));

        mockMvc.perform(get("/api/tip-profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(1)))
                .andExpect(jsonPath("$.aiExplanation").doesNotExist());
    }

    @Test
    @WithMockUser(username = "profile@example.com")
    void testGetProfile_NoHistory() throws Exception {
        mockMvc.perform(get("/api/tip-profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(0)))
                .andExpect(jsonPath("$.currencyProfiles", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "profile@example.com")
    void testGetProfile_CurrencyFilter() throws Exception {
        createTip(testUser, "USD", "Cafe", 10.0, 20.0);
        createTip(testUser, "EUR", "Cafe", 10.0, 20.0);
        
        mockMvc.perform(get("/api/tip-profile")
                .param("currency", "USD")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(1)))
                .andExpect(jsonPath("$.currencyProfiles", hasSize(1)))
                .andExpect(jsonPath("$.currencyProfiles[0].currency", is("USD")));
    }

    @Test
    @WithMockUser(username = "profile@example.com")
    void testGetProfile_UserIsolation() throws Exception {
        createTip(otherUser, "USD", "Cafe", 10.0, 20.0);
        
        mockMvc.perform(get("/api/tip-profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTipCount", is(0)));
    }

    @Test
    void testGetProfile_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/tip-profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
