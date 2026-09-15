package com.aitip.repository;

import com.aitip.entity.TipGoal;
import com.aitip.entity.TipGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipGoalRepository extends JpaRepository<TipGoal, UUID> {

    Optional<TipGoal> findByIdAndUserId(UUID id, UUID userId);

    List<TipGoal> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<TipGoal> findByUserIdAndStatus(UUID userId, TipGoalStatus status);

    void deleteByIdAndUserId(UUID id, UUID userId);

    // Using COALESCE to match partial unique index behavior for duplicate prevention
    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
        FROM TipGoal t
        WHERE t.user.id = :userId
          AND t.status = 'ACTIVE'
          AND t.goalType = :goalType
          AND t.period = :period
          AND COALESCE(t.currency, '') = COALESCE(:currency, '')
          AND COALESCE(t.restaurantName, '') = COALESCE(:restaurantName, '')
          AND COALESCE(t.serviceQuality, '') = COALESCE(:serviceQuality, '')
    """)
    boolean existsActiveDuplicate(
            @Param("userId") UUID userId,
            @Param("goalType") com.aitip.entity.TipGoalType goalType,
            @Param("period") com.aitip.entity.TipGoalPeriod period,
            @Param("currency") String currency,
            @Param("restaurantName") String restaurantName,
            @Param("serviceQuality") com.aitip.dto.ServiceQuality serviceQuality
    );
}
