package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TripDTO {
    private Long id;
    private String tripNumber;
    private Long vehicleId;
    private String vehicleRegNumber;
    private Long driverId;
    private String driverName;

    private LocalDateTime plannedStartDate;
    private LocalDateTime endDate;
    
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
    //private String cargoDescription;
    //private String priority;

    // Add metrics if you want to include them
    private TripMetricsDTO metrics;
}
