package com.aitip.dto;

import java.math.BigDecimal;

public record PoolMemberResponse(
        String name,
        BigDecimal allocationPercentage,
        BigDecimal allocatedAmount
) {
}
