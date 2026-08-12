package com.aitip.service;

import com.aitip.dto.CurrencyTipSummary;
import com.aitip.dto.TaxPeriod;
import com.aitip.dto.TaxSummaryRequest;
import com.aitip.dto.TaxSummaryResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxSummaryService {

    private final TipRepository tipRepository;
    private final UserService userService;

    private static final String DISCLAIMER = "This summary is for personal tracking and estimation only. Tax treatment varies by jurisdiction and individual circumstances. Consult a qualified tax professional or official tax authority for tax advice.";

    @Transactional(readOnly = true)
    public TaxSummaryResponse getSummary(String email, TaxSummaryRequest request) {
        User user = userService.getUserByEmail(email);

        // 1. Validate taxable percentage
        BigDecimal percentage = request.taxablePercentage();
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Taxable percentage must be between 0 and 100");
        }

        // 2. Resolve Dates
        LocalDateTime start;
        LocalDateTime end; // Exclusive
        LocalDate resolvedStart;
        LocalDate resolvedEnd;

        LocalDate now = LocalDate.now();

        if (request.period() == TaxPeriod.CURRENT_MONTH) {
            resolvedStart = now.withDayOfMonth(1);
            resolvedEnd = YearMonth.from(now).atEndOfMonth();
            start = resolvedStart.atStartOfDay();
            end = resolvedEnd.plusDays(1).atStartOfDay();
        } else if (request.period() == TaxPeriod.PREVIOUS_MONTH) {
            LocalDate prevMonth = now.minusMonths(1);
            resolvedStart = prevMonth.withDayOfMonth(1);
            resolvedEnd = YearMonth.from(prevMonth).atEndOfMonth();
            start = resolvedStart.atStartOfDay();
            end = resolvedEnd.plusDays(1).atStartOfDay();
        } else if (request.period() == TaxPeriod.CURRENT_YEAR) {
            resolvedStart = now.withDayOfYear(1);
            resolvedEnd = LocalDate.of(now.getYear(), 12, 31);
            start = resolvedStart.atStartOfDay();
            end = resolvedEnd.plusDays(1).atStartOfDay();
        } else if (request.period() == TaxPeriod.CUSTOM) {
            if (request.startDate() == null || request.endDate() == null) {
                throw new IllegalArgumentException("Start date and end date are required for CUSTOM period");
            }
            if (request.startDate().isAfter(request.endDate())) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
            resolvedStart = request.startDate();
            resolvedEnd = request.endDate();
            start = resolvedStart.atStartOfDay();
            end = resolvedEnd.plusDays(1).atStartOfDay();
        } else {
            throw new IllegalArgumentException("Invalid Tax Period");
        }

        // 3. Fetch tips
        List<Tip> tips = tipRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(user.getId(), start, end);

        // 4. Handle Empty State
        if (tips.isEmpty()) {
            return new TaxSummaryResponse(
                    request.period(),
                    resolvedStart,
                    resolvedEnd,
                    BigDecimal.ZERO, // totalTips
                    0,               // tipCount
                    null,            // averageTip
                    null,            // medianTip
                    BigDecimal.ZERO, // estimatedTaxableTips
                    percentage,
                    Collections.emptyList(), // currencyBreakdown
                    DISCLAIMER
            );
        }

        // 5. Group by Currency
        Map<String, List<Tip>> tipsByCurrency = tips.stream()
                .collect(Collectors.groupingBy(Tip::getCurrency));

        List<CurrencyTipSummary> breakdowns = new ArrayList<>();
        
        for (Map.Entry<String, List<Tip>> entry : tipsByCurrency.entrySet()) {
            String currency = entry.getKey();
            List<Tip> currencyTips = entry.getValue();
            
            int tipCount = currencyTips.size();
            
            BigDecimal totalTips = currencyTips.stream()
                    .map(Tip::getTipAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            BigDecimal averageTip = totalTips.divide(BigDecimal.valueOf(tipCount), 2, RoundingMode.HALF_UP);
            BigDecimal medianTip = calculateMedian(currencyTips);
            
            BigDecimal estimatedTaxableTips = totalTips.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            
            breakdowns.add(new CurrencyTipSummary(
                    currency,
                    tipCount,
                    totalTips,
                    averageTip,
                    medianTip,
                    estimatedTaxableTips
            ));
        }

        // 6. Handle Single vs Multiple Currencies
        if (breakdowns.size() == 1) {
            CurrencyTipSummary summary = breakdowns.get(0);
            return new TaxSummaryResponse(
                    request.period(),
                    resolvedStart,
                    resolvedEnd,
                    summary.totalTips(),
                    summary.tipCount(),
                    summary.averageTip(),
                    summary.medianTip(),
                    summary.estimatedTaxableTips(),
                    percentage,
                    breakdowns,
                    DISCLAIMER
            );
        } else {
            return new TaxSummaryResponse(
                    request.period(),
                    resolvedStart,
                    resolvedEnd,
                    null, // totalTips
                    null, // tipCount
                    null, // averageTip
                    null, // medianTip
                    null, // estimatedTaxableTips
                    percentage,
                    breakdowns,
                    DISCLAIMER
            );
        }
    }

    private BigDecimal calculateMedian(List<Tip> tips) {
        List<BigDecimal> amounts = tips.stream()
                .map(Tip::getTipAmount)
                .sorted()
                .toList();

        int size = amounts.size();
        if (size % 2 == 1) {
            return amounts.get(size / 2);
        } else {
            BigDecimal mid1 = amounts.get(size / 2 - 1);
            BigDecimal mid2 = amounts.get(size / 2);
            return mid1.add(mid2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }
}
