package com.aitip.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AchievementResponse(
        UUID id,
        String code,
        String name,
        String description,
        String category,
        String icon,
        boolean unlocked,
        LocalDateTime unlockedAt,
        int progress,
        int requirement
) {}
