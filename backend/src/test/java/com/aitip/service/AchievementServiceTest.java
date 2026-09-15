package com.aitip.service;

import com.aitip.entity.*;
import com.aitip.enums.AchievementCategory;
import com.aitip.enums.AchievementCode;
import com.aitip.repository.AchievementRepository;
import com.aitip.repository.TipBudgetRepository;
import com.aitip.repository.TipPoolRepository;
import com.aitip.repository.TipRepository;
import com.aitip.repository.UserAchievementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private UserAchievementRepository userAchievementRepository;
    @Mock
    private TipRepository tipRepository;
    @Mock
    private TipPoolRepository tipPoolRepository;
    @Mock
    private TipBudgetRepository tipBudgetRepository;

    @InjectMocks
    private AchievementService achievementService;

    private User testUser;
    private List<Achievement> catalog;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");

        catalog = new ArrayList<>();
        catalog.add(createAch(AchievementCode.FIRST_TIP, 1, AchievementCategory.HISTORY));
        catalog.add(createAch(AchievementCode.TIP_COLLECTOR, 10, AchievementCategory.HISTORY));
        catalog.add(createAch(AchievementCode.THREE_RESTAURANTS, 3, AchievementCategory.EXPLORATION));
        catalog.add(createAch(AchievementCode.SERVICE_QUALITY_USER, 1, AchievementCategory.PERSONALIZATION));
        catalog.add(createAch(AchievementCode.BUDGET_SETTER, 1, AchievementCategory.BUDGET));
        catalog.add(createAch(AchievementCode.BUDGET_TRACKER, 5, AchievementCategory.BUDGET));
        catalog.add(createAch(AchievementCode.TEAM_PLAYER, 1, AchievementCategory.POOLING));
        catalog.add(createAch(AchievementCode.THREE_CURRENCIES, 3, AchievementCategory.EXPLORATION));
    }

    private Achievement createAch(AchievementCode code, int req, AchievementCategory cat) {
        Achievement a = new Achievement();
        a.setId(UUID.randomUUID());
        a.setCode(code);
        a.setRequirementValue(req);
        a.setCategory(cat);
        a.setName(code.name());
        a.setDescription("Desc");
        return a;
    }

    @Test
    void zeroHistory_ReturnsAllLocked() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog);
        when(userAchievementRepository.findAllByUserIdWithAchievements(testUser.getId())).thenReturn(Collections.emptyList());
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(Collections.emptyList());
        when(tipPoolRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());
        when(tipBudgetRepository.findAllByUserIdOrderByCurrencyAsc(testUser.getId())).thenReturn(Collections.emptyList());

        var res = achievementService.getAchievements(testUser.getEmail());

        assertThat(res).hasSize(catalog.size());
        assertThat(res).allMatch(a -> !a.unlocked());
        assertThat(res).allMatch(a -> a.progress() == 0);
    }

    @Test
    void evaluate_FirstTip_UnlocksFirstTipAchievement() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog);
        
        Tip t = new Tip();
        t.setCurrency("USD");
        t.setRestaurantName("A");
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(t));
        when(tipPoolRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(Collections.emptyList());
        when(tipBudgetRepository.findAllByUserIdOrderByCurrencyAsc(testUser.getId())).thenReturn(Collections.emptyList());

        when(userAchievementRepository.existsByUserIdAndAchievementId(eq(testUser.getId()), any())).thenReturn(false);
        when(userAchievementRepository.save(any())).thenAnswer(inv -> {
            UserAchievement ua = inv.getArgument(0);
            ua.setUnlockedAt(LocalDateTime.now());
            return ua;
        });

        var newlyUnlocked = achievementService.evaluateAchievements(testUser.getEmail());

        assertThat(newlyUnlocked).hasSize(1);
        assertThat(newlyUnlocked.get(0).code()).isEqualTo(AchievementCode.FIRST_TIP.name());
        verify(userAchievementRepository, times(1)).save(any());
    }

    @Test
    void evaluate_RestaurantNormalization_DoesNotDoubleCount() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog);
        
        Tip t1 = new Tip(); t1.setCurrency("USD"); t1.setRestaurantName("Italian Place");
        Tip t2 = new Tip(); t2.setCurrency("USD"); t2.setRestaurantName(" italian   place ");
        Tip t3 = new Tip(); t3.setCurrency("USD"); t3.setRestaurantName("ITALIAN PLACE");
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(t1, t2, t3));
        
        when(userAchievementRepository.save(any())).thenAnswer(inv -> {
            UserAchievement ua = inv.getArgument(0);
            ua.setUnlockedAt(LocalDateTime.now());
            return ua;
        });
        
        var newlyUnlocked = achievementService.evaluateAchievements(testUser.getEmail());
        
        // We have 3 tips, which triggers FIRST_TIP.
        // It does NOT trigger THREE_RESTAURANTS because it's only 1 unique.
        assertThat(newlyUnlocked).anyMatch(a -> a.code().equals(AchievementCode.FIRST_TIP.name()));
        assertThat(newlyUnlocked).noneMatch(a -> a.code().equals(AchievementCode.THREE_RESTAURANTS.name()));
    }

    @Test
    void evaluate_UniqueRestaurants_UnlocksExplorer() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog);
        
        Tip t1 = new Tip(); t1.setCurrency("USD"); t1.setRestaurantName("A");
        Tip t2 = new Tip(); t2.setCurrency("USD"); t2.setRestaurantName("B");
        Tip t3 = new Tip(); t3.setCurrency("USD"); t3.setRestaurantName("C");
        
        when(tipRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(t1, t2, t3));
        when(userAchievementRepository.save(any())).thenAnswer(inv -> {
            UserAchievement ua = inv.getArgument(0);
            ua.setUnlockedAt(LocalDateTime.now());
            return ua;
        });

        var newlyUnlocked = achievementService.evaluateAchievements(testUser.getEmail());
        
        assertThat(newlyUnlocked).anyMatch(a -> a.code().equals(AchievementCode.THREE_RESTAURANTS.name()));
    }

    @Test
    void evaluate_DraftPool_DoesNotUnlockTeamPlayer() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog);
        
        TipPool p = new TipPool();
        p.setStatus(TipPoolStatus.DRAFT);
        when(tipPoolRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(p));
        
        var newlyUnlocked = achievementService.evaluateAchievements(testUser.getEmail());
        
        assertThat(newlyUnlocked).isEmpty();
    }

    @Test
    void evaluate_FinalizedPool_UnlocksTeamPlayer() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog);
        
        TipPool p = new TipPool();
        p.setStatus(TipPoolStatus.FINALIZED);
        when(tipPoolRepository.findAllByUserIdOrderByCreatedAtDesc(testUser.getId())).thenReturn(List.of(p));
        when(userAchievementRepository.save(any())).thenAnswer(inv -> {
            UserAchievement ua = inv.getArgument(0);
            ua.setUnlockedAt(LocalDateTime.now());
            return ua;
        });

        var newlyUnlocked = achievementService.evaluateAchievements(testUser.getEmail());
        
        assertThat(newlyUnlocked).anyMatch(a -> a.code().equals(AchievementCode.TEAM_PLAYER.name()));
    }

    @Test
    void getSummary_CompletionPercentageCalculatedCorrectly() {
        when(userService.getUserByEmail(testUser.getEmail())).thenReturn(testUser);
        when(achievementRepository.findAll()).thenReturn(catalog); // 8 achievements
        
        Achievement a = catalog.get(0);
        UserAchievement ua = new UserAchievement();
        ua.setAchievement(a);
        ua.setUnlockedAt(LocalDateTime.now());
        
        when(userAchievementRepository.findAllByUserIdWithAchievements(testUser.getId())).thenReturn(List.of(ua));
        
        var summary = achievementService.getSummary(testUser.getEmail());
        
        assertThat(summary.totalAchievements()).isEqualTo(8);
        assertThat(summary.unlockedAchievements()).isEqualTo(1);
        // 1 / 8 = 0.125 -> 12.50%
        assertThat(summary.completionPercentage()).isEqualTo(new BigDecimal("12.50"));
    }
}
