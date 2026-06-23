// src/main/java/com/pgsa/trailers/dto/TripDTO.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
public class TripDTO {
    private Long id;
    private String tripNumber;
    private Long vehicleId;
    private String vehicleRegNumber;
    private Long driverId;
    private String driverName;

    // Customer fields
    private Long customerId;
    private String customerName;
    private String customerCode;

    // Load fields
    private String loadId;
    private String loadNumber;
    private String loadType;
    private String loadDescription;
    private String loadStatus;

    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    
    // Original location fields (backward compatibility)
    private String originLocation;
    private String destinationLocation;
    
    // New detailed origin address fields
    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private Double originLatitude;
    private Double originLongitude;
    
    // New detailed destination address fields
    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private Double destinationLatitude;
    private Double destinationLongitude;
    
    private String status;
    private String priority;
    private String tripType;

    // Distance and duration
    private BigDecimal plannedDistanceKm;
    private BigDecimal actualDistanceKm;
    private BigDecimal estimatedDurationHours;
    private BigDecimal actualDurationHours;

    // Financial fields
    private BigDecimal tollCost;
    private BigDecimal otherExpenses;
    private BigDecimal costAmount;

    // Fuel
    private BigDecimal fuelConsumedLiters;
    private BigDecimal fuelEfficiency;

    // Odometer readings
    private BigDecimal actualStartOdometer;
    private BigDecimal actualEndOdometer;

    // Notes
    private String notes;
    private String driverNotes;
    private String specialInstructions;

    // Cargo
    private String cargoDescription;
    private String commodityType;
    private BigDecimal cargoWeight;
    private BigDecimal cargoValue;
    private Integer palletCount;
    private String containerNumber;

    // Audit fields
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime lastStatusUpdate;

    // Add metrics if you want to include them
    private TripMetricsDTO metrics;
}
