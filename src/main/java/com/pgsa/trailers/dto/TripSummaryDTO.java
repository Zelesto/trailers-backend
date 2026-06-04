package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    // New detailed origin address fields
    private String originCity;
    private String originZipCode;
    private String originProvince;
    
    // New detailed destination address fields
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    
    // Optional: Add metrics summary for quick view
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;
    
    // Optional: Add formatted addresses for display
    private String originDisplayAddress;
    private String destinationDisplayAddress;
}
