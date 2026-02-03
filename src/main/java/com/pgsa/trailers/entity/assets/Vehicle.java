package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.VehicleType;
import com.pgsa.trailers.helpers.JsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@Entity
@Table(
        name = "vehicle",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vehicle_registration", columnNames = {"registration_number"})
        }
)
public class Vehicle extends BaseEntity {

    @Column(name = "registration_number", unique = true, nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "vin", length = 50)
    private String vin;

    @Column(name = "make", length = 100)
    private String make;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "year")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;



    @Column(name = "fuel_type", length = 20)
    private String fuelType;

    @Column(name = "current_mileage", precision = 12, scale = 2)
    private BigDecimal currentMileage;

    @Column(name = "avg_consumption", precision = 12, scale = 2)
    private BigDecimal avgConsumption;

    @Column(name = "current_odometer", precision = 12, scale = 2)
    private BigDecimal currentOdometer;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "last_service_odometer", precision = 12, scale = 2)
    private BigDecimal lastServiceOdometer;

    @Column(name = "service_interval_days")
    private Integer serviceIntervalDays;

    @Column(name = "service_interval_km")
    private Integer serviceIntervalKm;

    @Column(name = "insurance_policy_number", length = 100)
    private String insurancePolicyNumber;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "roadworthy_expiry")
    private LocalDate roadworthyExpiry;

    @Column(name = "fleet_number", length = 50)
    private String fleetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id", foreignKey = @ForeignKey(name = "fk_vehicle_driver"))
    private Driver assignedDriver;

    @Column(name = "gps_tracker_id")
    private Long gpsTrackerId;

    @Column(name = "maintenance_status", length = 50)
    private String maintenanceStatus;

    @Column(name = "next_service_due")
    private LocalDate nextServiceDue;

    @Column(name = "next_service_odometer", precision = 12, scale = 2)
    private BigDecimal nextServiceOdometer;

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Convert(converter = JsonConverter.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "category")
    private String category; // Keep only category to match DB

    // ====== Lifecycle Hooks ======
    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null) setCreatedAt(LocalDateTime.now());
        if (getUpdatedAt() == null) setUpdatedAt(getCreatedAt());
        if (nextServiceDue == null) calculateNextService();
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }

    // ====== Helper Methods ======
    public void calculateNextService() {
        if (lastServiceDate != null && serviceIntervalDays != null) {
            this.nextServiceDue = lastServiceDate.plusDays(serviceIntervalDays);
        }
        if (lastServiceOdometer != null && serviceIntervalKm != null) {
            this.nextServiceOdometer = lastServiceOdometer.add(BigDecimal.valueOf(serviceIntervalKm));
        }
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean isAvailable() {
        return isActive() &&
                (assignedDriver == null) &&
                (insuranceExpiry == null || !insuranceExpiry.isBefore(LocalDate.now())) &&
                (roadworthyExpiry == null || !roadworthyExpiry.isBefore(LocalDate.now()));
    }

    public void incrementIncidents() {
        this.incidentsLogged = (incidentsLogged == null ? 0 : incidentsLogged) + 1;
    }

}
