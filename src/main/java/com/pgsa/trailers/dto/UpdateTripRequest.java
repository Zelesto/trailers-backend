// src/main/java/com/pgsa/trailers/dto/UpdateTripRequest.java
package com.pgsa.trailers.dto;

import com.pgsa.trailers.enums.TripStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateTripRequest {

    // ======================== ASSOCIATIONS ========================
    private Long vehicleId;
    private Long driverId;
    private Long supervisorId;
    
    // Customer and Load Associations
    private Long customerId;
    private String loadId;

    // ======================== IDENTITY ========================
    @Size(max = 50, message = "Trip type cannot exceed 50 characters")
    private String tripType;

    // ======================== ROUTE ========================
    @Size(max = 255)
    private String originLocation;
    
    @Size(max = 255)
    private String destinationLocation;

    // Detailed Origin
    @Size(max = 255)
    private String originStreetAddress;
    
    @Size(max = 100)
    private String originCity;
    
    @Size(max = 20)
    private String originZipCode;
    
    @Size(max = 100)
    private String originProvince;
    
    private Double originLatitude;
    private Double originLongitude;

    // Detailed Destination
    @Size(max = 255)
    private String destinationStreetAddress;
    
    @Size(max = 100)
    private String destinationCity;
    
    @Size(max = 20)
    private String destinationZipCode;
    
    @Size(max = 100)
    private String destinationProvince;
    
    private Double destinationLatitude;
    private Double destinationLongitude;

    // ======================== PLANNING ========================
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    
    @Positive(message = "Planned distance must be positive")
    private BigDecimal plannedDistanceKm;
    
    @Positive(message = "Planned duration must be positive")
    private BigDecimal plannedDurationHours;
    
    @Positive(message = "Estimated duration must be positive")
    private BigDecimal estimatedDurationHours;

    // ======================== EXECUTION ========================
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    
    @Positive(message = "Actual start odometer must be positive")
    private BigDecimal actualStartOdometer;
    
    @Positive(message = "Actual end odometer must be positive")
    private BigDecimal actualEndOdometer;
    
    private BigDecimal actualDistanceKm;
    private BigDecimal actualDurationHours;

    // ======================== COSTS ========================
    private BigDecimal tollCost;
    private BigDecimal otherExpenses;

    // ======================== CARGO ========================
    @Size(max = 100)
    private String commodityType;
    
    private String cargoDescription;
    
    @Positive(message = "Cargo weight must be positive")
    private BigDecimal cargoWeight;
    
    private BigDecimal cargoValue;
    
    @Positive(message = "Pallet count must be positive")
    private Integer palletCount;
    
    @Size(max = 50)
    private String containerNumber;
    
    private BigDecimal distanceKm;
    
    @Positive(message = "Fuel consumed must be positive")
    private BigDecimal fuelConsumedLiters;

    // ======================== WORKFLOW ========================
    private TripStatus status;
    private String approvalStatus;
    //private String priority;

    // ======================== NOTES ========================
    private String driverNotes;
    private String specialInstructions;
    private String notes;

    // ======================== REFERENCES ========================
    @Size(max = 100)
    private String referenceNumber;
    
    @Size(max = 100)
    private String purchaseOrderNumber;

    // ======================== CANCELLATION ========================
    private String cancellationReason;

    // ======================== ROUTE DATA ========================
    @Size(max = 255)
    private String gpsStartLocation;
    
    @Size(max = 255)
    private String gpsEndLocation;
    
    private String routeDetails;
    private String checkpoints;
}
