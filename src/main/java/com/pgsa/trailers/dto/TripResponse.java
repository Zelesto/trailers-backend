package com.pgsa.trailers.dto;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Data
public class TripResponse {

    private Long id;
    private String tripNumber;
    private String tripType;

    private Long vehicleId;
    private String vehicleRegistration;

    private Long driverId;
    private String driverName;

    private Long loadId;

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

    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;

    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;

    private String status;
    private String approvalStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional composition
    private TripMetricsResponse metrics;
}
