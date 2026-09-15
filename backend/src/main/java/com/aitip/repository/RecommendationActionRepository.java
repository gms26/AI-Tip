package com.aitip.repository;

import com.aitip.entity.RecommendationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationActionRepository extends JpaRepository<RecommendationAction, Long> {

    @Query("SELECT ra FROM RecommendationAction ra WHERE ra.user.id = :userId ORDER BY ra.createdAt DESC")
    List<RecommendationAction> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT ra FROM RecommendationAction ra WHERE ra.user.id = :userId AND ra.recommendationType = :type AND (ra.currency = :currency OR (ra.currency IS NULL AND :currency IS NULL)) ORDER BY ra.createdAt DESC")
    List<RecommendationAction> findLatestActions(@Param("userId") UUID userId, @Param("type") com.aitip.dto.TipRecommendationType type, @Param("currency") String currency);
}
