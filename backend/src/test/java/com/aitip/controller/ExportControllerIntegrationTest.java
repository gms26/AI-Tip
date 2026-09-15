package com.aitip.controller;

import com.aitip.dto.AnalyticsPeriod;
import com.aitip.dto.AnalyticsRequest;
import com.aitip.dto.ExportFilterRequest;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        tipRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("exporttest@example.com")
                .password("hash")
                .name("Export Test")
                .build();
        userRepository.save(testUser);

        token = jwtTokenProvider.generateToken(testUser.getEmail());
    }

    @Test
    void exportTips_CsvFormat() throws Exception {
        Tip tip = Tip.builder()
                .user(testUser)
                .restaurantName("Test Rest")
                .billAmount(new BigDecimal("10.00"))
                .tipPercentage(new BigDecimal("15.00"))
                .tipAmount(new BigDecimal("1.50"))
                .totalAmount(new BigDecimal("11.50"))
                .currency("USD")
                .build();
        tipRepository.save(tip);

        ExportFilterRequest filter = new ExportFilterRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/export/tips")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"tips.csv\""))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Rest")));
    }

    @Test
    void exportTips_JsonFormat() throws Exception {
        Tip tip = Tip.builder()
                .user(testUser)
                .restaurantName("Test Rest JSON")
                .billAmount(new BigDecimal("10.00"))
                .tipPercentage(new BigDecimal("15.00"))
                .tipAmount(new BigDecimal("1.50"))
                .totalAmount(new BigDecimal("11.50"))
                .currency("USD")
                .build();
        tipRepository.save(tip);

        ExportFilterRequest filter = new ExportFilterRequest(null, null, null, null, null);

        mockMvc.perform(post("/api/export/tips?format=json")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"tips.json\""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Rest JSON")));
    }

    @Test
    void exportAnalytics_ReturnsJson() throws Exception {
        AnalyticsRequest request = new AnalyticsRequest(AnalyticsPeriod.ALL_TIME, null, null, null, null);

        mockMvc.perform(post("/api/export/analytics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"analytics.json\""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
