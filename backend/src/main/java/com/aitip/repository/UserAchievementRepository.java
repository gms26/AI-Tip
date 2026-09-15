package com.aitip.repository;

import com.aitip.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement WHERE ua.user.id = :userId")
    List<UserAchievement> findAllByUserIdWithAchievements(@Param("userId") UUID userId);

    boolean existsByUserIdAndAchievementId(UUID userId, UUID achievementId);
}
