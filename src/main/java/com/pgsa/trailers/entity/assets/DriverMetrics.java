package com.pgsa.trailers.entity.assets;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "driver_metrics")
public class DriverMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "metric_date")
    private LocalDateTime metricDate;

    @Column(name = "total_trips")
    private Integer totalTrips;

    @Column(name = "total_distance_km")
    private Double totalDistanceKm;

    @Column(name = "total_hours")
    private Double totalHours;

    @Column(name = "fuel_consumption_liters")
    private Double fuelConsumptionLiters;

    @Column(name = "average_speed_kmh")
    private Double averageSpeedKmh;

    @Column(name = "hard_braking_count")
    private Integer hardBrakingCount;

    @Column(name = "rapid_acceleration_count")
    private Integer rapidAccelerationCount;

    @Column(name = "safety_score")
    private Double safetyScore;

    @Column(name = "efficiency_score")
    private Double efficiencyScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public DriverMetrics() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public DriverMetrics(Long driverId, LocalDateTime metricDate) {
        this();
        this.driverId = driverId;
        this.metricDate = metricDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public LocalDateTime getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDateTime metricDate) {
        this.metricDate = metricDate;
    }

    public Integer getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Integer totalTrips) {
        this.totalTrips = totalTrips;
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }

    public Double getFuelConsumptionLiters() {
        return fuelConsumptionLiters;
    }

    public void setFuelConsumptionLiters(Double fuelConsumptionLiters) {
        this.fuelConsumptionLiters = fuelConsumptionLiters;
    }

    public Double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    public void setAverageSpeedKmh(Double averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public Integer getHardBrakingCount() {
        return hardBrakingCount;
    }

    public void setHardBrakingCount(Integer hardBrakingCount) {
        this.hardBrakingCount = hardBrakingCount;
    }

    public Integer getRapidAccelerationCount() {
        return rapidAccelerationCount;
    }

    public void setRapidAccelerationCount(Integer rapidAccelerationCount) {
        this.rapidAccelerationCount = rapidAccelerationCount;
    }

    public Double getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(Double safetyScore) {
        this.safetyScore = safetyScore;
    }

    public Double getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(Double efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Pre-update hook
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}