package com.aitip.service;

import com.aitip.dto.*;
import com.aitip.entity.RecommendationAction;
import com.aitip.entity.User;
import com.aitip.repository.RecommendationActionRepository;
import com.aitip.util.CurrencyValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipRecommendationActionService {

    public static final int DISMISSAL_SUPPRESSION_DAYS = 7;

    private final UserService userService;
    private final RecommendationActionRepository actionRepository;

    @Transactional
    public TipRecommendationActionResponse performAction(String email, String typeString, String currencyParam, TipRecommendationActionRequest request) {
        User user = userService.getUserByEmail(email);

        TipRecommendationType type;
        try {
            type = TipRecommendationType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid recommendation type");
        }

        String currency = null;
        if (currencyParam != null && !currencyParam.isBlank()) {
            currency = CurrencyValidationUtil.normalizeAndValidate(currencyParam);
        }

        if (request.action() == null) {
            throw new IllegalArgumentException("Action is required");
        }

        if (request.action() == TipRecommendationAction.SNOOZED) {
            if (request.snoozeUntil() == null) {
                throw new IllegalArgumentException("snoozeUntil is required when action is SNOOZED");
            }
            if (request.snoozeUntil().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("snoozeUntil must be in the future");
            }
        }

        RecommendationAction action = new RecommendationAction();
        action.setUser(user);
        action.setRecommendationType(type);
        action.setCurrency(currency);
        action.setAction(request.action());
        action.setCreatedAt(LocalDateTime.now());
        
        if (request.action() == TipRecommendationAction.SNOOZED) {
            action.setSnoozedUntil(request.snoozeUntil());
        }

        actionRepository.save(action);

        return new TipRecommendationActionResponse(
                action.getRecommendationType(),
                action.getCurrency(),
                action.getAction(),
                action.getCreatedAt(),
                action.getSnoozedUntil(),
                "Recommendation successfully " + action.getAction().name().toLowerCase()
        );
    }

    @Transactional(readOnly = true)
    public TipRecommendationHistoryResponse getHistory(String email) {
        User user = userService.getUserByEmail(email);
        List<RecommendationAction> actions = actionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<TipRecommendationHistoryItem> items = actions.stream()
                .map(a -> new TipRecommendationHistoryItem(
                        a.getRecommendationType(),
                        a.getCurrency(),
                        a.getAction(),
                        a.getCreatedAt(),
                        a.getSnoozedUntil()
                )).toList();
                
        return new TipRecommendationHistoryResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public List<TipRecommendation> filterAndAnnotateRecommendations(String email, List<TipRecommendation> generatedRecommendations) {
        User user = userService.getUserByEmail(email);
        List<RecommendationAction> allUserActions = actionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<TipRecommendation> visibleRecommendations = new ArrayList<>();
        
        for (TipRecommendation rec : generatedRecommendations) {
            // Find the latest action for this specific type and currency
            RecommendationAction latestAction = allUserActions.stream()
                    .filter(a -> a.getRecommendationType() == rec.type() &&
                                 ((a.getCurrency() == null && rec.currency() == null) ||
                                  (a.getCurrency() != null && a.getCurrency().equals(rec.currency()))))
                    .findFirst() // Since it's ordered by createdAt DESC
                    .orElse(null);
                    
            if (latestAction == null) {
                visibleRecommendations.add(rec);
                continue;
            }
            
            boolean isSuppressed = false;
            LocalDateTime now = LocalDateTime.now();
            
            if (latestAction.getAction() == TipRecommendationAction.DISMISSED) {
                if (latestAction.getCreatedAt().plusDays(DISMISSAL_SUPPRESSION_DAYS).isAfter(now)) {
                    isSuppressed = true;
                }
            } else if (latestAction.getAction() == TipRecommendationAction.SNOOZED) {
                if (latestAction.getSnoozedUntil() != null && latestAction.getSnoozedUntil().isAfter(now)) {
                    isSuppressed = true;
                }
            }
            
            if (!isSuppressed) {
                // If it's not suppressed, we add it, and attach the userAction if it was REVIEWED
                TipRecommendationAction actionToAttach = null;
                if (latestAction.getAction() == TipRecommendationAction.REVIEWED) {
                    actionToAttach = TipRecommendationAction.REVIEWED;
                }
                
                visibleRecommendations.add(new TipRecommendation(
                        rec.type(),
                        rec.priority(),
                        rec.title(),
                        rec.message(),
                        rec.currency(),
                        rec.supportingValue(),
                        rec.supportingValueLabel(),
                        rec.action(),
                        actionToAttach
                ));
            }
        }
        
        return visibleRecommendations;
    }
}
