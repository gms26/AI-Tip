package com.aitip.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating the service quality of an existing tip.
 *
 * <p><b>Purpose:</b> Allows an authenticated user to change the service quality
 * rating they recorded on one of their own tips. Used by the
 * {@code PATCH /api/tips/{id}/service-quality} endpoint.</p>
 *
 * <p><b>Why a dedicated DTO instead of reusing CreateTipRequest?</b>
 * The PATCH endpoint updates a single field. A dedicated DTO keeps
 * the contract minimal and explicit â€” callers don't need to repeat
 * the full tip payload to update one attribute.</p>
 *
 * <p><b>Validation:</b>
 * {@code serviceQuality} must be one of POOR, AVERAGE, GOOD, EXCELLENT.
 * Invalid values will cause Jackson deserialization to fail, resulting
 * in a 400 response from {@code GlobalExceptionHandler}.</p>
 *
 * @param serviceQuality The new service quality rating
 */
public record UpdateServiceQualityRequest(

        @NotNull(message = "Service quality is required")
        ServiceQuality serviceQuality
) {
}
