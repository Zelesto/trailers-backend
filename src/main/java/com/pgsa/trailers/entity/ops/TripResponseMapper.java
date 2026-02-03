package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.TripMetricsResponse;
import com.pgsa.trailers.dto.TripResponse;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import org.springframework.stereotype.Component;

@Component
public class TripResponseMapper {

    public TripResponse toResponse(Trip trip) {
        if (trip == null) return null;

        TripResponse response = new TripResponse();

        response.setId(trip.getId());
        response.setTripNumber(trip.getTripNumber());
        response.setTripType(trip.getTripType());

        response.setOriginLocation(trip.getOriginLocation());
        response.setDestinationLocation(trip.getDestinationLocation());

        response.setPlannedStartDate(trip.getPlannedStartDate());
        response.setPlannedEndDate(trip.getPlannedEndDate());
        response.setActualStartDate(trip.getActualStartDate());
        response.setActualEndDate(trip.getActualEndDate());

        response.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
        response.setApprovalStatus(trip.getApprovalStatus());

        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());

        if (trip.getVehicle() != null) {
            response.setVehicleId(trip.getVehicle().getId());
            response.setVehicleRegistration(trip.getVehicle().getRegistrationNumber());
        }

        if (trip.getDriver() != null) {
            response.setDriverId(trip.getDriver().getId());
            response.setDriverName(
                    trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName()
            );
        }

        if (trip.getMetrics() != null) {
            response.setMetrics(toMetricsResponse(trip.getMetrics()));
        }

        return response;
    }

    private TripMetricsResponse toMetricsResponse(TripMetrics metrics) {
        TripMetricsResponse dto = new TripMetricsResponse();
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());
        return dto;
    }
}
