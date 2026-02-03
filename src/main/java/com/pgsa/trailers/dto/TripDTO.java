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
    private String originLocation;
    private String destinationLocation;
    private String status;
    private String cargoDescription;
    private String priority;

    // Add metrics if you want to include them
    private TripMetricsDTO metrics;
}