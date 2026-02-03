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

    private String originLocation;
    private String destinationLocation;

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
