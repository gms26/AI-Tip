package com.aitip.controller;

import com.aitip.dto.PersonalizationResetRequest;
import com.aitip.dto.PersonalizationUpdateRequest;
import com.aitip.dto.SmartTipFeedbackType;
import com.aitip.entity.TipPersonalizationPreference;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipPersonalizationPreferenceRepository;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.repository.UserRepository;
import com.aitip.service.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SmartTipPersonalizationController (Day 36).
 *
 * Tests the full HTTP request/response cycle including:
 * - GET/PUT/POST endpoints
 * - Validation failures
 * - Authentication requirements
 * - User isolation
 * - Currency isolation
 * - Persistence verification
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SmartTipPersonalizationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipPersonalizationPreferenceRepository preferenceRepository;

    @Autowired
    private TipRecommendationFeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiProvider AiProvider;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        preferenceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Personalization User");
        testUser.setEmail("personalization@example.com");
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
        feedbackRepository.deleteAll();
        preferenceRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- GET Settings ---

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("GET /api/smart-tip/personalization â€” default enabled state")
    void getSettings_defaultEnabled() throws Exception {
        mockMvc.perform(get("/api/smart-tip/personalization")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.status").value("ENABLED"))
                .andExpect(jsonPath("$.feedbackCount").value(0))
                .andExpect(jsonPath("$.personalizationAvailable").value(false));
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("GET â€” returns disabled after update")
    void getSettings_afterDisable() throws Exception {
        // Pre-create a disabled preference
        TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                .user(testUser)
                .currency("USD")
                .personalizationEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        preferenceRepository.save(pref);

        mockMvc.perform(get("/api/smart-tip/personalization")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    @DisplayName("GET â€” 401 without authentication")
    void getSettings_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/smart-tip/personalization")
                        .param("currency", "USD"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("GET â€” 400 for invalid currency")
    void getSettings_invalidCurrency() throws Exception {
        mockMvc.perform(get("/api/smart-tip/personalization")
                        .param("currency", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    // --- PUT Update ---

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("PUT â€” disable personalization")
    void updateSettings_disable() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonalizationUpdateRequest("USD", false));

        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("PUT â€” enable personalization")
    void updateSettings_enable() throws Exception {
        // First disable
        TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                .user(testUser)
                .currency("USD")
                .personalizationEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        preferenceRepository.save(pref);

        String body = objectMapper.writeValueAsString(new PersonalizationUpdateRequest("USD", true));

        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.status").value("ENABLED"));
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("PUT â€” validation: missing currency")
    void updateSettings_missingCurrency() throws Exception {
        String body = "{\"enabled\": false}";

        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("PUT â€” validation: missing enabled flag")
    void updateSettings_missingEnabled() throws Exception {
        String body = "{\"currency\": \"USD\"}";

        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT â€” 401 without authentication")
    void updateSettings_unauthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonalizationUpdateRequest("USD", false));

        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- POST Reset ---

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("POST /api/smart-tip/personalization/reset â€” deletes feedback")
    void resetLearning_deletesFeedback() throws Exception {
        // Create some feedback records
        for (int i = 0; i < 3; i++) {
            TipRecommendationFeedback fb = TipRecommendationFeedback.builder()
                    .user(testUser)
                    .currency("USD")
                    .billAmount(new BigDecimal("50.00"))
                    .chosenTipPercentage(new BigDecimal("15.00"))
                    .feedbackType(SmartTipFeedbackType.ACCEPTED)
                    .createdAt(LocalDateTime.now())
                    .build();
            feedbackRepository.save(fb);
        }

        String body = objectMapper.writeValueAsString(new PersonalizationResetRequest("USD"));

        mockMvc.perform(post("/api/smart-tip/personalization/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.deletedFeedbackCount").value(3));

        // Verify feedback is actually gone
        long remainingCount = feedbackRepository.countByUserAndCurrency(testUser, "USD");
        org.assertj.core.api.Assertions.assertThat(remainingCount).isZero();
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("POST reset â€” returns zero when no feedback exists")
    void resetLearning_emptyReset() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonalizationResetRequest("USD"));

        mockMvc.perform(post("/api/smart-tip/personalization/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedFeedbackCount").value(0));
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("POST reset â€” currency isolation (USD reset doesn't affect INR)")
    void resetLearning_currencyIsolation() throws Exception {
        // Create USD feedback
        TipRecommendationFeedback usdFb = TipRecommendationFeedback.builder()
                .user(testUser).currency("USD").billAmount(new BigDecimal("50.00"))
                .chosenTipPercentage(new BigDecimal("15.00")).feedbackType(SmartTipFeedbackType.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();
        feedbackRepository.save(usdFb);

        // Create INR feedback
        TipRecommendationFeedback inrFb = TipRecommendationFeedback.builder()
                .user(testUser).currency("INR").billAmount(new BigDecimal("500.00"))
                .chosenTipPercentage(new BigDecimal("10.00")).feedbackType(SmartTipFeedbackType.MODIFIED)
                .createdAt(LocalDateTime.now()).build();
        feedbackRepository.save(inrFb);

        // Reset only USD
        String body = objectMapper.writeValueAsString(new PersonalizationResetRequest("USD"));

        mockMvc.perform(post("/api/smart-tip/personalization/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedFeedbackCount").value(1));

        // Verify INR feedback is untouched
        long inrCount = feedbackRepository.countByUserAndCurrency(testUser, "INR");
        org.assertj.core.api.Assertions.assertThat(inrCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("POST reset â€” user isolation (user A's reset doesn't affect user B)")
    void resetLearning_userIsolation() throws Exception {
        // Create feedback for testUser
        TipRecommendationFeedback testFb = TipRecommendationFeedback.builder()
                .user(testUser).currency("USD").billAmount(new BigDecimal("50.00"))
                .chosenTipPercentage(new BigDecimal("15.00")).feedbackType(SmartTipFeedbackType.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();
        feedbackRepository.save(testFb);

        // Create feedback for otherUser
        TipRecommendationFeedback otherFb = TipRecommendationFeedback.builder()
                .user(otherUser).currency("USD").billAmount(new BigDecimal("75.00"))
                .chosenTipPercentage(new BigDecimal("18.00")).feedbackType(SmartTipFeedbackType.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();
        feedbackRepository.save(otherFb);

        // Reset as testUser
        String body = objectMapper.writeValueAsString(new PersonalizationResetRequest("USD"));

        mockMvc.perform(post("/api/smart-tip/personalization/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedFeedbackCount").value(1));

        // Verify otherUser's feedback is untouched
        long otherCount = feedbackRepository.countByUserAndCurrency(otherUser, "USD");
        org.assertj.core.api.Assertions.assertThat(otherCount).isEqualTo(1);
    }

    @Test
    @DisplayName("POST reset â€” 401 without authentication")
    void resetLearning_unauthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(new PersonalizationResetRequest("USD"));

        mockMvc.perform(post("/api/smart-tip/personalization/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- Persistence Verification ---

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("Persistence: PUT then GET returns updated value")
    void persistenceVerification() throws Exception {
        // Initially enabled (default)
        mockMvc.perform(get("/api/smart-tip/personalization").param("currency", "USD"))
                .andExpect(jsonPath("$.enabled").value(true));

        // Disable
        String disableBody = objectMapper.writeValueAsString(new PersonalizationUpdateRequest("USD", false));
        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disableBody))
                .andExpect(status().isOk());

        // Verify GET returns disabled
        mockMvc.perform(get("/api/smart-tip/personalization").param("currency", "USD"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.status").value("DISABLED"));

        // Re-enable
        String enableBody = objectMapper.writeValueAsString(new PersonalizationUpdateRequest("USD", true));
        mockMvc.perform(put("/api/smart-tip/personalization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enableBody))
                .andExpect(status().isOk());

        // Verify GET returns enabled again
        mockMvc.perform(get("/api/smart-tip/personalization").param("currency", "USD"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.status").value("ENABLED"));
    }

    // --- User Isolation (JWT) ---

    @Test
    @WithMockUser(username = "other@example.com")
    @DisplayName("User isolation: User B cannot see User A's settings")
    void userIsolation_cannotReadOthersSettings() throws Exception {
        // Create a preference for testUser (personalization@example.com)
        TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                .user(testUser)
                .currency("USD")
                .personalizationEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        preferenceRepository.save(pref);

        // otherUser (other@example.com) should see default (enabled) â€” not testUser's disabled state
        mockMvc.perform(get("/api/smart-tip/personalization").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true)); // default, not testUser's disabled
    }

    @Test
    @WithMockUser(username = "personalization@example.com")
    @DisplayName("Feedbackcount reflects in GET after reset")
    void feedbackCountAfterReset() throws Exception {
        // Create feedback
        for (int i = 0; i < 5; i++) {
            TipRecommendationFeedback fb = TipRecommendationFeedback.builder()
                    .user(testUser).currency("USD").billAmount(new BigDecimal("50.00"))
                    .chosenTipPercentage(new BigDecimal("15.00")).feedbackType(SmartTipFeedbackType.ACCEPTED)
                    .createdAt(LocalDateTime.now()).build();
            feedbackRepository.save(fb);
        }

        // Verify feedback count is 5
        mockMvc.perform(get("/api/smart-tip/personalization").param("currency", "USD"))
                .andExpect(jsonPath("$.feedbackCount").value(5))
                .andExpect(jsonPath("$.personalizationAvailable").value(true));

        // Reset
        String body = objectMapper.writeValueAsString(new PersonalizationResetRequest("USD"));
        mockMvc.perform(post("/api/smart-tip/personalization/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Verify count is now 0
        mockMvc.perform(get("/api/smart-tip/personalization").param("currency", "USD"))
                .andExpect(jsonPath("$.feedbackCount").value(0))
                .andExpect(jsonPath("$.personalizationAvailable").value(false));
    }
}
