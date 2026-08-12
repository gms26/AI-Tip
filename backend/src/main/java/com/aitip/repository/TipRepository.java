package com.aitip.repository;

import com.aitip.entity.Tip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Tip} entities.
 *
 * <p><b>Purpose:</b> Data access layer for tip operations.</p>
 *
 * <p><b>Security Pattern:</b>
 * Notice that almost all methods require `userId`. This ensures
 * that a user can NEVER access or modify another user's tips,
 * even if they guess a valid tip UUID. Authorization is enforced
 * at the query level.</p>
 */
@Repository
public interface TipRepository extends JpaRepository<Tip, UUID> {

    /**
     * Finds all tips for a specific user with pagination support.
     * Ordered automatically if Sort is provided in Pageable.
     *
     * @param userId The owner's UUID
     * @param pageable Pagination and sorting information
     * @return A page of tips
     */
    Page<Tip> findByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a specific tip, ensuring it belongs to the specified user.
     *
     * @param id The tip UUID
     * @param userId The owner's UUID
     * @return The tip if found and owned by user
     */
    Optional<Tip> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks if a tip exists and belongs to the specified user.
     * Efficient EXISTS query for permission checks.
     *
     * @param id The tip UUID
     * @param userId The owner's UUID
     * @return true if it exists and belongs to user
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);

    // =============================================
    // Day 4: Personalization queries
    // =============================================

    /**
     * Retrieves all tips for a user (unpaginated) for aggregate statistics.
     *
     * <p><b>Scalability note:</b> For users with thousands of tips, this
     * loads all records into memory. For the current scale this is acceptable.
     * If performance becomes an issue, replace with @Query aggregate functions.</p>
     */
    List<Tip> findAllByUserId(UUID userId);

    /**
     * Finds tips for a specific restaurant, case-insensitive.
     * Uses the V3 composite index on (user_id, restaurant_name).
     *
     * @param userId The owner's UUID
     * @param restaurantName The restaurant name (case-insensitive match)
     * @return List of tips at that restaurant
     */
    List<Tip> findByUserIdAndRestaurantNameIgnoreCase(UUID userId, String restaurantName);

    // =============================================
    // Day 6: Service quality statistics queries
    // =============================================

    /**
     * Retrieves all rated tips (where service_quality IS NOT NULL) for a user.
     *
     * <p><b>Purpose:</b> Used by {@code ServiceQualityService} to calculate
     * statistics. Tips with null service quality are historical pre-Day-6 records
     * and must be excluded from all service quality statistics.</p>
     *
     * <p><b>Index:</b> Uses V5 composite index on (user_id, service_quality)
     * for efficient filtering.</p>
     *
     * @param userId The owner's UUID
     * @return List of tips that have a non-null service quality rating
     */
    List<Tip> findByUserIdAndServiceQualityIsNotNull(UUID userId);

    /**
     * Finds tips for a specific restaurant and service quality.
     */
    List<Tip> findByUserIdAndRestaurantNameIgnoreCaseAndServiceQuality(UUID userId, String restaurantName, com.aitip.dto.ServiceQuality serviceQuality);

    /**
     * Finds tips for a specific service quality.
     */
    List<Tip> findByUserIdAndServiceQuality(UUID userId, com.aitip.dto.ServiceQuality serviceQuality);

    // =============================================
    // Day 10: Tax Tracking queries
    // =============================================

    /**
     * Retrieves tips for a user within a specific date range.
     * Uses inclusive start and exclusive end to correctly handle end-of-day boundaries.
     */
    List<Tip> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(UUID userId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
