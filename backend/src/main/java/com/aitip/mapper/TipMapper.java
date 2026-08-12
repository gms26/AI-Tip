package com.aitip.mapper;

import com.aitip.dto.CreateTipRequest;
import com.aitip.dto.TipResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;

import java.math.BigDecimal;

/**
 * Centralized mapping between Tip entity and DTOs.
 *
 * <p><b>Purpose:</b> Pure data transformation. Contains NO business logic
 * (like calculating tip amounts). The service performs calculations
 * and passes the results to the mapper.</p>
 */
public final class TipMapper {

    private TipMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a request to a Tip entity.
     *
     * @param request     The validated request
     * @param user        The authenticated user
     * @param tipAmount   Calculated tip amount
     * @param totalAmount Calculated total amount
     * @return Tip entity ready to be saved
     */
    public static Tip toEntity(CreateTipRequest request, User user, BigDecimal tipAmount, BigDecimal totalAmount) {
        return Tip.builder()
                .user(user)
                .restaurantName(request.restaurantName())
                .billAmount(request.billAmount())
                .tipPercentage(request.tipPercentage())
                .tipAmount(tipAmount)
                .totalAmount(totalAmount)
                .currency(request.currency())
                .serviceQuality(request.serviceQuality())
                .build();
    }

    /**
     * Converts a Tip entity to a response DTO.
     *
     * <p><b>Day 6:</b> {@code serviceQuality} may be null for historical tips.
     * The record field is nullable and clients must handle null gracefully.</p>
     *
     * @param tip The saved tip
     * @return Safe response without user info
     */
    public static TipResponse toTipResponse(Tip tip) {
        return new TipResponse(
                tip.getId(),
                tip.getRestaurantName(),
                tip.getBillAmount(),
                tip.getTipPercentage(),
                tip.getTipAmount(),
                tip.getTotalAmount(),
                tip.getCurrency(),
                tip.getCreatedAt(),
                tip.getServiceQuality()
        );
    }
}
