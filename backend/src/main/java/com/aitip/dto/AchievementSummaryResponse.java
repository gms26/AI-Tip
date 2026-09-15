package com.aitip.dto;

import java.math.BigDecimal;
import java.util.List;

public record AchievementSummaryResponse(
        int totalAchievements,
        int unlockedAchievements,
        BigDecimal completionPercentage,
        List<AchievementResponse> recentAchievements,
        List<AchievementResponse> achievements
) {}
