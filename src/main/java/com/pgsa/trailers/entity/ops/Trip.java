package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.enums.TripStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "trip",
        indexes = {
                @Index(name = "idx_trip_trip_number", columnList = "trip_number"),
                @Index(name = "idx_trip_status", columnList = "status"),
                @Index(name = "idx_trip_vehicle", columnList = "vehicle_id"),
                @Index(name = "idx_trip_origin_city", columnList = "origin_city"),
                @Index(name = "idx_trip_destination_city", columnList = "destination_city")
        }
)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========================
       Identity
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
    @JoinColumn(name = "load_id")
    private Load load;

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

    @Column(name = "actual_distance_km", precision = 10, scale = 2)
    private BigDecimal actualDistanceKm;

    @Column(name = "actual_duration_hours", precision = 10, scale = 2)
    private BigDecimal actualDurationHours;

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

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "driver_notes", columnDefinition = "TEXT")
    private String driverNotes;

    /* ========================
       Workflow
       ======================== */

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private TripStatus status;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

    @Column(name = "audit_trail", columnDefinition = "TEXT")
    private String auditTrail;

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
            address.append(" ").append(originZipCode);
        }

        if (originProvince != null && !originProvince.isBlank()) {
            address.append(", ").append(originProvince);
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
            address.append(" ").append(destinationZipCode);
        }

        if (destinationProvince != null && !destinationProvince.isBlank()) {
            address.append(", ").append(destinationProvince);
        }

        return address.toString();
    }

    public void updateOriginLocationFromComponents() {
        this.originLocation = buildOriginAddress();
    }

    public void updateDestinationLocationFromComponents() {
        this.destinationLocation = buildDestinationAddress();
    }
}
