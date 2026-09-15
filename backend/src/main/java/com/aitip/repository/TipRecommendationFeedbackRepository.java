package com.aitip.repository;

import com.aitip.dto.SmartTipFeedbackType;
import com.aitip.entity.TipRecommendationFeedback;
import com.aitip.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for persisting and querying Smart Tip recommendation feedback (Day 32).
 *
 * <p>All queries enforce strict User and Currency scoping to guarantee user and currency isolation.</p>
 */
@Repository
public interface TipRecommendationFeedbackRepository extends JpaRepository<TipRecommendationFeedback, UUID> {

    List<TipRecommendationFeedback> findAllByUserAndCurrencyOrderByCreatedAtAsc(User user, String currency);

    List<TipRecommendationFeedback> findAllByUserAndCurrencyOrderByCreatedAtDesc(User user, String currency);

    long countByUserAndCurrency(User user, String currency);

    long countByUserAndCurrencyAndFeedbackType(User user, String currency, SmartTipFeedbackType feedbackType);

    void deleteAllByUser(User user);

    long deleteAllByUserAndCurrency(User user, String currency);

    List<TipRecommendationFeedback> findTop5ByUserAndCurrencyOrderByCreatedAtDesc(User user, String currency);

    @Query("SELECT MIN(f.chosenTipPercentage) FROM TipRecommendationFeedback f WHERE f.user = :user AND f.currency = :currency")
    BigDecimal findMinChosenPercentageByUserAndCurrency(@Param("user") User user, @Param("currency") String currency);

    @Query("SELECT MAX(f.chosenTipPercentage) FROM TipRecommendationFeedback f WHERE f.user = :user AND f.currency = :currency")
    BigDecimal findMaxChosenPercentageByUserAndCurrency(@Param("user") User user, @Param("currency") String currency);

    @Query("SELECT AVG(f.chosenTipPercentage) FROM TipRecommendationFeedback f WHERE f.user = :user AND f.currency = :currency")
    BigDecimal findAverageChosenPercentageByUserAndCurrency(@Param("user") User user, @Param("currency") String currency);
}
