package com.aitip.repository;

import com.aitip.entity.TipPersonalizationPreference;
import com.aitip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Smart Tip personalization preferences (Day 36).
 *
 * <p>All queries are scoped to a specific User and Currency.</p>
 */
@Repository
public interface TipPersonalizationPreferenceRepository extends JpaRepository<TipPersonalizationPreference, UUID> {

    Optional<TipPersonalizationPreference> findByUserAndCurrency(User user, String currency);
}
