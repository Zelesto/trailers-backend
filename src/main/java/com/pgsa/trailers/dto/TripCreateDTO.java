package com.pgsa.trailers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripCreateDTO {
    private String tripNumber;
    private String tripType;
    private Long vehicleId;
    private Long driverId;
    private Long loadId;
    
    // Origin address components
    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    
    // Destination address components
    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    
    private LocalDateTime plannedStartDate;
    private LocalDateTime plannedEndDate;
    
    // Route calculation preferences
    private String vehicleType; // CAR, TRUCK, HGV, etc.
    private boolean calculateRoute = true;
}
