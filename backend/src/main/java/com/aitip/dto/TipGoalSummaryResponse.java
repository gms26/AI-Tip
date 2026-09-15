package com.aitip.dto;

import java.util.List;

public record TipGoalSummaryResponse(
        int totalGoals,
        int activeGoals,
        int completedGoals,
        int expiredGoals,
        double overallCompletionPercentage,
        List<TipGoalProgressResponse> goals
) {}
