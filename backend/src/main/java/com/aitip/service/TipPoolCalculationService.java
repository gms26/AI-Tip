package com.aitip.service;

import com.aitip.dto.PoolMemberRequest;
import com.aitip.dto.PoolMemberResponse;
import com.aitip.entity.DistributionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TipPoolCalculationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Entry point to calculate the distribution independently of JPA.
     */
    public List<PoolMemberResponse> calculateDistribution(BigDecimal totalTip, DistributionType type, List<PoolMemberRequest> members) {
        validateInputs(totalTip, members);

        if (type == DistributionType.EQUAL) {
            return calculateEqualDistribution(totalTip, members);
        } else {
            return calculatePercentageDistribution(totalTip, members);
        }
    }

    private void validateInputs(BigDecimal totalTip, List<PoolMemberRequest> members) {
        if (totalTip == null || totalTip.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total tip must be strictly greater than 0.");
        }

        if (members == null || members.size() < 2 || members.size() > 20) {
            throw new IllegalArgumentException("Pool must have between 2 and 20 members.");
        }

        Set<String> uniqueNames = new HashSet<>();
        for (PoolMemberRequest member : members) {
            if (member.name() == null || member.name().trim().isEmpty()) {
                throw new IllegalArgumentException("Member name cannot be blank.");
            }
            if (member.name().length() > 100) {
                throw new IllegalArgumentException("Member name exceeds maximum length of 100.");
            }
            
            // Case-insensitive duplicate check
            String normalizedName = member.name().trim().toLowerCase();
            if (!uniqueNames.add(normalizedName)) {
                throw new IllegalArgumentException("Duplicate member name found: " + member.name());
            }
        }
    }

    private List<PoolMemberResponse> calculateEqualDistribution(BigDecimal totalTip, List<PoolMemberRequest> members) {
        int count = members.size();
        BigDecimal countDec = BigDecimal.valueOf(count);

        // Calculate base equal percentage: 100 / count
        BigDecimal basePercentage = ONE_HUNDRED.divide(countDec, 5, RoundingMode.DOWN);
        
        // Calculate base equal amount: totalTip / count
        BigDecimal baseAmount = totalTip.divide(countDec, 2, RoundingMode.DOWN);

        List<PoolMemberResponse> responses = new ArrayList<>();
        BigDecimal sumAmounts = BigDecimal.ZERO;
        BigDecimal sumPercentages = BigDecimal.ZERO;

        // Give base amounts and percentages to everyone initially
        for (PoolMemberRequest member : members) {
            responses.add(new PoolMemberResponse(member.name().trim(), basePercentage, baseAmount));
            sumAmounts = sumAmounts.add(baseAmount);
            sumPercentages = sumPercentages.add(basePercentage);
        }

        // Reconcile amount remainder
        BigDecimal amountRemainder = totalTip.subtract(sumAmounts);
        if (amountRemainder.compareTo(BigDecimal.ZERO) != 0) {
            PoolMemberResponse first = responses.get(0);
            responses.set(0, new PoolMemberResponse(
                    first.name(),
                    first.allocationPercentage(),
                    first.allocatedAmount().add(amountRemainder)
            ));
        }

        // Reconcile percentage remainder
        BigDecimal percentageRemainder = ONE_HUNDRED.subtract(sumPercentages);
        if (percentageRemainder.compareTo(BigDecimal.ZERO) != 0) {
            PoolMemberResponse first = responses.get(0);
            responses.set(0, new PoolMemberResponse(
                    first.name(),
                    first.allocationPercentage().add(percentageRemainder),
                    first.allocatedAmount() // Already reconciled amount above
            ));
        }

        return responses;
    }

    private List<PoolMemberResponse> calculatePercentageDistribution(BigDecimal totalTip, List<PoolMemberRequest> members) {
        BigDecimal sumPercentages = BigDecimal.ZERO;
        for (PoolMemberRequest member : members) {
            if (member.allocationPercentage() == null) {
                throw new IllegalArgumentException("Allocation percentage is required for PERCENTAGE distribution.");
            }
            if (member.allocationPercentage().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Allocation percentage cannot be negative.");
            }
            sumPercentages = sumPercentages.add(member.allocationPercentage());
        }

        // Validate strictly equals 100.00
        if (sumPercentages.compareTo(ONE_HUNDRED) != 0) {
            throw new IllegalArgumentException("Total allocation percentages must equal exactly 100. Provided: " + sumPercentages);
        }

        List<PoolMemberResponse> responses = new ArrayList<>();
        BigDecimal sumAmounts = BigDecimal.ZERO;

        for (PoolMemberRequest member : members) {
            BigDecimal amount = totalTip.multiply(member.allocationPercentage())
                    .divide(ONE_HUNDRED, 2, RoundingMode.DOWN);
            
            responses.add(new PoolMemberResponse(member.name().trim(), member.allocationPercentage(), amount));
            sumAmounts = sumAmounts.add(amount);
        }

        // Reconcile amount remainder (caused by rounding on small percentages)
        BigDecimal amountRemainder = totalTip.subtract(sumAmounts);
        if (amountRemainder.compareTo(BigDecimal.ZERO) != 0) {
            PoolMemberResponse first = responses.get(0);
            responses.set(0, new PoolMemberResponse(
                    first.name(),
                    first.allocationPercentage(),
                    first.allocatedAmount().add(amountRemainder)
            ));
        }

        return responses;
    }
}
