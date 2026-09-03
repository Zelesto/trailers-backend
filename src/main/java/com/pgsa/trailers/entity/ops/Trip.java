package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@Entity
@Slf4j
@Table(
        name = "trip",
        indexes = {
                @Index(name = "idx_trip_trip_number", columnList = "trip_number", unique = true),
                @Index(name = "idx_trip_status", columnList = "status"),
                @Index(name = "idx_trip_vehicle", columnList = "vehicle_id"),
                @Index(name = "idx_trip_driver", columnList = "driver_id"),
                @Index(name = "idx_trip_load", columnList = "load_id"),
                @Index(name = "idx_trip_customer", columnList = "customer_id"),
                @Index(name = "idx_trip_origin_city", columnList = "origin_city"),
                @Index(name = "idx_trip_destination_city", columnList = "destination_city"),
                @Index(name = "idx_trip_created_at", columnList = "created_at"),
                @Index(name = "idx_trip_departed_from", columnList = "departed_from"),
                @Index(name = "idx_trip_is_from_depot", columnList = "is_from_depot")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Trip {

    // ====== Explicit Logger (since @Slf4j may not work) ======
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Trip.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========================
       Customer Relationship
       ======================== */
    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;


    @Column(name = "crane_used", columnDefinition = "boolean default false")
    private Boolean craneUsed;


    /* ========================
       Load Relationship
       ======================== */
    @Column(name = "load_id")
    private String loadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", referencedColumnName = "load_number", insertable = false, updatable = false)
    private Load load;

    // Load denormalized fields (for quick access without join)
    @Column(name = "load_number")
    private String loadNumber;

    @Column(name = "load_type")
    private String loadType;

    @Column(name = "load_description")
    private String loadDescription;

    @Column(name = "load_status")
    private String loadStatus;

    /* ========================
       Cargo fields
       ======================== */
    @Column(name = "commodity_type", length = 100)
    private String commodityType;

    @Column(name = "cargo_description", columnDefinition = "TEXT")
    private String cargoDescription;

    @Column(name = "cargo_weight", precision = 10, scale = 2)
    private BigDecimal cargoWeight;

    @Column(name = "cargo_value", precision = 15, scale = 2)
    private BigDecimal cargoValue;

    @Column(name = "pallet_count")
    private Integer palletCount;

    @Column(name = "container_number", length = 50)
    private String containerNumber;



        /* ========================
       DISTANCE
       ======================== */

        @Column(name = "calculated_distance_km", precision = 10, scale = 2)
        private BigDecimal calculatedDistanceKm;
        
        @Column(name = "actual_distance_km", precision = 10, scale = 2)
        private BigDecimal actualDistanceKm;
        
        @Column(name = "distance_calculated")
        private Boolean distanceCalculated = false;
        
        @Column(name = "distance_calculated_at")
        private LocalDateTime distanceCalculatedAt;
        
        @Column(name = "distance_calculation_error", length = 500)
        private String distanceCalculationError;

    /* ========================
       Notes fields
       ======================== */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    /* ========================
       Reference fields
       ======================== */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "purchase_order_number", length = 100)
    private String purchaseOrderNumber;

    /* ========================
       Identity - CRITICAL FIELD
       ======================== */
    @Column(name = "trip_number", nullable = false, unique = true, length = 50)
    private String tripNumber;

    @Column(name = "trip_type", length = 50)
    private String tripType;

    /* ========================
       Relationships
       ======================== */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Driver supervisor;

    /* ========================
       Planning
       ======================== */
    @Column(name = "planned_start_date")
    private LocalDateTime plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDateTime plannedEndDate;

    @Column(name = "planned_distance_km", precision = 10, scale = 2)
    private BigDecimal plannedDistanceKm;

    @Column(name = "planned_duration_hours", precision = 10, scale = 2)
    private BigDecimal plannedDurationHours;

    /* ========================
       Locations
       ======================== */
    @Column(name = "origin_location", nullable = false)
    private String originLocation;

    @Column(name = "destination_location", nullable = false)
    private String destinationLocation;

    /* ========================
       Origin Details
       ======================== */
    @Column(name = "origin_street_address", length = 255)
    private String originStreetAddress;

    @Column(name = "origin_city", length = 100)
    private String originCity;

    @Column(name = "origin_zip_code", length = 20)
    private String originZipCode;

    @Column(name = "origin_province", length = 100)
    private String originProvince;

    @Column(name = "origin_latitude")
    private Double originLatitude;

    @Column(name = "origin_longitude")
    private Double originLongitude;

    /* ========================
       Destination Details
       ======================== */
    @Column(name = "destination_street_address", length = 255)
    private String destinationStreetAddress;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "destination_zip_code", length = 20)
    private String destinationZipCode;

    @Column(name = "destination_province", length = 100)
    private String destinationProvince;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    /* ========================
       Execution
       ======================== */
    @Column(name = "actual_start_date")
    private LocalDateTime actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDateTime actualEndDate;

    @Column(name = "actual_start_odometer", precision = 12, scale = 2)
    private BigDecimal actualStartOdometer;

    @Column(name = "actual_end_odometer", precision = 12, scale = 2)
    private BigDecimal actualEndOdometer;

    @Column(name = "actual_duration_hours", precision = 10, scale = 2)
    private BigDecimal actualDurationHours;



        public Vehicle getVehicle() {
            return this.vehicle;
        }
    /* ========================
       Operational Metrics
       ======================== */
    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "estimated_duration_hours", precision = 10, scale = 2)
    private BigDecimal estimatedDurationHours;

    @Column(name = "fuel_consumed_liters", precision = 10, scale = 2)
    private BigDecimal fuelConsumedLiters;

    /* ========================
       Costs
       ======================== */
    @Column(name = "revenue_amount", precision = 15, scale = 2)
    private BigDecimal revenueAmount;

    @Column(name = "cost_amount", precision = 15, scale = 2)
    private BigDecimal costAmount;

    @Column(name = "toll_cost", precision = 15, scale = 2)
    private BigDecimal tollCost;

    @Column(name = "other_expenses", precision = 15, scale = 2)
    private BigDecimal otherExpenses;

    /* ========================
       Route Information
       ======================== */
    @Column(name = "gps_start_location", length = 255)
    private String gpsStartLocation;

    @Column(name = "gps_end_location", length = 255)
    private String gpsEndLocation;

    @Column(name = "route_details", columnDefinition = "TEXT")
    private String routeDetails;

    @Column(name = "checkpoints", columnDefinition = "TEXT")
    private String checkpoints;

    /* ========================
       Notes & Incidents
       ======================== */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "driver_notes", columnDefinition = "TEXT")
    private String driverNotes;

    /* ========================
       Workflow
       ======================== */
        @Column(name = "status", length = 50, nullable = false)
        private String status;

    @Column(name = "approval_status", length = 30)
    private String approvalStatus;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /* ========================
       Cancellation
       ======================== */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    /* ========================
       Audit
       ======================== */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

    @Type(JsonType.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail = new HashMap<>();

    /* ========================
       Metrics
       ======================== */
    @OneToOne(
            mappedBy = "trip",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private TripMetrics metrics;

    /* ========================
       DEPOT TRACKING
       ======================== */
    @Column(name = "from_depot_km", precision = 10, scale = 2)
    private BigDecimal fromDepotKm;

    @Column(name = "to_depot_km", precision = 10, scale = 2)
    private BigDecimal toDepotKm;

    @Column(name = "departed_from", length = 50)
    private String departedFrom;

    @Column(name = "departure_location", columnDefinition = "TEXT")
    private String departureLocation;

    @Column(name = "is_from_depot")
    private Boolean isFromDepot = false;

    /* ========================
       Equals and HashCode
       ======================== */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trip)) return false;
        Trip trip = (Trip) o;
        return id != null && Objects.equals(id, trip.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /* ========================
       Business Methods
       ======================== */

    public void calculateActualDistance() {
        if (actualStartOdometer != null && actualEndOdometer != null) {
            this.actualDistanceKm = actualEndOdometer.subtract(actualStartOdometer);
        }
    }

    public void calculateActualDuration() {
        if (actualStartDate != null && actualEndDate != null) {
            long hours = java.time.Duration.between(actualStartDate, actualEndDate).toHours();
            this.actualDurationHours = BigDecimal.valueOf(hours);
        }
    }

        public boolean isPlanned() {
            return "PLANNED".equals(status);
        }
        
        public boolean isInProgress() {
            return "IN_PROGRESS".equals(status);
        }
        
        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }
        
        public boolean isCancelled() {
            return "CANCELLED".equals(status);
        }
        
        public boolean isActive() {
            return status != null && 
                   ("PLANNED".equals(status) || 
                    "IN_PROGRESS".equals(status) || 
                    "ON_HOLD".equals(status));
        }

    public BigDecimal getTotalDepotKm() {
        BigDecimal from = fromDepotKm != null ? fromDepotKm : BigDecimal.ZERO;
        BigDecimal to = toDepotKm != null ? toDepotKm : BigDecimal.ZERO;
        return from.add(to);
    }


        public Boolean getCraneUsed() {
            return craneUsed != null ? craneUsed : false;
        }
        
        public void setCraneUsed(Boolean craneUsed) {
            this.craneUsed = craneUsed;
        }
    /* ========================
       LOAD MANAGEMENT METHODS - ADDED FOR COMPILATION
       ======================== */

    public void setLoad(Load load) {
        this.load = load;
    }

    public void setLoadId(String loadId) {
        this.loadId = loadId;
    }

    public void setLoadNumber(String loadNumber) {
        this.loadNumber = loadNumber;
    }

    public void setLoadType(String loadType) {
        this.loadType = loadType;
    }

    public void setLoadDescription(String loadDescription) {
        this.loadDescription = loadDescription;
    }

    public void setLoadStatus(String loadStatus) {
        this.loadStatus = loadStatus;
    }

    /* ========================
       DISTANCE
       ======================== */
public BigDecimal getCalculatedDistanceKm() { return calculatedDistanceKm; }
public void setCalculatedDistanceKm(BigDecimal calculatedDistanceKm) { this.calculatedDistanceKm = calculatedDistanceKm; }

public BigDecimal getActualDistanceKm() { return actualDistanceKm; }
public void setActualDistanceKm(BigDecimal actualDistanceKm) { this.actualDistanceKm = actualDistanceKm; }

public Boolean getDistanceCalculated() { return distanceCalculated; }
public void setDistanceCalculated(Boolean distanceCalculated) { this.distanceCalculated = distanceCalculated; }

public LocalDateTime getDistanceCalculatedAt() { return distanceCalculatedAt; }
public void setDistanceCalculatedAt(LocalDateTime distanceCalculatedAt) { this.distanceCalculatedAt = distanceCalculatedAt; }

public String getDistanceCalculationError() { return distanceCalculationError; }
public void setDistanceCalculationError(String distanceCalculationError) { this.distanceCalculationError = distanceCalculationError; }


        /* ========================
       EXPLICIT GETTERS (if Lombok fails)
       ======================== */

    public String getTripNumber() {
        return tripNumber;
    }

    public BigDecimal getCargoWeight() {
        return cargoWeight;
    }

    public BigDecimal getCargoValue() {
        return cargoValue;
    }

        public String getStatus() {
            return status;
        }


    public BigDecimal getFromDepotKm() {
        return fromDepotKm;
    }

    public BigDecimal getToDepotKm() {
        return toDepotKm;
    }

    public Long getId() {
        return id;
    }

    public String getLoadNumber() {
        return loadNumber;
    }

    public String getLoadType() {
        return loadType;
    }

    public String getLoadDescription() {
        return loadDescription;
    }

    public String getLoadStatus() {
        return loadStatus;
    }

    public Load getLoad() {
        return load;
    }

    public String getLoadId() {
        return loadId;
    }

        public String getOriginLocation() {
            return originLocation;
        }
        
        public String getDestinationLocation() {
            return destinationLocation;
        }

    /* ========================
       EXPLICIT SETTERS (if Lombok fails)
       ======================== */

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public void setCargoWeight(BigDecimal cargoWeight) {
        this.cargoWeight = cargoWeight;
    }

    public void setCargoValue(BigDecimal cargoValue) {
        this.cargoValue = cargoValue;
    }

        public void setStatus(String status) {
            this.status = status;
        }

    public void setFromDepotKm(BigDecimal fromDepotKm) {
        this.fromDepotKm = fromDepotKm;
    }

    public void setToDepotKm(BigDecimal toDepotKm) {
        this.toDepotKm = toDepotKm;
    }

    /* ========================
       Convenience Methods
       ======================== */

    public String buildOriginAddress() {
        StringBuilder address = new StringBuilder();

        if (originStreetAddress != null && !originStreetAddress.isBlank()) {
            address.append(originStreetAddress);
        }

        if (originCity != null && !originCity.isBlank()) {
            if (!address.isEmpty()) address.append(", ");
            address.append(originCity);
        }

        if (originZipCode != null && !originZipCode.isBlank()) {
            if (!address.isEmpty()) address.append(" ");
            address.append(originZipCode);
        }

        if (originProvince != null && !originProvince.isBlank()) {
            if (!address.isEmpty()) address.append(", ");
            address.append(originProvince);
        }

        return address.toString();
    }

    public String buildDestinationAddress() {
        StringBuilder address = new StringBuilder();

        if (destinationStreetAddress != null && !destinationStreetAddress.isBlank()) {
            address.append(destinationStreetAddress);
        }

        if (destinationCity != null && !destinationCity.isBlank()) {
            if (!address.isEmpty()) address.append(", ");
            address.append(destinationCity);
        }

        if (destinationZipCode != null && !destinationZipCode.isBlank()) {
            if (!address.isEmpty()) address.append(" ");
            address.append(destinationZipCode);
        }

        if (destinationProvince != null && !destinationProvince.isBlank()) {
            if (!address.isEmpty()) address.append(", ");
            address.append(destinationProvince);
        }

        return address.toString();
    }

    public void updateOriginLocationFromComponents() {
        this.originLocation = buildOriginAddress();
    }

    public void updateDestinationLocationFromComponents() {
        this.destinationLocation = buildDestinationAddress();
    }

    /* ========================
       LIFECYCLE CALLBACKS
       ======================== */

    @PrePersist
    protected void onCreate() {
        // Set default values
        if (status == null) {
            status = "PLANNED";
        }
        if (incidentsLogged == null) {
            incidentsLogged = 0;
        }
        if (lastStatusUpdate == null) {
            lastStatusUpdate = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (isFromDepot == null) {
            isFromDepot = false;
        }
        
        // Emergency: If tripNumber is null, generate a fallback
        if (tripNumber == null || tripNumber.trim().isEmpty()) {
            log.warn("🚨 TRIP NUMBER IS NULL IN @PrePersist! Generating emergency fallback.");
            this.tripNumber = "TRP-EMERG-" + System.currentTimeMillis();
        }
        
        // Update location components
        updateOriginLocationFromComponents();
        updateDestinationLocationFromComponents();
    }

    @PreUpdate
    protected void onUpdate() {
        updateOriginLocationFromComponents();
        updateDestinationLocationFromComponents();
    }
}
