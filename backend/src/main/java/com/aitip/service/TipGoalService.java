package com.aitip.service;

import com.aitip.dto.CreateTipGoalRequest;
import com.aitip.dto.TipGoalProgressResponse;
import com.aitip.dto.TipGoalResponse;
import com.aitip.dto.TipGoalSummaryResponse;
import com.aitip.entity.TipGoal;
import com.aitip.entity.TipGoalStatus;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipGoalRepository;
import com.aitip.util.CurrencyValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TipGoalService {

    private final TipGoalRepository tipGoalRepository;
    private final TipGoalCalculationService calculationService;
    private final UserService userService;

    @Transactional
    public TipGoalResponse createGoal(String email, CreateTipGoalRequest request) {
        User user = userService.getUserByEmail(email);

        String currency = null;
        if (request.currency() != null && !request.currency().isBlank()) {
            currency = CurrencyValidationUtil.normalizeAndValidate(request.currency());
        }
        
        String restaurantName = request.restaurantName() != null && !request.restaurantName().isBlank() ? request.restaurantName().trim() : null;
        String reqServiceQualityStr = request.serviceQuality() != null ? request.serviceQuality().name() : null;

        boolean exists = tipGoalRepository.existsActiveDuplicate(
                user.getId(),
                request.goalType(),
                request.period(),
                currency,
                restaurantName,
                request.serviceQuality()
        );

        if (exists) {
            throw new IllegalStateException("An active goal with this exact configuration already exists.");
        }

        TipGoal goal = TipGoal.builder()
                .user(user)
                .goalType(request.goalType())
                .targetValue(request.targetValue())
                .currency(currency)
                .period(request.period())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .restaurantName(restaurantName)
                .serviceQuality(request.serviceQuality())
                .status(TipGoalStatus.ACTIVE)
                .build();

        goal = tipGoalRepository.save(goal);
        return mapToResponse(goal);
    }

    @Transactional
    public List<TipGoalProgressResponse> getGoals(String email) {
        User user = userService.getUserByEmail(email);
        List<TipGoal> goals = tipGoalRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<TipGoalProgressResponse> responses = new ArrayList<>();
        for (TipGoal goal : goals) {
            responses.add(evaluateAndSyncGoal(goal));
        }
        return responses;
    }

    @Transactional
    public TipGoalProgressResponse getGoalProgress(String email, UUID id) {
        User user = userService.getUserByEmail(email);
        TipGoal goal = tipGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
                
        return evaluateAndSyncGoal(goal);
    }

    @Transactional
    public TipGoalSummaryResponse getSummary(String email) {
        User user = userService.getUserByEmail(email);
        List<TipGoal> goals = tipGoalRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        int active = 0, completed = 0, expired = 0;
        double totalProgressSum = 0;
        
        List<TipGoalProgressResponse> progressResponses = new ArrayList<>();

        for (TipGoal goal : goals) {
            TipGoalProgressResponse progress = evaluateAndSyncGoal(goal);
            progressResponses.add(progress);
            
            if (progress.status() == TipGoalStatus.ACTIVE) active++;
            else if (progress.status() == TipGoalStatus.COMPLETED) completed++;
            else if (progress.status() == TipGoalStatus.EXPIRED) expired++;

            totalProgressSum += progress.progressPercentage().doubleValue();
        }

        double overall = goals.isEmpty() ? 0.0 : totalProgressSum / goals.size();

        return new TipGoalSummaryResponse(
                goals.size(),
                active,
                completed,
                expired,
                overall,
                progressResponses
        );
    }

    @Transactional
    public void cancelGoal(String email, UUID id) {
        User user = userService.getUserByEmail(email);
        TipGoal goal = tipGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
                
        goal.setStatus(TipGoalStatus.CANCELLED);
        tipGoalRepository.save(goal);
    }

    @Transactional
    public TipGoalResponse getGoal(String email, UUID id) {
        User user = userService.getUserByEmail(email);
        TipGoal goal = tipGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));
        return mapToResponse(goal);
    }

    /**
     * Evaluates progress statelessly, and if status has naturally transitioned 
     * (e.g. met target -> COMPLETED, or passed end date -> EXPIRED), 
     * applies the transition and saves.
     */
    private TipGoalProgressResponse evaluateAndSyncGoal(TipGoal goal) {
        TipGoalProgressResponse progress = calculationService.calculateProgress(goal);
        
        if (goal.getStatus() == TipGoalStatus.ACTIVE && progress.status() != TipGoalStatus.ACTIVE) {
            goal.setStatus(progress.status());
            tipGoalRepository.save(goal);
        }
        
        return progress;
    }

    private TipGoalResponse mapToResponse(TipGoal goal) {
        return new TipGoalResponse(
                goal.getId(),
                goal.getGoalType(),
                goal.getTargetValue(),
                goal.getCurrency(),
                goal.getPeriod(),
                goal.getStartDate(),
                goal.getEndDate(),
                goal.getRestaurantName(),
                goal.getServiceQuality(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
