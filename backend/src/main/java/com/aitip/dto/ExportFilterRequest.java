package com.aitip.dto;

import java.time.LocalDateTime;

public record ExportFilterRequest(
        LocalDateTime startDate,
        LocalDateTime endDate,
        String restaurantName,
        String currency,
        ServiceQuality serviceQuality
) {}
