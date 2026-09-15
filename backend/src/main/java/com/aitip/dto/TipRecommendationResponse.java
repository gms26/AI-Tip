package com.aitip.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TipRecommendationResponse(
        LocalDateTime generatedAt,
        List<TipRecommendation> recommendations,
        String summary,
        String aiExplanation
) {
}
