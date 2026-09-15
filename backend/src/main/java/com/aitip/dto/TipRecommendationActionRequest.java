package com.aitip.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TipRecommendationActionRequest(
        @NotNull(message = "Action is required")
        TipRecommendationAction action,
        
        LocalDateTime snoozeUntil
) {
}
