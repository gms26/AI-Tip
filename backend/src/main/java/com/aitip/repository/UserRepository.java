package com.aitip.repository;

import com.aitip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p><b>Purpose:</b> Data access layer for user CRUD operations.
 * Spring Data auto-generates the implementation at runtime.</p>
 *
 * <p><b>Why JpaRepository over CrudRepository?</b>
 * JpaRepository extends CrudRepository + PagingAndSortingRepository.
 * Provides flush(), saveAndFlush(), and pagination â€” useful
 * for future user listing features.</p>
 *
 * <p><b>Why Optional for findByEmail?</b>
 * Forces callers to handle the "not found" case explicitly.
 * Prevents NullPointerException bugs.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address (case-sensitive).
     * Used by authentication to load user credentials.
     *
     * @param email the user's email
     * @return the user, if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered.
     * Used during registration to prevent duplicates.
     *
     * <p><b>Why a separate method instead of findByEmail().isPresent()?</b>
     * existsBy generates a more efficient SQL query (SELECT 1 ... LIMIT 1)
     * instead of loading the entire entity.</p>
     *
     * @param email the email to check
     * @return true if the email exists
     */
    boolean existsByEmail(String email);
}
