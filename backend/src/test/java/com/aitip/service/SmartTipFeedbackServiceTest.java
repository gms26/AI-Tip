package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmartTipFeedbackServiceTest {

    @Mock
    private TipRecommendationFeedbackRepository feedbackRepository;

    @Mock
    private UserService userService;

    @Mock
    private SmartTipAdaptationService adaptationService;

    private Clock fixedClock;
    private SmartTipFeedbackService service;
    private User testUser;
    private static final String TEST_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
        service = new SmartTipFeedbackService(feedbackRepository, userService, adaptationService, null, fixedClock);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(TEST_EMAIL);
        when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
    }

    // --- Classification Tests ---

    @Test
    @DisplayName("1. Accepted Recommendation: chosen equals suggested percentage")
    void recordFeedback_AcceptedRecommendation() {
        SmartTipFeedbackRequest request = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Luigi Trattoria", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );

        SmartTipFeedbackResponse response = service.recordFeedback(TEST_EMAIL, request);

        assertEquals(SmartTipFeedbackType.ACCEPTED, response.feedbackType());
        assertEquals(new BigDecimal("15.00"), response.suggestedTipPercentage());
        assertEquals(new BigDecimal("15.00"), response.chosenTipPercentage());
        assertEquals(new BigDecimal("0.00"), response.differencePercentagePoints());

        ArgumentCaptor<TipRecommendationFeedback> captor = ArgumentCaptor.forClass(TipRecommendationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        TipRecommendationFeedback saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertEquals("USD", saved.getCurrency());
        assertEquals(SmartTipFeedbackType.ACCEPTED, saved.getFeedbackType());
    }

    @Test
    @DisplayName("2. Modified Recommendation: higher chosen percentage (+3 pp)")
    void recordFeedback_ModifiedHigher() {
        SmartTipFeedbackRequest request = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Sushi Zen", ServiceQuality.EXCELLENT,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("18.00")
        );

        SmartTipFeedbackResponse response = service.recordFeedback(TEST_EMAIL, request);

        assertEquals(SmartTipFeedbackType.MODIFIED, response.feedbackType());
        assertEquals(new BigDecimal("15.00"), response.suggestedTipPercentage());
        assertEquals(new BigDecimal("18.00"), response.chosenTipPercentage());
        assertEquals(new BigDecimal("3.00"), response.differencePercentagePoints());
    }

    @Test
    @DisplayName("3. Modified Recommendation: lower chosen percentage (-3 pp)")
    void recordFeedback_ModifiedLower() {
        SmartTipFeedbackRequest request = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("60.00"), "Diner", ServiceQuality.AVERAGE,
                new BigDecimal("18.00"), "HISTORICAL_TYPICAL", new BigDecimal("15.00")
        );

        SmartTipFeedbackResponse response = service.recordFeedback(TEST_EMAIL, request);

        assertEquals(SmartTipFeedbackType.MODIFIED, response.feedbackType());
        assertEquals(new BigDecimal("18.00"), response.suggestedTipPercentage());
        assertEquals(new BigDecimal("15.00"), response.chosenTipPercentage());
        assertEquals(new BigDecimal("-3.00"), response.differencePercentagePoints());
    }

    @Test
    @DisplayName("4. Custom Tip: no suggested percentage supplied")
    void recordFeedback_CustomTip() {
        SmartTipFeedbackRequest request = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("40.00"), null, ServiceQuality.GOOD,
                null, null, new BigDecimal("20.00")
        );

        SmartTipFeedbackResponse response = service.recordFeedback(TEST_EMAIL, request);

        assertEquals(SmartTipFeedbackType.CUSTOM, response.feedbackType());
        assertNull(response.suggestedTipPercentage());
        assertEquals(new BigDecimal("20.00"), response.chosenTipPercentage());
        assertNull(response.differencePercentagePoints());
    }

    @Test
    @DisplayName("5. BigDecimal Equality: 15 vs 15.00 evaluated accurately")
    void recordFeedback_BigDecimalScaleEquality() {
        SmartTipFeedbackRequest request = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("15"), "OPTIMIZED", new BigDecimal("15.00")
        );

        SmartTipFeedbackResponse response = service.recordFeedback(TEST_EMAIL, request);

        assertEquals(SmartTipFeedbackType.ACCEPTED, response.feedbackType());
        assertEquals(new BigDecimal("0.00"), response.differencePercentagePoints());
    }

    // --- Validation Tests ---

    @Test
    @DisplayName("6. Null request throws IllegalArgumentException")
    void recordFeedback_NullRequest_Throws() {
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, null));
    }

    @Test
    @DisplayName("7. Bill amount <= 0.01 throws IllegalArgumentException")
    void recordFeedback_BillBoundary_Throws() {
        SmartTipFeedbackRequest badReq = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("0.01"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, badReq));
    }

    @Test
    @DisplayName("8. Bill amount 0.02 is valid boundary")
    void recordFeedback_BillBoundary_Valid() {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("0.02"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );
        SmartTipFeedbackResponse res = service.recordFeedback(TEST_EMAIL, req);
        assertNotNull(res);
    }

    @Test
    @DisplayName("9. Chosen tip percentage negative throws IllegalArgumentException")
    void recordFeedback_NegativeChosen_Throws() {
        SmartTipFeedbackRequest badReq = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("-1.00")
        );
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, badReq));
    }

    @Test
    @DisplayName("10. Chosen tip percentage > 100 throws IllegalArgumentException")
    void recordFeedback_Over100Chosen_Throws() {
        SmartTipFeedbackRequest badReq = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("100.01")
        );
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, badReq));
    }

    @Test
    @DisplayName("11. Suggested tip percentage negative throws IllegalArgumentException")
    void recordFeedback_NegativeSuggested_Throws() {
        SmartTipFeedbackRequest badReq = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("-5.00"), "OPTIMIZED", new BigDecimal("15.00")
        );
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, badReq));
    }

    @Test
    @DisplayName("12. Suggested tip percentage > 100 throws IllegalArgumentException")
    void recordFeedback_Over100Suggested_Throws() {
        SmartTipFeedbackRequest badReq = new SmartTipFeedbackRequest(
                "USD", new BigDecimal("50.00"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("105.00"), "OPTIMIZED", new BigDecimal("15.00")
        );
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, badReq));
    }

    @Test
    @DisplayName("13. Invalid currency code throws IllegalArgumentException")
    void recordFeedback_InvalidCurrency_Throws() {
        SmartTipFeedbackRequest badReq = new SmartTipFeedbackRequest(
                "US", new BigDecimal("50.00"), "Cafe", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );
        assertThrows(IllegalArgumentException.class, () -> service.recordFeedback(TEST_EMAIL, badReq));
    }

    // --- Normalization Tests ---

    @Test
    @DisplayName("14. Currency is trimmed and normalized to uppercase")
    void recordFeedback_CurrencyNormalized() {
        SmartTipFeedbackRequest req = new SmartTipFeedbackRequest(
                "usd", new BigDecimal("50.00"), "  Bistro  ", ServiceQuality.GOOD,
                new BigDecimal("15.00"), "OPTIMIZED", new BigDecimal("15.00")
        );
        service.recordFeedback(TEST_EMAIL, req);

        ArgumentCaptor<TipRecommendationFeedback> captor = ArgumentCaptor.forClass(TipRecommendationFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals("USD", captor.getValue().getCurrency());
        assertEquals("Bistro", captor.getValue().getRestaurantName());
    }

    // --- Summary Retrieval & Scoping Tests ---

    @Test
    @DisplayName("15. Summary delegates to adaptation service with user and normalized currency")
    void getSummary_Success() {
        SmartTipAdaptationResult mockAdaptation = new SmartTipAdaptationResult(
                8, 5, 2, 1, 7, new BigDecimal("2.50"),
                SmartTipFeedbackDirection.PREFERS_HIGHER, true, new BigDecimal("2.50"),
                "Based on 7 previous decisions..."
        );
        when(adaptationService.getAdaptation(testUser, "USD")).thenReturn(mockAdaptation);

        SmartTipFeedbackSummaryResponse summary = service.getSummary(TEST_EMAIL, "usd");

        assertEquals("USD", summary.currency());
        assertEquals(8, summary.feedbackCount());
        assertEquals(5, summary.acceptedCount());
        assertEquals(2, summary.modifiedCount());
        assertEquals(1, summary.customCount());
        assertEquals(new BigDecimal("2.50"), summary.averageDifferencePercentagePoints());
        assertEquals(SmartTipFeedbackDirection.PREFERS_HIGHER, summary.direction());
        assertTrue(summary.adaptationApplied());
        assertEquals(new BigDecimal("2.50"), summary.adaptationAdjustment());
    }

    @Test
    @DisplayName("16. Summary with invalid currency throws")
    void getSummary_InvalidCurrency_Throws() {
        assertThrows(IllegalArgumentException.class, () -> service.getSummary(TEST_EMAIL, ""));
    }
}
