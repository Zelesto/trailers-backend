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
                @Index(name = "idx_trip_vehicle", columnList = "vehicle_id")
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
       Locations
       ======================== */

    @Column(name = "origin_location", nullable = false)
    private String originLocation;

    @Column(name = "destination_location", nullable = false)
    private String destinationLocation;

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

}
