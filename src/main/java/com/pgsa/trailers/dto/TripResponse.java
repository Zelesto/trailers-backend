package com.pgsa.trailers.dto;

import com.pgsa.trailers.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Long id;
    private String tripNumber;
    private String tripType;

    // Relationships
    private Long vehicleId;
    private String vehicleRegistration;
    private String vehicleMake;
    private String vehicleModel;

    private Long driverId;
    private String driverName;
    private String driverLicenseNumber;

    private Long supervisorId;
    private String supervisorName;

    private Long loadId;
    private String loadReference;

    // Planning
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    private BigDecimal plannedDistanceKm;
    private BigDecimal plannedDurationHours;
    private BigDecimal estimatedDurationHours;

    // Origin
    private String originLocation;
    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private Double originLatitude;
    private Double originLongitude;

    // Destination
    private String destinationLocation;
    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private Double destinationLatitude;
    private Double destinationLongitude;

    // Execution
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    private BigDecimal actualStartOdometer;
    private BigDecimal actualEndOdometer;
    private BigDecimal actualDistanceKm;
    private BigDecimal actualDurationHours;

    // Operational Metrics
    private BigDecimal distanceKm;
    private BigDecimal fuelConsumedLiters;
    private BigDecimal fuelEfficiency;

    // Costs
    private BigDecimal tollCost;
    private BigDecimal otherExpenses;
    private BigDecimal totalCost;

    // Cargo
    private String commodityType;
    private String cargoDescription;
    private BigDecimal cargoWeight;
    private BigDecimal cargoValue;
    private Integer palletCount;
    private String containerNumber;

    // Workflow
    private TripStatus status;
    private String approvalStatus;
    private String priority;
    private LocalDateTime approvedAt;

    // Cancellation
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // Route
    private String gpsStartLocation;
    private String gpsEndLocation;
    private String routeDetails;
    private String checkpoints;

    // Notes
    private String driverNotes;
    private String specialInstructions;
    private String notes;

    // References
    private String referenceNumber;
    private String purchaseOrderNumber;

    // Incidents
    private Integer incidentsLogged;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime lastStatusUpdate;

    // Metrics
    private TripMetricsResponse metrics;
}
