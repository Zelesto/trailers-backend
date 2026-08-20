package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "load", indexes = {
        @Index(name = "idx_load_load_number", columnList = "load_number", unique = true),
        @Index(name = "idx_load_status", columnList = "status"),
        @Index(name = "idx_load_customer", columnList = "customer_id"),
        @Index(name = "idx_load_loading_date", columnList = "loading_date"),
        @Index(name = "idx_load_reference_number", columnList = "reference_number")
})
public class Load extends BaseEntity {

    // ====== EXPLICIT LOGGER (since @Slf4j may not work) ======
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Load.class);

    @Column(name = "load_number", unique = true, nullable = false, length = 50)
    private String loadNumber;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_cubic_m", precision = 10, scale = 2)
    private BigDecimal volumeCubicM;

    @Column(name = "loading_date")
    private LocalDateTime loadingDate;

    @Column(name = "unloading_date")
    private LocalDateTime unloadingDate;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "commodity_type", length = 100)
    private String commodityType;

    @Column(name = "pallet_count")
    private Integer palletCount;

    @Column(name = "container_number", length = 50)
    private String containerNumber;

    @Column(name = "hazardous_material")
    private Boolean hazardousMaterial = false;

    @Column(name = "special_handling", columnDefinition = "TEXT")
    private String specialHandling;

    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "actual_value", precision = 15, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "priority", length = 20)
    private String priority;

    // ======================== NEW FIELDS FROM DATABASE ========================
    
    @Column(name = "origin_location", length = 255)
    private String originLocation;

    @Column(name = "destination_location", length = 255)
    private String destinationLocation;

    @Column(name = "handling_instructions", columnDefinition = "TEXT")
    private String handlingInstructions;

    @Column(name = "packaging_type", length = 50)
    private String packagingType;

    @Column(name = "hazard_class", length = 50)
    private String hazardClass;

    @Column(name = "temperature_requirements", length = 50)
    private String temperatureRequirements;

    @Column(name = "trips_count")
    private Integer tripsCount = 0;

    @Column(name = "total_distance_km")
    private Integer totalDistanceKm = 0;

    @Column(name = "total_hours_active")
    private Integer totalHoursActive = 0;

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "insurance_policy_number", length = 100)
    private String insurancePolicyNumber;

    @Column(name = "insurance_expiry")
    private LocalDate insuranceExpiry;

    @Column(name = "customs_clearance_status", length = 50)
    private String customsClearanceStatus;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

    @Column(name = "audit_trail", columnDefinition = "TEXT")
    private String auditTrail;

    /* ========================
       DEPOT TRACKING - NEW FIELDS
       ======================== */
    @Column(name = "total_from_depot_km", precision = 10, scale = 2)
    private BigDecimal totalFromDepotKm = BigDecimal.ZERO;

    @Column(name = "total_to_depot_km", precision = 10, scale = 2)
    private BigDecimal totalToDepotKm = BigDecimal.ZERO;

    @Column(name = "total_depot_km", precision = 10, scale = 2)
    private BigDecimal totalDepotKm = BigDecimal.ZERO;

    @OneToMany(mappedBy = "load", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    // ======================== HELPER METHODS ========================
    
    public void addTrip(Trip trip) {
        if (trip == null) {
            log.warn("⚠️ Attempted to add null trip to load: {}", this.loadNumber);
            return;
        }
        
        trips.add(trip);
        trip.setLoad(this);
        trip.setLoadNumber(this.loadNumber);
        trip.setLoadType(this.commodityType);
        trip.setLoadDescription(this.description);
        trip.setLoadStatus(this.status != null ? this.status : "PENDING");
        
        if (this.tripsCount == null) {
            this.tripsCount = 0;
        }
        this.tripsCount++;
        
        log.debug("✅ Added trip {} to load {}", trip.getTripNumber(), this.loadNumber);
        recalculateDepotTotals();
    }

    public void removeTrip(Trip trip) {
        if (trip == null) {
            log.warn("⚠️ Attempted to remove null trip from load: {}", this.loadNumber);
            return;
        }
        
        trips.remove(trip);
        trip.setLoad(null);
        trip.setLoadNumber(null);
        trip.setLoadType(null);
        trip.setLoadDescription(null);
        trip.setLoadStatus(null);
        
        if (this.tripsCount != null && this.tripsCount > 0) {
            this.tripsCount--;
        }
        
        log.debug("✅ Removed trip {} from load {}", trip.getTripNumber(), this.loadNumber);
        recalculateDepotTotals();
    }

    public boolean isEmpty() {
        return trips == null || trips.isEmpty();
    }

    public int getTripCount() {
        return trips != null ? trips.size() : 0;
    }

    public BigDecimal getTotalWeight() {
        if (weightKg != null) {
            return weightKg;
        }
        if (trips != null && !trips.isEmpty()) {
            return trips.stream()
                    .map(Trip::getCargoWeight)
                    .filter(w -> w != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalValue() {
        if (actualValue != null) {
            return actualValue;
        }
        if (trips != null && !trips.isEmpty()) {
            return trips.stream()
                    .map(Trip::getCargoValue)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public int getTotalTrips() {
        return trips != null ? trips.size() : 0;
    }

    // FIXED: Removed .name() calls on status field (String)
    public int getCompletedTrips() {
        if (trips == null || trips.isEmpty()) {
            return 0;
        }
        return (int) trips.stream()
                .filter(t -> t.getStatus() != null && 
                    ("COMPLETED".equals(t.getStatus()) || 
                     "FINALIZED".equals(t.getStatus())))
                .count();
    }

    public void recalculateDepotTotals() {
        if (trips == null || trips.isEmpty()) {
            totalFromDepotKm = BigDecimal.ZERO;
            totalToDepotKm = BigDecimal.ZERO;
            totalDepotKm = BigDecimal.ZERO;
            return;
        }
        
        totalFromDepotKm = trips.stream()
                .map(Trip::getFromDepotKm)
                .filter(km -> km != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalToDepotKm = trips.stream()
                .map(Trip::getToDepotKm)
                .filter(km -> km != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalDepotKm = trips.stream()
                .map(Trip::getTotalDepotKm)
                .filter(km -> km != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* ========================
       LIFECYCLE CALLBACKS
       ======================== */

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (priority == null) {
            priority = "NORMAL";
        }
        if (hazardousMaterial == null) {
            hazardousMaterial = false;
        }
        if (tripsCount == null) {
            tripsCount = 0;
        }
        if (totalDistanceKm == null) {
            totalDistanceKm = 0;
        }
        if (totalHoursActive == null) {
            totalHoursActive = 0;
        }
        if (incidentsLogged == null) {
            incidentsLogged = 0;
        }
        if (lastStatusUpdate == null) {
            lastStatusUpdate = LocalDateTime.now();
        }
        if (referenceNumber == null || referenceNumber.trim().isEmpty()) {
            referenceNumber = "";
        }
        if (totalFromDepotKm == null) {
            totalFromDepotKm = BigDecimal.ZERO;
        }
        if (totalToDepotKm == null) {
            totalToDepotKm = BigDecimal.ZERO;
        }
        if (totalDepotKm == null) {
            totalDepotKm = BigDecimal.ZERO;
        }
        
        log.info("✅ Load pre-persist complete: {} | Status: {} | Ref: {}", 
            this.loadNumber, this.status, this.referenceNumber);
    }

    @PreUpdate
    protected void onUpdate() {
        if (trips != null && !trips.isEmpty()) {
            for (Trip trip : trips) {
                trip.setLoadNumber(this.loadNumber);
                trip.setLoadType(this.commodityType);
                trip.setLoadDescription(this.description);
                // FIXED: Removed .name() call on status field
                trip.setLoadStatus(this.status != null ? this.status : "PENDING");
            }
            log.debug("✅ Updated {} trips with load details", trips.size());
        }
        
        lastStatusUpdate = LocalDateTime.now();
        tripsCount = trips != null ? trips.size() : 0;
        recalculateDepotTotals();
        
        log.info("✅ Load pre-update complete: {} | Trips: {} | Depot KM: {}", 
            this.loadNumber, this.tripsCount, this.totalDepotKm);
    }

    public String getType() {
        if (commodityType != null && !commodityType.isEmpty()) {
            return commodityType;
        }
        return "GENERAL";
    }

    public boolean canAcceptTrip() {
        return isActive();
    }
    
    public String getStatusDisplay() {
        return status != null ? status : "UNKNOWN";
    }
    
    public boolean isActive() {
        return status != null && 
               !"COMPLETED".equals(status) && 
               !"CANCELLED".equals(status);
    }
}
