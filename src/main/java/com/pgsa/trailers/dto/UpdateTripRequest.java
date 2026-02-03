package com.pgsa.trailers.dto;

import com.pgsa.trailers.enums.TripStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateTripRequest {

    // Associations
    private Long vehicleId;   // Optional
    private Long driverId;    // Optional

    // Route
    @Size(max = 255)
    private String originLocation;

    @Size(max = 255)
    private String destinationLocation;

    // Execution
    private LocalDateTime actualStartDate;
    private LocalDateTime actualEndDate;

    // Lifecycle
    private TripStatus status;
}
