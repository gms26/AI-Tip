package com.aitip.dto;

import java.math.BigDecimal;

public record PoolMemberRequest(
        String name,
        BigDecimal allocationPercentage
) {
}
