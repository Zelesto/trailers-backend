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
       Identity & classification
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

    /* ========================
       Locations (Original fields kept for backward compatibility)
       ======================== */

    @Column(name = "origin_location", nullable = false)
    private String originLocation;

    @Column(name = "destination_location", nullable = false)
    private String destinationLocation;

    /* ========================
       New: Detailed origin address fields
       ======================== */

    @Column(name = "origin_street_address", length = 255)
    private String originStreetAddress;

    @Column(name = "origin_city", length = 100)
    private String originCity;

    @Column(name = "origin_zip_code", length = 10)
    private String originZipCode;

    @Column(name = "origin_province", length = 50)
    private String originProvince;

    @Column(name = "origin_latitude")
    private Double originLatitude;

    @Column(name = "origin_longitude")
    private Double originLongitude;

    /* ========================
       New: Detailed destination address fields
       ======================== */

    @Column(name = "destination_street_address", length = 255)
    private String destinationStreetAddress;

    @Column(name = "destination_city", length = 100)
    private String destinationCity;

    @Column(name = "destination_zip_code", length = 10)
    private String destinationZipCode;

    @Column(name = "destination_province", length = 50)
    private String destinationProvince;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    /* ========================
       Planning vs actuals
       ======================== */

    @Column(name = "planned_start_date")
    private LocalDateTime plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDateTime plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDateTime actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDateTime actualEndDate;


    @Column(name = "actual_start_odometer")
    private BigDecimal actualStartOdometer;

    @Column(name = "actual_end_odometer")
    private BigDecimal actualEndOdometer;

    @Column(name = "actual_distance_km")
    private BigDecimal actualDistanceKm;


    // Planned route (estimated from routing service)
    @Column(name = "planned_distance_km", precision = 10, scale = 2)
    private BigDecimal plannedDistanceKm;

    @Column(name = "planned_duration_hours", precision = 10, scale = 2)
    private BigDecimal plannedDurationHours;


    /* ========================
       Workflow & approval
       ======================== */

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TripStatus status;


    @Column(name = "approval_status", length = 30)
    private String approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Driver supervisor;

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

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

    /* ========================
       Metrics (read-only relation)
       ======================== */

    @OneToOne(
    mappedBy = "trip",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private TripMetrics metrics;

    /* ========================
       Helper methods
       ======================== */

    /**
     * Builds the complete origin address string from components
     */
    public String buildOriginAddress() {
        StringBuilder address = new StringBuilder();
        
        if (originStreetAddress != null && !originStreetAddress.trim().isEmpty()) {
            address.append(originStreetAddress);
        }
        
        if (originCity != null && !originCity.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(originCity);
        }
        
        if (originZipCode != null && !originZipCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(originZipCode);
        }
        
        if (originProvince != null && !originProvince.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(originProvince);
        }
        
        address.append(", South Africa");
        
        return address.toString();
    }

    /**
     * Builds the complete destination address string from components
     */
    public String buildDestinationAddress() {
        StringBuilder address = new StringBuilder();
        
        if (destinationStreetAddress != null && !destinationStreetAddress.trim().isEmpty()) {
            address.append(destinationStreetAddress);
        }
        
        if (destinationCity != null && !destinationCity.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(destinationCity);
        }
        
        if (destinationZipCode != null && !destinationZipCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(destinationZipCode);
        }
        
        if (destinationProvince != null && !destinationProvince.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(destinationProvince);
        }
        
        address.append(", South Africa");
        
        return address.toString();
    }

    /**
     * Updates the legacy origin_location field from components
     */
    public void updateOriginLocationFromComponents() {
        this.originLocation = buildOriginAddress();
    }

    /**
     * Updates the legacy destination_location field from components
     */
    public void updateDestinationLocationFromComponents() {
        this.destinationLocation = buildDestinationAddress();
    }

    /**
     * Populates all origin address fields from a single string (for backward compatibility)
     */
    public void setOriginFromAddressString(String fullAddress) {
        this.originLocation = fullAddress;
        // Note: This doesn't parse the string into components.
        // Components would need to be set separately or use a geocoding service.
    }

    /**
     * Populates all destination address fields from a single string (for backward compatibility)
     */
    public void setDestinationFromAddressString(String fullAddress) {
        this.destinationLocation = fullAddress;
        // Note: This doesn't parse the string into components.
        // Components would need to be set separately or use a geocoding service.
    }
}
