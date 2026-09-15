package com.aitip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing a user's per-currency personalization preference (Day 36).
 *
 * <p>Controls whether feedback-based adaptation is used in Smart Tip recommendations.
 * Missing rows default to enabled at the application level.</p>
 */
@Entity
@Table(name = "tip_personalization_preferences",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "currency"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipPersonalizationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "personalization_enabled", nullable = false)
    private boolean personalizationEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
