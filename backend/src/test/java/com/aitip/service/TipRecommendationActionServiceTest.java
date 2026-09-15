package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.RecommendationAction;
import com.aitip.entity.User;
import com.aitip.repository.RecommendationActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TipRecommendationActionServiceTest {

    private UserService userService;
    private RecommendationActionRepository actionRepository;
    private TipRecommendationActionService actionService;

    private User testUser;

    @BeforeEach
    void setup() {
        userService = mock(UserService.class);
        actionRepository = mock(RecommendationActionRepository.class);
        actionService = new TipRecommendationActionService(userService, actionRepository);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");

        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
    }

    @Test
    void performAction_validReview_savesAction() {
        TipRecommendationActionRequest req = new TipRecommendationActionRequest(TipRecommendationAction.REVIEWED, null);
        TipRecommendationActionResponse res = actionService.performAction("test@example.com", "BUDGET_WARNING", "USD", req);

        ArgumentCaptor<RecommendationAction> captor = ArgumentCaptor.forClass(RecommendationAction.class);
        verify(actionRepository).save(captor.capture());

        RecommendationAction saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(TipRecommendationAction.REVIEWED);
        assertThat(saved.getRecommendationType()).isEqualTo(TipRecommendationType.BUDGET_WARNING);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(res.message()).contains("reviewed");
    }

    @Test
    void performAction_invalidSnooze_throwsException() {
        // Missing timestamp
        TipRecommendationActionRequest req1 = new TipRecommendationActionRequest(TipRecommendationAction.SNOOZED, null);
        assertThatThrownBy(() -> actionService.performAction("test@example.com", "BUDGET_WARNING", null, req1))
                .isInstanceOf(IllegalArgumentException.class);

        // Past timestamp
        TipRecommendationActionRequest req2 = new TipRecommendationActionRequest(TipRecommendationAction.SNOOZED, LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> actionService.performAction("test@example.com", "BUDGET_WARNING", null, req2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void filterAndAnnotate_removesDismissedWithinWindow() {
        TipRecommendation rec1 = new TipRecommendation(TipRecommendationType.BUDGET_WARNING, TipRecommendationPriority.HIGH, "t", "m", "USD", "v", "l", "/a");
        
        RecommendationAction action = new RecommendationAction();
        action.setAction(TipRecommendationAction.DISMISSED);
        action.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action.setCurrency("USD");
        action.setCreatedAt(LocalDateTime.now().minusDays(2)); // Within 7 day window

        when(actionRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(action));

        List<TipRecommendation> filtered = actionService.filterAndAnnotateRecommendations("test@example.com", List.of(rec1));
        assertThat(filtered).isEmpty();
    }

    @Test
    void filterAndAnnotate_keepsDismissedOutsideWindow() {
        TipRecommendation rec1 = new TipRecommendation(TipRecommendationType.BUDGET_WARNING, TipRecommendationPriority.HIGH, "t", "m", "USD", "v", "l", "/a");
        
        RecommendationAction action = new RecommendationAction();
        action.setAction(TipRecommendationAction.DISMISSED);
        action.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action.setCurrency("USD");
        action.setCreatedAt(LocalDateTime.now().minusDays(8)); // Outside 7 day window

        when(actionRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(action));

        List<TipRecommendation> filtered = actionService.filterAndAnnotateRecommendations("test@example.com", List.of(rec1));
        assertThat(filtered).hasSize(1);
    }

    @Test
    void filterAndAnnotate_removesActiveSnooze() {
        TipRecommendation rec1 = new TipRecommendation(TipRecommendationType.BUDGET_WARNING, TipRecommendationPriority.HIGH, "t", "m", "USD", "v", "l", "/a");
        
        RecommendationAction action = new RecommendationAction();
        action.setAction(TipRecommendationAction.SNOOZED);
        action.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action.setCurrency("USD");
        action.setCreatedAt(LocalDateTime.now().minusDays(1));
        action.setSnoozedUntil(LocalDateTime.now().plusDays(2)); // Active snooze

        when(actionRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(action));

        List<TipRecommendation> filtered = actionService.filterAndAnnotateRecommendations("test@example.com", List.of(rec1));
        assertThat(filtered).isEmpty();
    }

    @Test
    void filterAndAnnotate_keepsExpiredSnooze() {
        TipRecommendation rec1 = new TipRecommendation(TipRecommendationType.BUDGET_WARNING, TipRecommendationPriority.HIGH, "t", "m", "USD", "v", "l", "/a");
        
        RecommendationAction action = new RecommendationAction();
        action.setAction(TipRecommendationAction.SNOOZED);
        action.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action.setCurrency("USD");
        action.setCreatedAt(LocalDateTime.now().minusDays(3));
        action.setSnoozedUntil(LocalDateTime.now().minusDays(1)); // Expired snooze

        when(actionRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(action));

        List<TipRecommendation> filtered = actionService.filterAndAnnotateRecommendations("test@example.com", List.of(rec1));
        assertThat(filtered).hasSize(1);
    }
}
