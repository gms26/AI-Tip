package com.aitip.dto;

import com.aitip.entity.DistributionType;
import com.aitip.entity.TipPoolStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TipPoolResponse(
        UUID id,
        String restaurantName,
        BigDecimal totalTip,
        String currency,
        DistributionType distributionType,
        TipPoolStatus status,
        List<PoolMemberResponse> members,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
