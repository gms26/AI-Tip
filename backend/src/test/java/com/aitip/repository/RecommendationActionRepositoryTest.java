package com.aitip.repository;

import com.aitip.dto.TipRecommendationAction;
import com.aitip.dto.TipRecommendationType;
import com.aitip.entity.RecommendationAction;
import com.aitip.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RecommendationActionRepositoryTest {

    @Autowired
    private RecommendationActionRepository actionRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setName("User 1");
        user1.setPassword("hash");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setName("User 2");
        user2.setPassword("hash");
        user2 = userRepository.save(user2);
    }

    @Test
    void saveAndFindAllByUserId() {
        RecommendationAction action = new RecommendationAction();
        action.setUser(user1);
        action.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action.setCurrency("USD");
        action.setAction(TipRecommendationAction.DISMISSED);
        action.setCreatedAt(LocalDateTime.now());
        actionRepository.save(action);

        RecommendationAction action2 = new RecommendationAction();
        action2.setUser(user2);
        action2.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action2.setCurrency("USD");
        action2.setAction(TipRecommendationAction.REVIEWED);
        action2.setCreatedAt(LocalDateTime.now());
        actionRepository.save(action2);

        List<RecommendationAction> user1Actions = actionRepository.findAllByUserIdOrderByCreatedAtDesc(user1.getId());
        assertThat(user1Actions).hasSize(1);
        assertThat(user1Actions.get(0).getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    void findLatestActions() {
        RecommendationAction action1 = new RecommendationAction();
        action1.setUser(user1);
        action1.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action1.setCurrency("USD");
        action1.setAction(TipRecommendationAction.REVIEWED);
        action1.setCreatedAt(LocalDateTime.now().minusDays(2));
        actionRepository.save(action1);

        RecommendationAction action2 = new RecommendationAction();
        action2.setUser(user1);
        action2.setRecommendationType(TipRecommendationType.BUDGET_WARNING);
        action2.setCurrency("USD");
        action2.setAction(TipRecommendationAction.DISMISSED);
        action2.setCreatedAt(LocalDateTime.now());
        actionRepository.save(action2);

        List<RecommendationAction> actions = actionRepository.findLatestActions(user1.getId(), TipRecommendationType.BUDGET_WARNING, "USD");
        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).getAction()).isEqualTo(TipRecommendationAction.DISMISSED);
    }
}
