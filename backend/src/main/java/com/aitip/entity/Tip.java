package com.aitip.entity;

import com.aitip.dto.ServiceQuality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity mapped to the {@code tips} table.
 *
 * <p><b>Purpose:</b> Represents a tip calculation saved by a user.</p>
 *
 * <p><b>Why BigDecimal?</b>
 * Floating point types (double, float) suffer from precision issues
 * during arithmetic operations. Money requires exact decimal arithmetic
 * to avoid losing pennies over time. JDBC maps NUMERIC/DECIMAL columns
 * to Java BigDecimal naturally.</p>
 *
 * <p><b>Why @ManyToOne(fetch = FetchType.LAZY)?</b>
 * Default fetch type for @ManyToOne is EAGER, which forces a JOIN
 * or a secondary SELECT every time a Tip is loaded, even if the user
 * details aren't needed. LAZY prevents N+1 query problems and improves
 * performance.</p>
 */
@Entity
@Table(name = "tips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who created this tip.
     * Mapped to user_id foreign key.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "restaurant_name")
    private String restaurantName;

    @Column(name = "bill_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal billAmount;

    @Column(name = "tip_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal tipPercentage;

    @Column(name = "tip_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal tipAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * The user's explicit rating of the service they received.
     *
     * <p><b>Why nullable?</b>
     * Tips created before Day 6 have no service quality rating.
     * NULL means "not rated" — we do NOT invent a rating from tip percentage
     * or any other heuristic. Only the user's explicit selection sets this.</p>
     *
     * <p><b>Why EnumType.STRING?</b>
     * Stores "EXCELLENT" not "3". Safe from enum reordering bugs.
     * Column V4 migration: VARCHAR(20).</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_quality", length = 20)
    private ServiceQuality serviceQuality;

    @Column(name = "created_at", nullable = false, updatable = false)
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
