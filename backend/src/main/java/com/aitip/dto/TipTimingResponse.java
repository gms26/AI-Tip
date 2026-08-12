package com.aitip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipTimingResponse {
    private TipTimingState state;
    private String message;
    private String confidence; // LOW, MEDIUM, HIGH
    private String restaurantName;
    private Integer visitCount;
    private LocalDateTime lastVisitAt;
    private BigDecimal lastTipPercentage;
    private BigDecimal medianTipPercentage;
    private Boolean hasRestaurantHistory;
    private String reason;
}
