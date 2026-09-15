package com.aitip.entity;

import com.aitip.dto.ServiceQuality;
import com.aitip.dto.SmartTipFeedbackType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing user feedback on an advisory Smart Tip recommendation (Day 32).
 *
 * <p>Persisted only upon explicit successful save of a tip. Captures whether the user
 * accepted, modified, or entered a custom tip, along with recommendation context.</p>
 */
@Entity
@Table(name = "tip_recommendation_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipRecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "restaurant_name", length = 255)
    private String restaurantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_quality", length = 30)
    private ServiceQuality serviceQuality;

    @Column(name = "bill_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal billAmount;

    @Column(name = "suggested_tip_percentage", precision = 5, scale = 2)
    private BigDecimal suggestedTipPercentage;

    @Column(name = "chosen_tip_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal chosenTipPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private SmartTipFeedbackType feedbackType;

    @Column(name = "recommendation_type", length = 50)
    private String recommendationType;

    @Column(name = "difference_percentage_points", precision = 5, scale = 2)
    private BigDecimal differencePercentagePoints;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
