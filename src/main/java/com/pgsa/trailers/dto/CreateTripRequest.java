package com.pgsa.trailers.dto;

import com.pgsa.trailers.enums.TripStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateTripRequest {

    /* ========================
       RELATIONSHIPS
       ======================== */
    private Long vehicleId;
    private Long driverId;
    private Long supervisorId;
    private Long loadId;

    /* ========================
       IDENTITY
       ======================== */
    private String tripType;

    /* ========================
       WORKFLOW
       ======================== */
    private TripStatus status;
    private String approvalStatus;

    /* ========================
       PLANNING
       ======================== */
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;

    private BigDecimal plannedDistanceKm;
    private BigDecimal plannedDurationHours;
    private BigDecimal estimatedDurationHours;

    /* ========================
       COSTS
       ======================== */
    private BigDecimal tollCost;
    private BigDecimal otherExpenses;

    /* ========================
       CARGO
       ======================== */
    private String commodityType;
    private String cargoDescription;
    private BigDecimal cargoWeight;
    private BigDecimal cargoValue;
    private Integer palletCount;
    private String containerNumber;

    private BigDecimal distanceKm;
    private BigDecimal fuelConsumedLiters;

    /* ========================
       ORIGIN (RAW + STRUCTURED)
       ======================== */
    private String originLocation;

    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private Double originLatitude;
    private Double originLongitude;

    /* ========================
       DESTINATION (RAW + STRUCTURED)
       ======================== */
    private String destinationLocation;

    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private Double destinationLatitude;
    private Double destinationLongitude;

    /* ========================
       NOTES
       ======================== */
    private String notes;
    private String specialInstructions;
    private String driverNotes;

    /* ========================
       REFERENCES
       ======================== */
    private String referenceNumber;
    private String purchaseOrderNumber;

    /* ========================
       OPERATIONS
       ======================== */
    private Integer incidentsLogged;

    private String cancellationReason;

    /* ========================
       ROUTE DATA
       ======================== */
    private String gpsStartLocation;
    private String gpsEndLocation;
    private String routeDetails;
    private String checkpoints;

    /* ========================
       AUDIT
       ======================== */
    private String auditTrail;
}
