package com.pgsa.trailers.entity.assets;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_metrics")
@Getter
@Setter
public class VehicleMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK to vehicle
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false,
            foreignKey = @ForeignKey(name = "vehicle_metrics_vehicle_id_fkey"))
    private Vehicle vehicle;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "distance_traveled")
    private Double distanceTraveled;

    @Column(name = "fuel_used")
    private Double fuelUsed;

    @Column(name = "fuel_efficiency")
    private Double fuelEfficiency;

    @Column(name = "maintenance_cost")
    private Double maintenanceCost;

    @Column(name = "downtime_hours")
    private Double downtimeHours;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
