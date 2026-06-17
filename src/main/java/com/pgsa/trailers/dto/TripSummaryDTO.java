package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSummaryDTO {
    private Long id;
    private String tripNumber;
    private String status;
    private String vehicleRegistration;
    private String driverName;
    private LocalDateTime plannedStartDate;
    private LocalDateTime endDate;
    
    // Original location fields (backward compatibility)
    private String originLocation;
    private String destinationLocation;
    
    // New city fields for display in lists/tables
    private String originCity;
    private String destinationCity;
    
    // Zip codes for reference
    private String originZipCode;
    private String destinationZipCode;
    
    // Quick metrics for summary view
    private BigDecimal totalDistanceKm;
    private BigDecimal plannedDistanceKm;

    // Constructor matching the JPQL query in TripAnalyticsRepository
    // Order must match exactly: id, tripNumber, status, vehicleRegistration, driverName,
    // plannedStartDate, endDate, originLocation, destinationLocation, originCity,
    // destinationCity, originZipCode, destinationZipCode, totalDistanceKm, plannedDistanceKm
    public TripSummaryDTO(Long id, String tripNumber, String status, String vehicleRegistration,
                          String driverName, LocalDateTime plannedStartDate, LocalDateTime endDate,
                          String originLocation, String destinationLocation, String originCity,
                          String destinationCity, String originZipCode, String destinationZipCode,
                          BigDecimal totalDistanceKm, BigDecimal plannedDistanceKm) {
        this.id = id;
        this.tripNumber = tripNumber;
        this.status = status;
        this.vehicleRegistration = vehicleRegistration;
        this.driverName = driverName;
        this.plannedStartDate = plannedStartDate;
        this.endDate = endDate;
        this.originLocation = originLocation;
        this.destinationLocation = destinationLocation;
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.originZipCode = originZipCode;
        this.destinationZipCode = destinationZipCode;
        this.totalDistanceKm = totalDistanceKm != null ? totalDistanceKm : BigDecimal.ZERO;
        this.plannedDistanceKm = plannedDistanceKm != null ? plannedDistanceKm : BigDecimal.ZERO;
    }
}
