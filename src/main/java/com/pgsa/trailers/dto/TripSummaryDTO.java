// src/main/java/com/pgsa/trailers/dto/TripSummaryDTO.java
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
public class TripSummaryDTO {
    private Long id;
    private String tripNumber;
    private TripStatus status;
    private String vehicleRegistration;
    private String driverName;
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    
    // Original location fields (backward compatibility)
    private String originLocation;
    private String destinationLocation;
    
    // City fields for display in lists/tables
    private String originCity;
    private String destinationCity;
    
    // Zip codes for reference
    private String originZipCode;
    private String destinationZipCode;
    
    // Additional fields for load management
    private String origin;
    private String destination;
    private String commodityType;
    private BigDecimal cargoWeight;
    private Integer palletCount;
    private String containerNumber;
    
    // Helper method to get display origin
    public String getOrigin() {
        if (originCity != null && !originCity.isEmpty()) {
            return originCity;
        }
        if (originLocation != null && !originLocation.isEmpty()) {
            return originLocation;
        }
        return "N/A";
    }
    
    // Helper method to get display destination
    public String getDestination() {
        if (destinationCity != null && !destinationCity.isEmpty()) {
            return destinationCity;
        }
        if (destinationLocation != null && !destinationLocation.isEmpty()) {
            return destinationLocation;
        }
        return "N/A";
    }
    
    // Helper method to get display status
    public String getStatusDisplay() {
        return status != null ? status.name() : "N/A";
    }
}
