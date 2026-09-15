package com.aitip.entity;

import com.aitip.dto.TipRecommendationAction;
import com.aitip.dto.TipRecommendationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_actions")
@Getter
@Setter
public class RecommendationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", nullable = false)
    private TipRecommendationType recommendationType;

    @Column(name = "currency", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private TipRecommendationAction action;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "snoozed_until")
    private LocalDateTime snoozedUntil;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
