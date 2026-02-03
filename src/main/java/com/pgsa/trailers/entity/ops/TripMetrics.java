package com.pgsa.trailers.entity.ops;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "trip_metrics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_metrics_trip", columnNames = "trip_id")
        },
        indexes = {
                @Index(name = "idx_trip_metrics_trip", columnList = "trip_id")
        }
)
public class TripMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========================
       Relationship (owner side)
       ======================== */

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    /* ========================
       Distance & time
       ======================== */

    @Column(name = "total_distance_km", precision = 10, scale = 2)
    private BigDecimal totalDistanceKm = BigDecimal.ZERO;

    @Column(name = "total_duration_hours", precision = 10, scale = 2)
    private BigDecimal totalDurationHours = BigDecimal.ZERO;

    @Column(name = "idle_time_hours", precision = 10, scale = 2)
    private BigDecimal idleTimeHours = BigDecimal.ZERO;

    @Column(name = "average_speed_kmh", precision = 10, scale = 2)
    private BigDecimal averageSpeedKmh = BigDecimal.ZERO;

    /* ========================
       Fuel
       ======================== */

    @Column(name = "fuel_used_liters", precision = 10, scale = 2)
    private BigDecimal fuelUsedLiters = BigDecimal.ZERO;

    /* ========================
       Activity (derived)
       ======================== */

    @Column(name = "incident_count", nullable = false)
    private Integer incidentCount = 0;

    @Column(name = "tasks_completed", nullable = false)
    private Integer tasksCompleted = 0;

    /* ========================
       Financial (aggregated)
       ======================== */

    @Column(name = "revenue_amount", precision = 15, scale = 2)
    private BigDecimal revenueAmount = BigDecimal.ZERO;

    @Column(name = "cost_amount", precision = 15, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    /* ========================
       Audit
       ======================== */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "finalized", nullable = false)
    private boolean finalized = false;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
