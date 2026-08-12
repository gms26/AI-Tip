package com.aitip.service;

import com.aitip.dto.SplitRequest;
import com.aitip.dto.SplitResponse;
import com.aitip.exception.InvalidSplitException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for pure tip and split calculations.
 *
 * <p><b>Purpose:</b> Isolates calculation logic from CRUD operations.
 * Makes unit testing easy and allows for future AI or analytics
 * features to reuse these calculations.</p>
 */
@Service
public class TipCalculationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Calculates the tip amount based on bill and percentage.
     * RoundingMode.HALF_UP ensures standard financial rounding to 2 decimals.
     *
     * @param billAmount    The total bill
     * @param tipPercentage The tip percentage (e.g., 20.00)
     * @return Calculated tip amount
     */
    public BigDecimal calculateTipAmount(BigDecimal billAmount, BigDecimal tipPercentage) {
        return billAmount.multiply(tipPercentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the total amount (bill + tip).
     *
     * @param billAmount The total bill
     * @param tipAmount  The calculated tip amount
     * @return Total amount
     */
    public BigDecimal calculateTotalAmount(BigDecimal billAmount, BigDecimal tipAmount) {
        return billAmount.add(tipAmount);
    }

    /**
     * Performs a split calculation (equal or custom).
     *
     * @param request The split request containing either 'people' or 'customSplit'
     * @return The split response with detailed breakdown
     */
    public SplitResponse calculateSplit(SplitRequest request) {
        BigDecimal tipAmount = calculateTipAmount(request.billAmount(), request.tipPercentage());
        BigDecimal totalAmount = calculateTotalAmount(request.billAmount(), tipAmount);

        List<SplitResponse.PersonSplit> splits = new ArrayList<>();
        int numberOfPeople;

        // Custom Split
        if (request.customSplit() != null && !request.customSplit().isEmpty()) {
            Map<String, BigDecimal> customSplit = request.customSplit();
            numberOfPeople = customSplit.size();
            
            if (numberOfPeople < 2) {
                throw new InvalidSplitException("Custom split requires at least 2 people.");
            }

            BigDecimal totalCustomAmount = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : customSplit.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                     throw new InvalidSplitException("Split amount for " + entry.getKey() + " cannot be negative.");
                }
                totalCustomAmount = totalCustomAmount.add(entry.getValue());
                splits.add(new SplitResponse.PersonSplit(entry.getKey(), entry.getValue()));
            }

            // Validate custom split adds up exactly to the total amount
            if (totalCustomAmount.compareTo(totalAmount) != 0) {
                throw new InvalidSplitException(
                        String.format("Custom split total (%s) does not match the calculated total bill with tip (%s).", 
                        totalCustomAmount, totalAmount)
                );
            }

        } 
        // Equal Split
        else if (request.people() != null && request.people().size() >= 2) {
            numberOfPeople = request.people().size();
            BigDecimal divisor = new BigDecimal(numberOfPeople);
            
            // Standard financial rounding for division
            BigDecimal equalShare = totalAmount.divide(divisor, 2, RoundingMode.HALF_UP);
            
            // Handle rounding remainders (e.g. 10.00 / 3 = 3.33 * 3 = 9.99, remainder 0.01)
            BigDecimal totalAssigned = equalShare.multiply(divisor);
            BigDecimal remainder = totalAmount.subtract(totalAssigned);

            for (int i = 0; i < numberOfPeople; i++) {
                BigDecimal share = equalShare;
                // Add the remainder (positive or negative) to the first person's share to balance
                if (i == 0 && remainder.compareTo(BigDecimal.ZERO) != 0) {
                    share = share.add(remainder);
                }
                splits.add(new SplitResponse.PersonSplit(request.people().get(i), share));
            }
        } 
        else {
            throw new InvalidSplitException("Split requires either a list of at least 2 people or a custom split map.");
        }

        return new SplitResponse(
                request.billAmount(),
                tipAmount,
                totalAmount,
                request.tipPercentage(),
                numberOfPeople,
                splits
        );
    }
}
