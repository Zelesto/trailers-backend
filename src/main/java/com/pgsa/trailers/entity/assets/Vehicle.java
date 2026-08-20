package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.config.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Slf4j
@Table(
        name = "vehicle",
        indexes = {
                @Index(name = "idx_vehicle_registration", columnList = "registration_number"),
                @Index(name = "idx_vehicle_vin", columnList = "vin"),
                @Index(name = "idx_vehicle_fleet_number", columnList = "fleet_number"),
                @Index(name = "idx_vehicle_status", columnList = "status"),
                @Index(name = "idx_vehicle_vehicle_type", columnList = "vehicle_type"),
                @Index(name = "idx_vehicle_assigned_driver", columnList = "assigned_driver_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vehicle_registration", columnNames = {"registration_number"}),
                @UniqueConstraint(name = "uq_vehicle_vin", columnNames = {"vin"}),
                @UniqueConstraint(name = "uq_vehicle_fleet_number", columnNames = {"fleet_number"})
        }
)
public class Vehicle extends BaseEntity {

    // ====== EXPLICIT LOGGER (since @Slf4j may not work) ======
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Vehicle.class);

    @Column(name = "registration_number", unique = true, nullable = false, length = 20)
    private String registrationNumber;

    @Column(name = "vin", length = 50, unique = true)
    private String vin;

    @Column(name = "make", length = 100)
    private String make;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "year")
    private Integer year;

    
    @Column(name = "vehicle_type", length = 20, nullable = false)
    private String vehicleType;

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

    @Column(name = "fleet_number", length = 50, unique = true)
    private String fleetNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
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

    // ====== AUDIT TRAIL WITH HIBERNATE TYPES ======
    @Type(JsonType.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail = new HashMap<>();

    
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "maintenance_cost", precision = 15, scale = 2)
    private BigDecimal maintenanceCost;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_due")
    private LocalDate nextMaintenanceDue;

    @Column(name = "fuel_efficiency", precision = 12, scale = 2)
    private BigDecimal fuelEfficiency;

    @Column(name = "insurance_provider", length = 100)
    private String insuranceProvider;

    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

        @Column(name = "current_fuel_level")
        private Double currentFuelLevel = 0.0;
        
        @Column(name = "fuel_capacity")
        private Double fuelCapacity = 400.0;
        
        @Column(name = "fuel_tank_count")
        private Integer fuelTankCount = 1;
        
        @Column(name = "fuel_tank_type")
        private String fuelTankType = "SINGLE";
        
        @Column(name = "last_fuel_update")
        private LocalDateTime lastFuelUpdate;


        

    // ====== Constructors ======
    public Vehicle() {
        this.status = "ACTIVE";
        this.incidentsLogged = 0;
        this.auditTrail = new HashMap<>();
        this.setIsActive(true);
        this.setVersion(0);
    }

    // ====== Business Methods ======
    
    public void calculateNextService() {
        if (lastServiceDate != null && serviceIntervalDays != null) {
            this.nextServiceDue = lastServiceDate.plusDays(serviceIntervalDays);
        }
        if (lastServiceOdometer != null && serviceIntervalKm != null) {
            this.nextServiceOdometer = lastServiceOdometer.add(BigDecimal.valueOf(serviceIntervalKm));
        }
    }

    public boolean isActive() {
            return ("ACTIVE".equals(status) || "AVAILABLE".equals(status)) && super.isActive();
        }

    public boolean isAvailable() {
        return isActive() &&
                assignedDriver == null &&
                isInsuranceValid() &&
                isRoadworthyValid() &&
                !isInMaintenance();
    }

    public boolean isInsuranceValid() {
        return insuranceExpiry == null || !insuranceExpiry.isBefore(LocalDate.now());
    }

    public boolean isRoadworthyValid() {
        return roadworthyExpiry == null || !roadworthyExpiry.isBefore(LocalDate.now());
    }

    public boolean isInMaintenance() {
        return "MAINTENANCE".equalsIgnoreCase(maintenanceStatus) || 
               "MAINTENANCE".equals(status);
    }

    public boolean isOverdueForService() {
        if (nextServiceDue != null && nextServiceDue.isBefore(LocalDate.now())) {
            return true;
        }
        if (nextServiceOdometer != null && currentOdometer != null && 
            currentOdometer.compareTo(nextServiceOdometer) >= 0) {
            return true;
        }
        return false;
    }

    public void incrementIncidents() {
        this.incidentsLogged = (incidentsLogged == null ? 0 : incidentsLogged) + 1;
    }

    public void assignDriver(Driver driver) {
        this.assignedDriver = driver;
        this.status = VehicleStatus.ASSIGNED;
    }

    public void unassignDriver() {
        this.assignedDriver = null;
        if (status == VehicleStatus.ASSIGNED) {
            this.status = VehicleStatus.AVAILABLE;
        }
    }

    public void updateOdometer(BigDecimal newOdometer) {
        if (newOdometer != null) {
            this.currentOdometer = newOdometer;
            this.currentMileage = newOdometer;
        }
    }

    public void markForMaintenance(String reason) {
        this.maintenanceStatus = reason;
        this.status = VehicleStatus.MAINTENANCE;
    }

    public void completeMaintenance() {
        this.maintenanceStatus = null;
        this.status = VehicleStatus.AVAILABLE;
    }

    public BigDecimal getDistanceSinceLastService() {
        if (currentOdometer != null && lastServiceOdometer != null) {
            return currentOdometer.subtract(lastServiceOdometer);
        }
        return BigDecimal.ZERO;
    }

    public String getDisplayName() {
        return String.format("%s %s (%s)", 
            make != null ? make : "", 
            model != null ? model : "", 
            registrationNumber);
    }

    // ====== Explicit Getters/Setters for BaseEntity fields ======
    // These ensure the methods exist and are accessible


        public Double getCurrentFuelLevel() { return currentFuelLevel; }
        public void setCurrentFuelLevel(Double currentFuelLevel) { this.currentFuelLevel = currentFuelLevel; }
        
        public Double getFuelCapacity() { return fuelCapacity; }
        public void setFuelCapacity(Double fuelCapacity) { this.fuelCapacity = fuelCapacity; }
        
        public Integer getFuelTankCount() { return fuelTankCount; }
        public void setFuelTankCount(Integer fuelTankCount) { this.fuelTankCount = fuelTankCount; }
        
        public String getFuelTankType() { return fuelTankType; }
        public void setFuelTankType(String fuelTankType) { this.fuelTankType = fuelTankType; }
        
        public LocalDateTime getLastFuelUpdate() { return lastFuelUpdate; }
        public void setLastFuelUpdate(LocalDateTime lastFuelUpdate) { this.lastFuelUpdate = lastFuelUpdate; }

        public void resetFuelToFull() {
            this.currentFuelLevel = this.fuelCapacity != null ? this.fuelCapacity : 400.0;
            this.lastFuelUpdate = LocalDateTime.now();
        }
    
    public Boolean getIsActive() {
        return super.isActive();
    }

    public void setIsActive(Boolean isActive) {
        super.setIsActive(isActive);
    }

    public Integer getVersion() {
        return super.getVersion();
    }

    public void setVersion(Integer version) {
        super.setVersion(version);
    }

    // ====== Lifecycle Hooks ======
    @PrePersist
    protected void onCreate() {
            if (status == null) {
                status = "ACTIVE";
            }
            if (vehicleType == null) {
                vehicleType = "TRUCK";
            }
        if (incidentsLogged == null) {
            incidentsLogged = 0;
        }
        if (auditTrail == null) {
            auditTrail = new HashMap<>();
        }
        // Ensure BaseEntity fields are set
        if (getIsActive() == null) {
            setIsActive(true);
        }
        if (getVersion() == null) {
            setVersion(0);
        }
        calculateNextService();
        log.debug("✅ Vehicle pre-persist: {}", this.registrationNumber);
    }

    @PreUpdate
    protected void onUpdate() {
        calculateNextService();
        log.debug("🔄 Vehicle pre-update: {}", this.registrationNumber);
    }

    // ====== Additional Getters/Setters (if Lombok fails) ======
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

        public String getVehicleType() {
            return vehicleType;
        }

        public void setVehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
        }

        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
    public BigDecimal getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(BigDecimal currentOdometer) {
        this.currentOdometer = currentOdometer;
    }

    public Driver getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(Driver assignedDriver) {
        this.assignedDriver = assignedDriver;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
