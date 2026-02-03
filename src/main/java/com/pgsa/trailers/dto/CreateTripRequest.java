package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateTripRequest {

    private Long vehicleId;
    private Long driverId;
    private Long loadId;

    private String tripType;

    private String originLocation;
    private String destinationLocation;

    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
}
