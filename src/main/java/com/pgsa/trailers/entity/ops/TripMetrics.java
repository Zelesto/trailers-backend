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

    @OneToOne
    @JoinColumn(name = "trip_id", referencedColumnName = "id")
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
       Location-based metrics (new)
       ======================== */

    @Column(name = "origin_city_travel_time_hours", precision = 10, scale = 2)
    private BigDecimal originCityTravelTimeHours = BigDecimal.ZERO;

    @Column(name = "destination_city_travel_time_hours", precision = 10, scale = 2)
    private BigDecimal destinationCityTravelTimeHours = BigDecimal.ZERO;

    @Column(name = "planned_vs_actual_distance_variance_km", precision = 10, scale = 2)
    private BigDecimal plannedVsActualDistanceVarianceKm = BigDecimal.ZERO;

    @Column(name = "planned_vs_actual_duration_variance_hours", precision = 10, scale = 2)
    private BigDecimal plannedVsActualDurationVarianceHours = BigDecimal.ZERO;

    @Column(name = "geocoding_confidence_score", precision = 5, scale = 2)
    private BigDecimal geocodingConfidenceScore = BigDecimal.ZERO;

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

    /* ========================
       Helper methods for metrics calculation
       ======================== */

    /**
     * Calculate fuel efficiency based on distance and fuel used
     * @return fuel efficiency in km per liter
     */
    public BigDecimal calculateFuelEfficiency() {
        if (fuelUsedLiters == null || fuelUsedLiters.compareTo(BigDecimal.ZERO) <= 0 ||
            totalDistanceKm == null || totalDistanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalDistanceKm.divide(fuelUsedLiters, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate fuel efficiency in liters per 100km
     * @return fuel consumption in L/100km
     */
    public BigDecimal calculateFuelConsumptionLPer100km() {
        if (totalDistanceKm == null || totalDistanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return fuelUsedLiters.multiply(BigDecimal.valueOf(100))
                .divide(totalDistanceKm, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate on-time performance as percentage
     * @param plannedHours planned duration in hours
     * @return percentage (0-100)
     */
    public BigDecimal calculateOnTimePerformance(BigDecimal plannedHours) {
        if (plannedHours == null || plannedHours.compareTo(BigDecimal.ZERO) <= 0 ||
            totalDurationHours == null) {
            return BigDecimal.valueOf(100);
        }
        
        BigDecimal variance = totalDurationHours.subtract(plannedHours);
        if (variance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(100);
        }
        
        BigDecimal performance = BigDecimal.valueOf(100).subtract(
            variance.divide(plannedHours, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        );
        
        return performance.max(BigDecimal.ZERO);
    }

    /**
     * Update variance metrics based on planned vs actual
     */
    public void updateVarianceMetrics(BigDecimal plannedDistanceKm, BigDecimal plannedDurationHours) {
        if (plannedDistanceKm != null && this.totalDistanceKm != null) {
            this.plannedVsActualDistanceVarianceKm = this.totalDistanceKm.subtract(plannedDistanceKm);
        }
        
        if (plannedDurationHours != null && this.totalDurationHours != null) {
            this.plannedVsActualDurationVarianceHours = this.totalDurationHours.subtract(plannedDurationHours);
        }
    }

    /**
     * Calculate cost per kilometer
     * @return cost per km
     */
    public BigDecimal calculateCostPerKm() {
        if (totalDistanceKm == null || totalDistanceKm.compareTo(BigDecimal.ZERO) <= 0 ||
            costAmount == null) {
            return BigDecimal.ZERO;
        }
        return costAmount.divide(totalDistanceKm, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate revenue per kilometer
     * @return revenue per km
     */
    public BigDecimal calculateRevenuePerKm() {
        if (totalDistanceKm == null || totalDistanceKm.compareTo(BigDecimal.ZERO) <= 0 ||
            revenueAmount == null) {
            return BigDecimal.ZERO;
        }
        return revenueAmount.divide(totalDistanceKm, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate profit margin as percentage
     * @return profit margin percentage
     */
    public BigDecimal calculateProfitMargin() {
        if (revenueAmount == null || revenueAmount.compareTo(BigDecimal.ZERO) <= 0 ||
            costAmount == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal profit = revenueAmount.subtract(costAmount);
        return profit.divide(revenueAmount, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Add incident and optionally adjust metrics
     */
    public void addIncident() {
        if (this.incidentCount == null) {
            this.incidentCount = 0;
        }
        this.incidentCount++;
    }

    /**
     * Complete a task and update metrics
     */
    public void completeTask() {
        if (this.tasksCompleted == null) {
            this.tasksCompleted = 0;
        }
        this.tasksCompleted++;
    }

    /**
     * Update fuel metrics
     */
    public void updateFuelMetrics(BigDecimal fuelUsedLiters, BigDecimal distanceKm) {
        this.fuelUsedLiters = fuelUsedLiters;
        if (distanceKm != null) {
            this.totalDistanceKm = distanceKm;
        }
    }

    /**
     * Check if trip is profitable
     * @return true if revenue > cost
     */
    public boolean isProfitable() {
        if (revenueAmount == null || costAmount == null) {
            return false;
        }
        return revenueAmount.compareTo(costAmount) > 0;
    }

    /**
     * Reset all metrics to zero (for recalculation)
     */
    public void resetMetrics() {
        this.totalDistanceKm = BigDecimal.ZERO;
        this.totalDurationHours = BigDecimal.ZERO;
        this.idleTimeHours = BigDecimal.ZERO;
        this.averageSpeedKmh = BigDecimal.ZERO;
        this.fuelUsedLiters = BigDecimal.ZERO;
        this.incidentCount = 0;
        this.tasksCompleted = 0;
        this.revenueAmount = BigDecimal.ZERO;
        this.costAmount = BigDecimal.ZERO;
        this.originCityTravelTimeHours = BigDecimal.ZERO;
        this.destinationCityTravelTimeHours = BigDecimal.ZERO;
        this.plannedVsActualDistanceVarianceKm = BigDecimal.ZERO;
        this.plannedVsActualDurationVarianceHours = BigDecimal.ZERO;
        this.geocodingConfidenceScore = BigDecimal.ZERO;
    }
}
