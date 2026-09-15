package com.aitip.service;

import com.aitip.dto.AchievementResponse;
import com.aitip.dto.AchievementSummaryResponse;
import com.aitip.entity.*;
import com.aitip.enums.AchievementCode;
import com.aitip.repository.AchievementRepository;
import com.aitip.repository.TipBudgetRepository;
import com.aitip.repository.TipPoolRepository;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserAchievementRepository;
import com.aitip.util.CurrencyValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final UserService userService;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final TipRepository tipRepository;
    private final TipPoolRepository tipPoolRepository;
    private final TipBudgetRepository tipBudgetRepository;

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAchievements(String email) {
        User user = userService.getUserByEmail(email);
        List<Achievement> allAchievements = achievementRepository.findAll();
        List<UserAchievement> unlocked = userAchievementRepository.findAllByUserIdWithAchievements(user.getId());

        Map<UUID, UserAchievement> unlockedMap = unlocked.stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

        List<AchievementResponse> responses = new ArrayList<>();
        
        // We need to calculate current progress for all achievements
        // This requires evaluating the facts just to get the display numbers
        Map<AchievementCode, Integer> progressMap = calculateProgressFacts(user);

        for (Achievement achievement : allAchievements) {
            UserAchievement ua = unlockedMap.get(achievement.getId());
            boolean isUnlocked = ua != null;
            
            int requirement = achievement.getRequirementValue();
            int currentVal = progressMap.getOrDefault(achievement.getCode(), 0);
            int progress = Math.min(currentVal, requirement);
            
            // If it is unlocked in the DB, force progress to requirement for display purposes
            if (isUnlocked) {
                progress = requirement;
            }

            responses.add(new AchievementResponse(
                    achievement.getId(),
                    achievement.getCode().name(),
                    achievement.getName(),
                    achievement.getDescription(),
                    achievement.getCategory().name(),
                    achievement.getIcon(),
                    isUnlocked,
                    isUnlocked ? ua.getUnlockedAt() : null,
                    progress,
                    requirement
            ));
        }

        // Sort by Category, then Unlocked Status (unlocked first), then Requirement
        responses.sort(Comparator
                .comparing(AchievementResponse::category)
                .thenComparing(AchievementResponse::unlocked).reversed()
                .thenComparingInt(AchievementResponse::requirement));

        return responses;
    }

    @Transactional(readOnly = true)
    public AchievementSummaryResponse getSummary(String email) {
        List<AchievementResponse> achievements = getAchievements(email);
        
        int total = achievements.size();
        long unlockedCount = achievements.stream().filter(AchievementResponse::unlocked).count();
        
        BigDecimal completionPercentage = BigDecimal.ZERO;
        if (total > 0) {
            completionPercentage = BigDecimal.valueOf(unlockedCount)
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }

        List<AchievementResponse> recent = achievements.stream()
                .filter(AchievementResponse::unlocked)
                .sorted(Comparator.comparing(AchievementResponse::unlockedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .toList();

        return new AchievementSummaryResponse(total, (int) unlockedCount, completionPercentage, recent, achievements);
    }

    @Transactional
    public List<AchievementResponse> evaluateAchievements(String email) {
        User user = userService.getUserByEmail(email);
        List<Achievement> allAchievements = achievementRepository.findAll();
        Map<AchievementCode, Integer> progressMap = calculateProgressFacts(user);
        
        List<AchievementResponse> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            int currentVal = progressMap.getOrDefault(achievement.getCode(), 0);
            if (currentVal >= achievement.getRequirementValue()) {
                // Condition met, unlock if not already unlocked
                boolean exists = userAchievementRepository.existsByUserIdAndAchievementId(user.getId(), achievement.getId());
                if (!exists) {
                    UserAchievement ua = new UserAchievement();
                    ua.setUser(user);
                    ua.setAchievement(achievement);
                    UserAchievement saved = userAchievementRepository.save(ua);
                    
                    newlyUnlocked.add(new AchievementResponse(
                            achievement.getId(),
                            achievement.getCode().name(),
                            achievement.getName(),
                            achievement.getDescription(),
                            achievement.getCategory().name(),
                            achievement.getIcon(),
                            true,
                            saved.getUnlockedAt(),
                            achievement.getRequirementValue(),
                            achievement.getRequirementValue()
                    ));
                    log.info("User {} unlocked achievement: {}", user.getEmail(), achievement.getCode());
                }
            }
        }
        
        return newlyUnlocked;
    }

    private Map<AchievementCode, Integer> calculateProgressFacts(User user) {
        List<Tip> tips = tipRepository.findAllByUserId(user.getId());
        List<TipPool> pools = tipPoolRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        List<TipBudget> budgets = tipBudgetRepository.findAllByUserIdOrderByCurrencyAsc(user.getId());

        int tipCount = tips.size();
        
        // Unique Normalized Restaurants
        Set<String> uniqueRestaurants = tips.stream()
                .map(t -> normalizeString(t.getRestaurantName()))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
                
        // Unique Normalized Currencies
        Set<String> uniqueCurrencies = tips.stream()
                .map(t -> CurrencyValidationUtil.normalizeAndValidate(t.getCurrency()))
                .collect(Collectors.toSet());
                
        // Rated Tips
        long ratedCount = tips.stream()
                .filter(t -> t.getServiceQuality() != null)
                .count();

        // Finalized Pools
        long finalizedPools = pools.stream()
                .filter(p -> p.getStatus() == TipPoolStatus.FINALIZED)
                .count();

        // Budget Tracker Logic
        // Requires a budget and at least 5 tips IN THAT SAME CURRENCY
        int maxTipsInBudgetCurrency = 0;
        for (TipBudget budget : budgets) {
            String budgetCurrency = CurrencyValidationUtil.normalizeAndValidate(budget.getCurrency());
            long tipsInCurrency = tips.stream()
                    .filter(t -> CurrencyValidationUtil.normalizeAndValidate(t.getCurrency()).equals(budgetCurrency))
                    .count();
            if (tipsInCurrency > maxTipsInBudgetCurrency) {
                maxTipsInBudgetCurrency = (int) tipsInCurrency;
            }
        }

        Map<AchievementCode, Integer> map = new EnumMap<>(AchievementCode.class);
        
        // History & Consistency
        map.put(AchievementCode.FIRST_TIP, tipCount);
        map.put(AchievementCode.TIP_COLLECTOR, tipCount);
        map.put(AchievementCode.TIP_MASTER, tipCount);
        map.put(AchievementCode.TIP_LEGEND, tipCount);
        map.put(AchievementCode.FIVE_VISITS, tipCount);
        map.put(AchievementCode.TWENTY_FIVE_VISITS, tipCount);

        // Exploration
        map.put(AchievementCode.THREE_RESTAURANTS, uniqueRestaurants.size());
        map.put(AchievementCode.TEN_RESTAURANTS, uniqueRestaurants.size());
        map.put(AchievementCode.THREE_CURRENCIES, uniqueCurrencies.size());

        // Personalization
        map.put(AchievementCode.SERVICE_QUALITY_USER, (int) ratedCount);
        map.put(AchievementCode.PERSONALIZED_USER, (int) ratedCount);

        // Budget
        map.put(AchievementCode.BUDGET_SETTER, budgets.size());
        map.put(AchievementCode.BUDGET_TRACKER, budgets.isEmpty() ? 0 : maxTipsInBudgetCurrency);

        // Pooling
        map.put(AchievementCode.TEAM_PLAYER, (int) finalizedPools);
        map.put(AchievementCode.POOL_MASTER, (int) finalizedPools);

        // Receipts & Analytics are locked currently because no persisted facts exist for them.
        map.put(AchievementCode.RECEIPT_SCANNER, 0);
        map.put(AchievementCode.RECEIPT_RECONCILER, 0);
        map.put(AchievementCode.ANALYTICS_USER, 0);
        map.put(AchievementCode.INSIGHTS_USER, 0);

        return map;
    }

    private String normalizeString(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
