package com.aitip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipTimingRequest {
    private BigDecimal billAmount;
    private BigDecimal tipPercentage;
    private String restaurantName;
    private ServiceQuality serviceQuality;
    private Boolean saved;
}
