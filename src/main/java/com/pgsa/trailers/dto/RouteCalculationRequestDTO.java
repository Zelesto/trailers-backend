package com.pgsa.trailers.dto;

import com.pgsa.trailers.entity.ops.Trip;
import lombok.Data;

@Data
public class RouteCalculationRequestDTO {
    private Long tripId;
    private String originLocation;
    private String destinationLocation;
    private String vehicleType;

    // Factory method to convert Trip -> DTO
    public static RouteCalculationRequestDTO fromTrip(Trip trip) {
        RouteCalculationRequestDTO dto = new RouteCalculationRequestDTO();

        dto.setOriginLocation(trip.getOriginLocation());
        dto.setDestinationLocation(trip.getDestinationLocation());
        // ✅ FIXED: Remove .name() - vehicleType is now a String
        dto.setVehicleType(trip.getVehicle() != null ? trip.getVehicle().getVehicleType() : null);
        return dto;
    }
}
