package com.aitip.util;

import com.aitip.dto.TipBehaviorType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TipCalculationUtil {

    private TipCalculationUtil() {}

    public static BigDecimal calculateMedian(List<BigDecimal> sorted) {
        if (sorted == null || sorted.isEmpty()) return BigDecimal.ZERO;
        int size = sorted.size();
        if (size == 1) return sorted.get(0);

        if (size % 2 == 1) {
            return sorted.get(size / 2);
        } else {
            BigDecimal mid1 = sorted.get((size / 2) - 1);
            BigDecimal mid2 = sorted.get(size / 2);
            return mid1.add(mid2).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        }
    }

    public static BigDecimal calculateMean(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Standard deviation approach:
     * consistencyScore = max(0, 100 - (SD * 10))
     */
    public static int calculateConsistencyScore(List<BigDecimal> values, BigDecimal mean) {
        if (values == null || values.isEmpty()) return 0;
        if (values.size() == 1) return 100;

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal val : values) {
            BigDecimal diff = val.subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }

        BigDecimal variance = varianceSum.divide(new BigDecimal(values.size()), 4, RoundingMode.HALF_UP);
        double standardDeviation = Math.sqrt(variance.doubleValue());

        double penalty = standardDeviation * 10.0;
        int score = (int) Math.round(100.0 - penalty);
        return Math.max(0, Math.min(100, score));
    }

    public static TipBehaviorType determineBehaviorType(int consistencyScore) {
        if (consistencyScore >= 90) return TipBehaviorType.VERY_CONSISTENT;
        if (consistencyScore >= 70) return TipBehaviorType.CONSISTENT;
        if (consistencyScore >= 40) return TipBehaviorType.VARIABLE;
        return TipBehaviorType.HIGHLY_VARIABLE;
    }
}
