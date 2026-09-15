package com.aitip.dto;

import java.util.List;
import java.util.Set;

public record TipDataQualityResponse(
        long totalTips,
        long cleanTips,
        long anomalyCount,
        long highSeverityCount,
        long warningCount,
        long infoCount,
        List<TipAnomalyRecord> anomalies,
        Set<String> currencies,
        String message
) {}
