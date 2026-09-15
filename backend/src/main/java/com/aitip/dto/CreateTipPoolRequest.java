package com.aitip.dto;

import com.aitip.entity.DistributionType;

import java.math.BigDecimal;
import java.util.List;

public record CreateTipPoolRequest(
        BigDecimal totalTip,
        String currency,
        String restaurantName,
        DistributionType distributionType,
        List<PoolMemberRequest> members
) {
}
