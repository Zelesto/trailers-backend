// src/main/java/com/pgsa/trailers/dto/CreateTripRequest.java
package com.pgsa.trailers.dto;

import com.pgsa.trailers.enums.TripStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateTripRequest {

    /* ========================
       RELATIONSHIPS (Required)
       ======================== */
    @NotNull(message = "Vehicle ID is required")
    @Positive(message = "Vehicle ID must be positive")
    private Long vehicleId;

    private Long driverId;
    private Long supervisorId;
    
    /* ========================
       CUSTOMER & LOAD RELATIONSHIPS
       ======================== */
    private Long customerId;  // Link to customer for invoicing
    private String loadId;    // Link to load for consolidation

    /* ========================
       IDENTITY
       ======================== */
    @Size(max = 50, message = "Trip type cannot exceed 50 characters")
    private String tripType;

    /* ========================
       WORKFLOW
       ======================== */
    private TripStatus status;
    private String approvalStatus;
    //private String priority;

    /* ========================
       PLANNING
       ======================== */
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;

    @Positive(message = "Planned distance must be positive")
    private BigDecimal plannedDistanceKm;

    @Positive(message = "Planned duration must be positive")
    private BigDecimal plannedDurationHours;

    @Positive(message = "Estimated duration must be positive")
    private BigDecimal estimatedDurationHours;

    /* ========================
       COSTS
       ======================== */
    @Positive(message = "Toll cost cannot be negative")
    private BigDecimal tollCost;

    private BigDecimal otherExpenses;

    /* ========================
       CARGO
       ======================== */
    @Size(max = 100, message = "Commodity type cannot exceed 100 characters")
    private String commodityType;

    private String cargoDescription;

    @Positive(message = "Cargo weight must be positive")
    private BigDecimal cargoWeight;

    private BigDecimal cargoValue;

    @Positive(message = "Pallet count must be positive")
    private Integer palletCount;

    @Size(max = 50, message = "Container number cannot exceed 50 characters")
    private String containerNumber;

    @Positive(message = "Distance must be positive")
    private BigDecimal distanceKm;

    @Positive(message = "Fuel consumed must be positive")
    private BigDecimal fuelConsumedLiters;

    /* ========================
       ORIGIN (RAW + STRUCTURED)
       ======================== */
    @NotNull(message = "Origin location is required")
    @Size(max = 255, message = "Origin location cannot exceed 255 characters")
    private String originLocation;

    @Size(max = 255, message = "Origin street address cannot exceed 255 characters")
    private String originStreetAddress;

    @Size(max = 100, message = "Origin city cannot exceed 100 characters")
    private String originCity;

    @Size(max = 20, message = "Origin zip code cannot exceed 20 characters")
    private String originZipCode;

    @Size(max = 100, message = "Origin province cannot exceed 100 characters")
    private String originProvince;

    private Double originLatitude;
    private Double originLongitude;

    /* ========================
       DESTINATION (RAW + STRUCTURED)
       ======================== */
    @NotNull(message = "Destination location is required")
    @Size(max = 255, message = "Destination location cannot exceed 255 characters")
    private String destinationLocation;

    @Size(max = 255, message = "Destination street address cannot exceed 255 characters")
    private String destinationStreetAddress;

    @Size(max = 100, message = "Destination city cannot exceed 100 characters")
    private String destinationCity;

    @Size(max = 20, message = "Destination zip code cannot exceed 20 characters")
    private String destinationZipCode;

    @Size(max = 100, message = "Destination province cannot exceed 100 characters")
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
    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNumber;

    @Size(max = 100, message = "Purchase order number cannot exceed 100 characters")
    private String purchaseOrderNumber;

    /* ========================
       OPERATIONS
       ======================== */
    private Integer incidentsLogged;
    private String cancellationReason;

    /* ========================
       ROUTE DATA
       ======================== */
    @Size(max = 255, message = "GPS start location cannot exceed 255 characters")
    private String gpsStartLocation;

    @Size(max = 255, message = "GPS end location cannot exceed 255 characters")
    private String gpsEndLocation;

    private String routeDetails;
    private String checkpoints;

    /* ========================
       AUDIT
       ======================== */
    private String auditTrail;
}
