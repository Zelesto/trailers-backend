// src/main/java/com/pgsa/trailers/dto/TripResponse.java
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

    // ======================== CUSTOMER ========================
    private Long customerId;
    private String customerName;
    private String customerCode;

    // ======================== LOAD ========================
    private String loadId;
    private String loadNumber;
    private String loadType;
    private String loadDescription;
    private String loadStatus;

    // ======================== VEHICLE ========================
    private Long vehicleId;
    private String vehicleRegistration;
    private String vehicleMake;
    private String vehicleModel;

    // ======================== DRIVER ========================
    private Long driverId;
    private String driverName;
    private String driverLicenseNumber;

    // ======================== SUPERVISOR ========================
    private Long supervisorId;
    private String supervisorName;

    // ======================== PLANNING ========================
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    private BigDecimal plannedDistanceKm;
    private BigDecimal plannedDurationHours;
    private BigDecimal estimatedDurationHours;

    // ======================== ORIGIN ========================
    private String originLocation;
    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private Double originLatitude;
    private Double originLongitude;

    // ======================== DESTINATION ========================
    private String destinationLocation;
    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private Double destinationLatitude;
    private Double destinationLongitude;

    // ======================== EXECUTION ========================
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    private BigDecimal actualStartOdometer;
    private BigDecimal actualEndOdometer;
    private BigDecimal actualDistanceKm;
    private BigDecimal actualDurationHours;

    // ======================== OPERATIONAL METRICS ========================
    private BigDecimal distanceKm;
    private BigDecimal fuelConsumedLiters;
    private BigDecimal fuelEfficiency;

    // ======================== COSTS ========================
    private BigDecimal tollCost;
    private BigDecimal otherExpenses;
    private BigDecimal totalCost;
    private BigDecimal revenueAmount;
    private BigDecimal costAmount;

    // ======================== CARGO ========================
    private String commodityType;
    private String cargoDescription;
    private BigDecimal cargoWeight;
    private BigDecimal cargoValue;
    private Integer palletCount;
    private String containerNumber;

    // ======================== WORKFLOW ========================
    private TripStatus status;
    private String approvalStatus;
    private String priority;
    private LocalDateTime approvedAt;

    // ======================== CANCELLATION ========================
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    // ======================== ROUTE ========================
    private String gpsStartLocation;
    private String gpsEndLocation;
    private String routeDetails;
    private String checkpoints;

    // ======================== NOTES ========================
    private String driverNotes;
    private String specialInstructions;
    private String notes;

    // ======================== REFERENCES ========================
    private String referenceNumber;
    private String purchaseOrderNumber;

    // ======================== INCIDENTS ========================
    private Integer incidentsLogged;

    // ======================== AUDIT ========================
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime lastStatusUpdate;

    // ======================== METRICS ========================
    private TripMetricsResponse metrics;
}
