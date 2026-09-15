package com.aitip.service;

import com.aitip.dto.TipBudgetRequest;
import com.aitip.dto.TipBudgetResponse;
import com.aitip.dto.TipBudgetStatusResponse;
import com.aitip.entity.TipBudget;
import com.aitip.entity.User;
import com.aitip.exception.ResourceNotFoundException;
import com.aitip.repository.TipBudgetRepository;
import com.aitip.util.CurrencyValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for tip budget CRUD operations.
 *
 * <p><b>Security:</b> User identity is always resolved from
 * {@code Authentication.getName()} via {@link UserService}.
 * No userId is ever accepted from the frontend.</p>
 */
@Service
public class TipBudgetService {

    private static final Logger log = LoggerFactory.getLogger(TipBudgetService.class);

    private final TipBudgetRepository tipBudgetRepository;
    private final TipBudgetCalculationService calculationService;
    private final UserService userService;

    public TipBudgetService(TipBudgetRepository tipBudgetRepository,
                            TipBudgetCalculationService calculationService,
                            UserService userService) {
        this.tipBudgetRepository = tipBudgetRepository;
        this.calculationService = calculationService;
        this.userService = userService;
    }

    /**
     * Creates or updates a budget for the authenticated user.
     * If a budget for the given currency already exists, it is updated.
     */
    @Transactional
    public TipBudgetResponse createOrUpdateBudget(String email, TipBudgetRequest request) {
        User user = userService.getUserByEmail(email);
        String currency = CurrencyValidationUtil.normalizeAndValidate(request.currency());

        TipBudget budget = tipBudgetRepository.findByUserIdAndCurrency(user.getId(), currency)
                .map(existing -> {
                    existing.setMonthlyLimit(request.monthlyLimit());
                    existing.setWarningThreshold(request.warningThreshold());
                    return existing;
                })
                .orElseGet(() -> TipBudget.builder()
                        .user(user)
                        .currency(currency)
                        .monthlyLimit(request.monthlyLimit())
                        .warningThreshold(request.warningThreshold())
                        .build());

        TipBudget saved = tipBudgetRepository.save(budget);
        log.debug("Budget saved for user {} / {}: limit={}, threshold={}",
                user.getId(), currency, saved.getMonthlyLimit(), saved.getWarningThreshold());

        return toResponse(saved);
    }

    /**
     * Returns all budgets for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<TipBudgetResponse> getBudgets(String email) {
        User user = userService.getUserByEmail(email);
        return tipBudgetRepository.findAllByUserIdOrderByCurrencyAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns the deterministic budget status for a specific currency.
     */
    @Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
    public TipBudgetStatusResponse getBudgetStatus(String email, String currency) {
        User user = userService.getUserByEmail(email);
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);

        TipBudget budget = tipBudgetRepository.findByUserIdAndCurrency(user.getId(), normalizedCurrency)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No budget found for currency: " + normalizedCurrency));

        return calculationService.calculateStatus(budget, user.getId());
    }

    /**
     * Deletes the budget for a specific currency.
     */
    @Transactional
    public void deleteBudget(String email, String currency) {
        User user = userService.getUserByEmail(email);
        String normalizedCurrency = CurrencyValidationUtil.normalizeAndValidate(currency);

        if (tipBudgetRepository.findByUserIdAndCurrency(user.getId(), normalizedCurrency).isEmpty()) {
            throw new ResourceNotFoundException("No budget found for currency: " + normalizedCurrency);
        }

        tipBudgetRepository.deleteByUserIdAndCurrency(user.getId(), normalizedCurrency);
        log.debug("Budget deleted for user {} / {}", user.getId(), normalizedCurrency);
    }

    private TipBudgetResponse toResponse(TipBudget budget) {
        return new TipBudgetResponse(
                budget.getId(),
                budget.getCurrency(),
                budget.getMonthlyLimit(),
                budget.getWarningThreshold(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
