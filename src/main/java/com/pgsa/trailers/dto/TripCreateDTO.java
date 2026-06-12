package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateDTO {

    private String tripNumber;
    private String tripType;
    private String status;
    private String approvalStatus;
    private String priority;

    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;

    private Double estimatedDuration;
    private Double plannedDistanceKm;
    private Double plannedDurationHours;

    private BigDecimal tollCost;
    private BigDecimal otherExpenses;

    private Long vehicleId;
    private Long driverId;
    private Long supervisorId;
    private Long loadId;

    private String commodityType;
    private String cargoDescription;
    private Double cargoWeight;
    private BigDecimal cargoValue;
    private Integer palletCount;
    private String containerNumber;

    // Origin
    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private Double originLatitude;
    private Double originLongitude;
    private String originLocation;

    // Destination
    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private Double destinationLatitude;
    private Double destinationLongitude;
    private String destinationLocation;

    private String notes;
    private String specialInstructions;
    private String driverNotes;

    private String referenceNumber;
    private String purchaseOrderNumber;

    private String cancellationReason;

    private String auditTrail;

    private Integer incidentsLogged;

    // Existing routing fields
    private String vehicleType;
    private boolean calculateRoute = true;
}
