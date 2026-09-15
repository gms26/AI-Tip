package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.TipPersonalizationPreference;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import com.aitip.enums.PersonalizationStatus;
import com.aitip.repository.TipPersonalizationPreferenceRepository;
import com.aitip.repository.TipRecommendationFeedbackRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SmartTipPersonalizationService (Day 36).
 *
 * Covers: settings retrieval, enable/disable, reset semantics,
 * currency isolation, and user isolation.
 */
@ExtendWith(MockitoExtension.class)
class SmartTipPersonalizationServiceTest {

    @Mock
    private TipPersonalizationPreferenceRepository preferenceRepository;

    @Mock
    private TipRecommendationFeedbackRepository feedbackRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SmartTipPersonalizationService service;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .password("hashed")
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .name("Other User")
                .password("hashed")
                .build();

        lenient().when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        lenient().when(userService.getUserByEmail("other@example.com")).thenReturn(otherUser);
    }

    // --- Settings Tests ---

    @Nested
    @DisplayName("Settings Retrieval")
    class SettingsTests {

        @Test
        @DisplayName("1. Default state is enabled when no preference row exists")
        void defaultStateIsEnabled() {
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD")).thenReturn(Optional.empty());
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(0L);

            PersonalizationSettingsResponse response = service.getSettings("test@example.com", "USD");

            assertThat(response.enabled()).isTrue();
            assertThat(response.status()).isEqualTo(PersonalizationStatus.ENABLED);
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.feedbackCount()).isEqualTo(0);
            assertThat(response.lastLearningDate()).isNull();
            assertThat(response.personalizationAvailable()).isFalse();
        }

        @Test
        @DisplayName("2. Existing disabled preference is returned correctly")
        void existingDisabledPreference() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(false).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD")).thenReturn(Optional.of(pref));
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(8L);

            TipRecommendationFeedback recent = TipRecommendationFeedback.builder()
                    .createdAt(LocalDateTime.of(2026, 9, 9, 10, 0)).build();
            when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                    .thenReturn(List.of(recent));

            PersonalizationSettingsResponse response = service.getSettings("test@example.com", "USD");

            assertThat(response.enabled()).isFalse();
            assertThat(response.status()).isEqualTo(PersonalizationStatus.DISABLED);
            assertThat(response.feedbackCount()).isEqualTo(8);
            assertThat(response.lastLearningDate()).isNotNull();
            assertThat(response.personalizationAvailable()).isTrue();
        }

        @Test
        @DisplayName("3. Existing enabled preference is returned correctly")
        void existingEnabledPreference() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(true).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD")).thenReturn(Optional.of(pref));
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(3L);

            TipRecommendationFeedback recent = TipRecommendationFeedback.builder()
                    .createdAt(LocalDateTime.of(2026, 9, 8, 15, 30)).build();
            when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                    .thenReturn(List.of(recent));

            PersonalizationSettingsResponse response = service.getSettings("test@example.com", "USD");

            assertThat(response.enabled()).isTrue();
            assertThat(response.status()).isEqualTo(PersonalizationStatus.ENABLED);
            assertThat(response.feedbackCount()).isEqualTo(3);
            assertThat(response.personalizationAvailable()).isFalse(); // < 5
        }
    }

    // --- Update Tests ---

    @Nested
    @DisplayName("Settings Update")
    class UpdateTests {

        @Test
        @DisplayName("4. Update enabled → disabled")
        void updateEnabledToDisabled() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(true).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.of(pref));
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(0L);

            service.updateSettings("test@example.com", "USD", false);

            assertThat(pref.isPersonalizationEnabled()).isFalse();
            verify(preferenceRepository).save(pref);
        }

        @Test
        @DisplayName("5. Update disabled → enabled")
        void updateDisabledToEnabled() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(false).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.of(pref));
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(0L);

            service.updateSettings("test@example.com", "USD", true);

            assertThat(pref.isPersonalizationEnabled()).isTrue();
            verify(preferenceRepository).save(pref);
        }

        @Test
        @DisplayName("6. Currency isolation — USD setting doesn't affect INR")
        void currencyIsolation() {
            TipPersonalizationPreference usdPref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(true).build();
            TipPersonalizationPreference inrPref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("INR").personalizationEnabled(true).build();

            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.of(usdPref));
            lenient().when(preferenceRepository.findByUserAndCurrency(testUser, "INR"))
                    .thenReturn(Optional.of(inrPref));
            lenient().when(feedbackRepository.countByUserAndCurrency(any(), any())).thenReturn(0L);

            service.updateSettings("test@example.com", "USD", false);

            assertThat(usdPref.isPersonalizationEnabled()).isFalse();
            assertThat(inrPref.isPersonalizationEnabled()).isTrue();
        }

        @Test
        @DisplayName("Creates new preference row when none exists (lazy init)")
        void createsNewPreferenceRow() {
            when(preferenceRepository.findByUserAndCurrency(testUser, "EUR"))
                    .thenReturn(Optional.empty());
            when(feedbackRepository.countByUserAndCurrency(testUser, "EUR")).thenReturn(0L);

            service.updateSettings("test@example.com", "EUR", false);

            ArgumentCaptor<TipPersonalizationPreference> captor = ArgumentCaptor.forClass(TipPersonalizationPreference.class);
            verify(preferenceRepository).save(captor.capture());
            assertThat(captor.getValue().isPersonalizationEnabled()).isFalse();
            assertThat(captor.getValue().getCurrency()).isEqualTo("EUR");
        }
    }

    // --- Reset Tests ---

    @Nested
    @DisplayName("Reset Learning")
    class ResetTests {

        @Test
        @DisplayName("7. Reset deletes only current user's feedback")
        void resetDeletesOnlyCurrentUser() {
            when(feedbackRepository.deleteAllByUserAndCurrency(testUser, "USD")).thenReturn(5L);

            PersonalizationResetResponse response = service.resetLearning("test@example.com", "USD");

            assertThat(response.success()).isTrue();
            assertThat(response.deletedFeedbackCount()).isEqualTo(5);
            verify(feedbackRepository).deleteAllByUserAndCurrency(testUser, "USD");
            verify(feedbackRepository, never()).deleteAllByUserAndCurrency(eq(otherUser), any());
        }

        @Test
        @DisplayName("8. Reset deletes only selected currency")
        void resetDeletesOnlySelectedCurrency() {
            when(feedbackRepository.deleteAllByUserAndCurrency(testUser, "USD")).thenReturn(3L);

            service.resetLearning("test@example.com", "USD");

            verify(feedbackRepository).deleteAllByUserAndCurrency(testUser, "USD");
            verify(feedbackRepository, never()).deleteAllByUserAndCurrency(testUser, "INR");
        }

        @Test
        @DisplayName("9. Reset returns zero when no feedback exists")
        void resetReturnsZeroWhenEmpty() {
            when(feedbackRepository.deleteAllByUserAndCurrency(testUser, "USD")).thenReturn(0L);

            PersonalizationResetResponse response = service.resetLearning("test@example.com", "USD");

            assertThat(response.success()).isTrue();
            assertThat(response.deletedFeedbackCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("10. Reset does not delete tips (only feedback repository called)")
        void resetDoesNotDeleteTips() {
            when(feedbackRepository.deleteAllByUserAndCurrency(testUser, "USD")).thenReturn(2L);

            service.resetLearning("test@example.com", "USD");

            // Only feedbackRepository is called — no tip/goal/budget/achievement repo interactions
            verify(feedbackRepository).deleteAllByUserAndCurrency(testUser, "USD");
            verifyNoMoreInteractions(feedbackRepository);
        }

        @Test
        @DisplayName("13. Reset does not affect another user")
        void resetDoesNotAffectAnotherUser() {
            when(feedbackRepository.deleteAllByUserAndCurrency(testUser, "USD")).thenReturn(4L);

            service.resetLearning("test@example.com", "USD");

            verify(feedbackRepository, never()).deleteAllByUserAndCurrency(eq(otherUser), any());
            verify(feedbackRepository, never()).deleteAllByUser(any());
        }

        @Test
        @DisplayName("14. Reset does not affect another currency")
        void resetDoesNotAffectAnotherCurrency() {
            when(feedbackRepository.deleteAllByUserAndCurrency(testUser, "USD")).thenReturn(1L);

            service.resetLearning("test@example.com", "USD");

            verify(feedbackRepository).deleteAllByUserAndCurrency(testUser, "USD");
            verify(feedbackRepository, never()).deleteAllByUserAndCurrency(testUser, "INR");
            verify(feedbackRepository, never()).deleteAllByUserAndCurrency(testUser, "EUR");
        }
    }

    // --- isPersonalizationEnabled Tests ---

    @Nested
    @DisplayName("isPersonalizationEnabled")
    class IsEnabledTests {

        @Test
        @DisplayName("15. Returns true when enabled preference exists")
        void enabledPreferenceReturnsTrue() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .personalizationEnabled(true).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.of(pref));

            assertThat(service.isPersonalizationEnabled(testUser, "USD")).isTrue();
        }

        @Test
        @DisplayName("16. Returns false when disabled preference exists")
        void disabledPreferenceReturnsFalse() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .personalizationEnabled(false).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.of(pref));

            assertThat(service.isPersonalizationEnabled(testUser, "USD")).isFalse();
        }

        @Test
        @DisplayName("17. Returns true when no preference exists (backward compatible default)")
        void noPreferenceReturnsTrue() {
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.empty());

            assertThat(service.isPersonalizationEnabled(testUser, "USD")).isTrue();
        }

        @Test
        @DisplayName("Safe defaults: null user returns true")
        void nullUserReturnsTrue() {
            assertThat(service.isPersonalizationEnabled(null, "USD")).isTrue();
        }

        @Test
        @DisplayName("Safe defaults: null currency returns true")
        void nullCurrencyReturnsTrue() {
            assertThat(service.isPersonalizationEnabled(testUser, null)).isTrue();
        }

        @Test
        @DisplayName("19. Re-enabling personalization restores learning behavior")
        void reEnablingRestoresLearning() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(false).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD"))
                    .thenReturn(Optional.of(pref));

            // Initially disabled
            assertThat(service.isPersonalizationEnabled(testUser, "USD")).isFalse();

            // Re-enable
            pref.setPersonalizationEnabled(true);
            assertThat(service.isPersonalizationEnabled(testUser, "USD")).isTrue();
        }
    }

    // --- Explainability Tests ---

    @Nested
    @DisplayName("Explainability")
    class ExplainabilityTests {

        @Test
        @DisplayName("21. Enabled state is represented correctly in settings response")
        void enabledStateRepresentation() {
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD")).thenReturn(Optional.empty());
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(0L);

            PersonalizationSettingsResponse response = service.getSettings("test@example.com", "USD");

            assertThat(response.enabled()).isTrue();
            assertThat(response.status()).isEqualTo(PersonalizationStatus.ENABLED);
        }

        @Test
        @DisplayName("22. Disabled state is represented correctly in settings response")
        void disabledStateRepresentation() {
            TipPersonalizationPreference pref = TipPersonalizationPreference.builder()
                    .user(testUser).currency("USD").personalizationEnabled(false).build();
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD")).thenReturn(Optional.of(pref));
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(0L);

            PersonalizationSettingsResponse response = service.getSettings("test@example.com", "USD");

            assertThat(response.enabled()).isFalse();
            assertThat(response.status()).isEqualTo(PersonalizationStatus.DISABLED);
        }

        @Test
        @DisplayName("23. personalizationAvailable is true when feedbackCount >= 5")
        void personalizationAvailableWhenEnoughFeedback() {
            when(preferenceRepository.findByUserAndCurrency(testUser, "USD")).thenReturn(Optional.empty());
            when(feedbackRepository.countByUserAndCurrency(testUser, "USD")).thenReturn(10L);

            TipRecommendationFeedback recent = TipRecommendationFeedback.builder()
                    .createdAt(LocalDateTime.now()).build();
            when(feedbackRepository.findTop5ByUserAndCurrencyOrderByCreatedAtDesc(testUser, "USD"))
                    .thenReturn(List.of(recent));

            PersonalizationSettingsResponse response = service.getSettings("test@example.com", "USD");

            assertThat(response.personalizationAvailable()).isTrue();
        }
    }
}
