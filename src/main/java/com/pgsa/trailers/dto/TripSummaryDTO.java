// src/main/java/com/pgsa/trailers/dto/TripSummaryDTO.java
package com.pgsa.trailers.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSummaryDTO {
    private Long id;
    private String tripNumber;
    private String referenceNumber; 
    private String status;
    private String vehicleRegistration;
    private String driverName;
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    
    // Original location fields (backward compatibility)
    private String originLocation;
    private String destinationLocation;

    private BigDecimal fromDepotKm;
    private BigDecimal toDepotKm;  
    
    // City fields for display in lists/tables
    private String originCity;
    private String destinationCity;
    
    // Zip codes for reference
    private String originZipCode;
    private String destinationZipCode;
    
    // Additional fields for load management
    private String commodityType;
    private BigDecimal cargoWeight;
    private Integer palletCount;
    private String containerNumber;
    
    // ======================== ADDED FIELDS ========================
    private Long customerId;
    private String customerName;
    private Long vehicleId;
    private Long driverId;
    private String loadNumber;
    private String tripType;
    private String approvalStatus;
    private BigDecimal actualDistanceKm;
    private BigDecimal plannedDistanceKm;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;
    private String originStreetAddress;
    private String destinationStreetAddress;
    private String originProvince;
    private String destinationProvince;

    // Constructor matching the query in TripAnalyticsRepository
    public TripSummaryDTO(Long id, String tripNumber, String status, 
                          String vehicleRegistration, String driverName,
                          LocalDateTime plannedStartDate, LocalDateTime plannedEndDate,
                          String originLocation, String destinationLocation,
                          String originCity, String destinationCity,
                          String originZipCode, String destinationZipCode) {
        this.id = id;
        this.tripNumber = tripNumber;
        this.status = status;
        this.vehicleRegistration = vehicleRegistration;
        this.driverName = driverName;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
        this.originLocation = originLocation;
        this.destinationLocation = destinationLocation;
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.originZipCode = originZipCode;
        this.destinationZipCode = destinationZipCode;
    }
    
    // ======================== HELPER METHODS ========================
    
    /**
     * Get display origin (prefers city over full location)
     */
    public String getOrigin() {
        if (originCity != null && !originCity.isEmpty()) {
            return originCity;
        }
        if (originLocation != null && !originLocation.isEmpty()) {
            return originLocation;
        }
        return "N/A";
    }
    
    /**
     * Get display destination (prefers city over full location)
     */
    public String getDestination() {
        if (destinationCity != null && !destinationCity.isEmpty()) {
            return destinationCity;
        }
        if (destinationLocation != null && !destinationLocation.isEmpty()) {
            return destinationLocation;
        }
        return "N/A";
    }
    
    /**
     * Get display status
     */
    public String getStatusDisplay() {
        return status != null ? status : "N/A";
    }
    
    /**
     * Get display reference number (or trip number if reference is null)
     */
    public String getDisplayReference() {
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            return referenceNumber;
        }
        return tripNumber != null ? tripNumber : "N/A";
    }
    
    /**
     * Get full location display for origin
     */
    public String getFullOrigin() {
        StringBuilder sb = new StringBuilder();
        if (originStreetAddress != null && !originStreetAddress.isEmpty()) {
            sb.append(originStreetAddress);
        }
        if (originCity != null && !originCity.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(originCity);
        }
        if (originProvince != null && !originProvince.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(originProvince);
        }
        if (sb.length() == 0 && originLocation != null) {
            return originLocation;
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }
    
    /**
     * Get full location display for destination
     */
    public String getFullDestination() {
        StringBuilder sb = new StringBuilder();
        if (destinationStreetAddress != null && !destinationStreetAddress.isEmpty()) {
            sb.append(destinationStreetAddress);
        }
        if (destinationCity != null && !destinationCity.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(destinationCity);
        }
        if (destinationProvince != null && !destinationProvince.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(destinationProvince);
        }
        if (sb.length() == 0 && destinationLocation != null) {
            return destinationLocation;
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }
    
    /**
     * Get total depot kilometers for this trip
     */
    public BigDecimal getTotalDepotKm() {
        BigDecimal from = fromDepotKm != null ? fromDepotKm : BigDecimal.ZERO;
        BigDecimal to = toDepotKm != null ? toDepotKm : BigDecimal.ZERO;
        return from.add(to);
    }
    
    /**
     * Check if the trip has depot tracking data
     */
    public boolean hasDepotData() {
        return (fromDepotKm != null && fromDepotKm.compareTo(BigDecimal.ZERO) > 0) ||
               (toDepotKm != null && toDepotKm.compareTo(BigDecimal.ZERO) > 0);
    }
}
