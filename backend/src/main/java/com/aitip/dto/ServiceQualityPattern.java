package com.aitip.dto;

import java.math.BigDecimal;

/**
 * Service quality pattern showing tipping behavior by service quality rating.
 *
 * <p>Only service-quality categories actually present in the user's
 * tip history are included. Null service quality tips are excluded.</p>
 */
public record ServiceQualityPattern(
        ServiceQuality serviceQuality,
        int count,
        BigDecimal averageTipPercentage
) {}
