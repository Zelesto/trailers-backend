package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
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
    
}
