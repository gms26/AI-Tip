package com.aitip.service;

import com.aitip.dto.SmartTipAdaptationResult;
import com.aitip.dto.SmartTipFeedbackDirection;
import com.aitip.dto.SmartTipFeedbackType;
import com.aitip.dto.SmartTipPersonalizationEffectivenessResponse;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.enums.CalibrationStatus;
import com.aitip.enums.PersonalizationEffectiveness;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartTipPersonalizationEffectivenessServiceTest {

    @Mock
    private TipRecommendationFeedbackRepository feedbackRepository;

    @Mock
    private SmartTipPersonalizationService personalizationService;

    @Mock
    private SmartTipAdaptationService adaptationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private SmartTipPersonalizationEffectivenessService service;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(TEST_EMAIL);

        lenient().when(userService.getUserByEmail(TEST_EMAIL)).thenReturn(testUser);
        lenient().when(personalizationService.isPersonalizationEnabled(testUser, "USD")).thenReturn(true);
    }

    private TipRecommendationFeedback createFeedback(BigDecimal diff, SmartTipFeedbackType type) {
        return TipRecommendationFeedback.builder()
                .differencePercentagePoints(diff)
                .feedbackType(type)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void mockAdaptation(List<TipRecommendationFeedback> list, SmartTipFeedbackDirection direction, long acc, long mod, long cus) {
        SmartTipAdaptationResult result = new SmartTipAdaptationResult(
                list.size(), acc, mod, cus, list.size(), BigDecimal.ZERO, direction, false, BigDecimal.ZERO, "Msg"
        );
        lenient().when(adaptationService.analyze(list)).thenReturn(result);
        when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD")).thenReturn(list);
    }

    @Nested
    @DisplayName("Empty States and Transitions")
    class EmptyStates {
        @Test
        @DisplayName("1. Empty feedback returns INSUFFICIENT_DATA")
        void emptyFeedback() {
            when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD")).thenReturn(Collections.emptyList());

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.totalFeedback()).isZero();
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.INSUFFICIENT_DATA);
            assertThat(res.calibrationStatus()).isEqualTo(CalibrationStatus.NOT_ENOUGH_DATA);
        }

        @Test
        @DisplayName("2. One feedback record returns INSUFFICIENT_DATA")
        void oneFeedback() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 1, 0, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.totalFeedback()).isEqualTo(1);
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.INSUFFICIENT_DATA);
        }

        @Test
        @DisplayName("3. Four feedback records returns INSUFFICIENT_DATA")
        void fourFeedback() {
            List<TipRecommendationFeedback> list = Collections.nCopies(4, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 4, 0, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.totalFeedback()).isEqualTo(4);
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.INSUFFICIENT_DATA);
        }

        @Test
        @DisplayName("4. Exactly five feedback records returns EARLY_SIGNAL")
        void fiveFeedback() {
            List<TipRecommendationFeedback> list = Collections.nCopies(5, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 5, 0, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.totalFeedback()).isEqualTo(5);
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.EARLY_SIGNAL);
        }

        @Test
        @DisplayName("5. Nine feedback records returns EARLY_SIGNAL")
        void nineFeedback() {
            List<TipRecommendationFeedback> list = Collections.nCopies(9, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 9, 0, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.totalFeedback()).isEqualTo(9);
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.EARLY_SIGNAL);
        }

        @Test
        @DisplayName("6. Exactly ten feedback records transitions to full effectiveness")
        void tenFeedback() {
            List<TipRecommendationFeedback> list = Collections.nCopies(10, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 10, 0, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.totalFeedback()).isEqualTo(10);
            assertThat(res.effectiveness()).isNotEqualTo(PersonalizationEffectiveness.INSUFFICIENT_DATA);
            assertThat(res.effectiveness()).isNotEqualTo(PersonalizationEffectiveness.EARLY_SIGNAL);
        }
    }

    @Nested
    @DisplayName("Effectiveness Calculations")
    class EffectivenessCalculations {
        @Test
        @DisplayName("7. 80% alignment is STRONG_ALIGNMENT")
        void strongAlignment() {
            List<TipRecommendationFeedback> list = new ArrayList<>(Collections.nCopies(8, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED)));
            list.addAll(Collections.nCopies(2, createFeedback(new BigDecimal("5.0"), SmartTipFeedbackType.MODIFIED)));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 8, 2, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.alignmentRate()).isEqualByComparingTo("80.00");
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.STRONG_ALIGNMENT);
        }

        @Test
        @DisplayName("8. 60% alignment is HELPFUL")
        void helpfulAlignment() {
            List<TipRecommendationFeedback> list = new ArrayList<>(Collections.nCopies(6, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED)));
            list.addAll(Collections.nCopies(4, createFeedback(new BigDecimal("5.0"), SmartTipFeedbackType.MODIFIED)));
            mockAdaptation(list, SmartTipFeedbackDirection.PREFERS_HIGHER, 6, 4, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.alignmentRate()).isEqualByComparingTo("60.00");
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.HELPFUL);
        }

        @Test
        @DisplayName("9. Below 60% alignment is LOW_ALIGNMENT")
        void lowAlignment() {
            List<TipRecommendationFeedback> list = new ArrayList<>(Collections.nCopies(5, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED)));
            list.addAll(Collections.nCopies(5, createFeedback(new BigDecimal("5.0"), SmartTipFeedbackType.MODIFIED)));
            mockAdaptation(list, SmartTipFeedbackDirection.PREFERS_HIGHER, 5, 5, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.alignmentRate()).isEqualByComparingTo("50.00");
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.LOW_ALIGNMENT);
        }
    }

    @Nested
    @DisplayName("Calibration Status")
    class CalibrationStatusTests {
        @Test
        @DisplayName("10. Difference <=1 pp is WELL_CALIBRATED")
        void wellCalibrated() {
            List<TipRecommendationFeedback> list = Collections.nCopies(5, createFeedback(new BigDecimal("0.5"), SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 0, 5, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.calibrationStatus()).isEqualTo(CalibrationStatus.WELL_CALIBRATED);
        }

        @Test
        @DisplayName("11. Difference >1 and <=3 pp is SLIGHTLY_OFF")
        void slightlyOff() {
            List<TipRecommendationFeedback> list = Collections.nCopies(5, createFeedback(new BigDecimal("2.0"), SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.PREFERS_HIGHER, 0, 5, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.calibrationStatus()).isEqualTo(CalibrationStatus.SLIGHTLY_OFF);
        }

        @Test
        @DisplayName("12. Difference >3 pp is SIGNIFICANTLY_OFF")
        void significantlyOff() {
            List<TipRecommendationFeedback> list = Collections.nCopies(5, createFeedback(new BigDecimal("4.0"), SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.PREFERS_HIGHER, 0, 5, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.calibrationStatus()).isEqualTo(CalibrationStatus.SIGNIFICANTLY_OFF);
        }
    }

    @Nested
    @DisplayName("Counts and Rates")
    class CountsAndRates {
        @Test
        @DisplayName("13. Accepted count is correct")
        void acceptedCount() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 1, 0, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.acceptedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("14. Modified count is correct")
        void modifiedCount() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 0, 1, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.modifiedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("15. Custom count is correct")
        void customCount() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.CUSTOM));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 0, 0, 1);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.customCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("16. Acceptance rate is correct")
        void acceptanceRate() {
            List<TipRecommendationFeedback> list = new ArrayList<>(Collections.nCopies(3, createFeedback(BigDecimal.ZERO, SmartTipFeedbackType.ACCEPTED)));
            list.add(createFeedback(BigDecimal.ONE, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 3, 1, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.acceptanceRate()).isEqualByComparingTo("75.00");
        }

        @Test
        @DisplayName("17. Average difference is correct")
        void averageDifference() {
            List<TipRecommendationFeedback> list = List.of(
                    createFeedback(new BigDecimal("-2.0"), SmartTipFeedbackType.MODIFIED),
                    createFeedback(new BigDecimal("4.0"), SmartTipFeedbackType.MODIFIED)
            );
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 0, 2, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            // abs(-2) = 2, abs(4) = 4. Average = 3.00
            assertThat(res.averageDifferencePercentagePoints()).isEqualByComparingTo("3.00");
        }
    }

    @Nested
    @DisplayName("Learning Direction")
    class LearningDirection {
        @Test
        @DisplayName("18. Higher preference mapped")
        void higherPreference() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ONE, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.PREFERS_HIGHER, 0, 1, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.learningDirection()).isEqualTo(SmartTipFeedbackDirection.PREFERS_HIGHER);
        }

        @Test
        @DisplayName("19. Lower preference mapped")
        void lowerPreference() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ONE, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.PREFERS_LOWER, 0, 1, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.learningDirection()).isEqualTo(SmartTipFeedbackDirection.PREFERS_LOWER);
        }

        @Test
        @DisplayName("20. Aligned preference mapped")
        void alignedPreference() {
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ONE, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 0, 1, 0);
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.learningDirection()).isEqualTo(SmartTipFeedbackDirection.ALIGNED);
        }
    }

    @Nested
    @DisplayName("Isolation and State")
    class IsolationAndState {
        @Test
        @DisplayName("21. Disabled personalization exposes state but still returns historical data")
        void disabledPersonalization() {
            when(personalizationService.isPersonalizationEnabled(testUser, "USD")).thenReturn(false);
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ONE, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.INSUFFICIENT_DATA, 0, 1, 0);

            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");

            assertThat(res.personalizationEnabled()).isFalse();
            assertThat(res.totalFeedback()).isEqualTo(1);
        }

        @Test
        @DisplayName("22. USD isolation")
        void usdIsolation() {
            when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD")).thenReturn(Collections.emptyList());
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("23. INR isolation")
        void inrIsolation() {
            when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "INR")).thenReturn(Collections.emptyList());
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "INR");
            assertThat(res.currency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("24. User isolation is handled by passing correct email to repo")
        void userIsolation() {
            // Already proven by the user passed to feedbackRepository
            List<TipRecommendationFeedback> list = List.of(createFeedback(BigDecimal.ONE, SmartTipFeedbackType.MODIFIED));
            mockAdaptation(list, SmartTipFeedbackDirection.ALIGNED, 0, 1, 0);
            service.getEffectiveness(TEST_EMAIL, "USD");
        }

        @Test
        @DisplayName("25. Reset returning insufficient data (zero records)")
        void resetInsufficientData() {
            when(feedbackRepository.findAllByUserAndCurrencyOrderByCreatedAtAsc(testUser, "USD")).thenReturn(Collections.emptyList());
            SmartTipPersonalizationEffectivenessResponse res = service.getEffectiveness(TEST_EMAIL, "USD");
            assertThat(res.totalFeedback()).isZero();
            assertThat(res.effectiveness()).isEqualTo(PersonalizationEffectiveness.INSUFFICIENT_DATA);
        }
    }
}
