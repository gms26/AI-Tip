package com.aitip.controller;

import com.aitip.dto.RegisterRequest;
import com.aitip.dto.CreateTipRequest;
import com.aitip.dto.ServiceQuality;
import com.aitip.service.AuthService;
import com.aitip.service.TipService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AchievementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private TipService tipService;

    private String tokenA;
    private String tokenB;
    private String emailA = "usera@example.com";
    private String emailB = "userb@example.com";

    @BeforeEach
    void setUp() {
        tokenA = authService.register(new RegisterRequest("User A", emailA, "password")).token();
        tokenB = authService.register(new RegisterRequest("User B", emailB, "password")).token();
    }

    @Test
    void getAchievements_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/achievements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAchievements_NewUser_AllLocked() throws Exception {
        mockMvc.perform(get("/api/achievements")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].unlocked").value(false))
                .andExpect(jsonPath("$[0].progress").value(0));
    }

    @Test
    void getSummary_NewUser_ReturnsZero() throws Exception {
        mockMvc.perform(get("/api/achievements/summary")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlockedAchievements").value(0))
                .andExpect(jsonPath("$.completionPercentage").value(0.0));
    }

    @Test
    void evaluateAchievements_FirstTip_UnlocksAndIdempotent() throws Exception {
        // User A creates a tip
        tipService.calculateAndSave(new CreateTipRequest(
                new BigDecimal("50.00"), new BigDecimal("20.00"), "Cafe", "USD", ServiceQuality.GOOD
        ), emailA);

        // Evaluate first time -> should return FIRST_TIP
        mockMvc.perform(post("/api/achievements/evaluate")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].code").value("FIRST_TIP"));

        // Evaluate second time -> should be idempotent, return empty
        mockMvc.perform(post("/api/achievements/evaluate")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Summary should now show 1 (or 2 if SERVICE_QUALITY_USER unlocked) unlocked
        mockMvc.perform(get("/api/achievements/summary")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlockedAchievements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void evaluateAchievements_UserIsolation() throws Exception {
        // User A creates a tip
        tipService.calculateAndSave(new CreateTipRequest(
                new BigDecimal("50.00"), new BigDecimal("20.00"), "Cafe", "USD", ServiceQuality.GOOD
        ), emailA);

        mockMvc.perform(post("/api/achievements/evaluate")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));

        // User B evaluates but has no tips
        mockMvc.perform(post("/api/achievements/evaluate")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
