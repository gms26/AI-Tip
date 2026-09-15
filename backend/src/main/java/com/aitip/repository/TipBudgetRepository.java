package com.aitip.repository;

import com.aitip.entity.TipBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TipBudget} entities.
 *
 * <p><b>Security Pattern:</b>
 * Every query requires {@code userId} to enforce user-level data isolation.
 * A user can never access or modify another user's budgets.</p>
 */
@Repository
public interface TipBudgetRepository extends JpaRepository<TipBudget, UUID> {

    Optional<TipBudget> findByUserIdAndCurrency(UUID userId, String currency);

    List<TipBudget> findAllByUserIdOrderByCurrencyAsc(UUID userId);

    void deleteByUserIdAndCurrency(UUID userId, String currency);
}
