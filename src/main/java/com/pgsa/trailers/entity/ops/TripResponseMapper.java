package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.TripMetricsResponse;
import com.pgsa.trailers.dto.TripResponse;
import org.springframework.stereotype.Component;

@Component
public class TripResponseMapper {

    public TripResponse toResponse(Trip trip) {
        if (trip == null) {
            return null;
        }

        TripResponse response = new TripResponse();

        response.setId(trip.getId());
        response.setTripNumber(trip.getTripNumber());
        response.setTripType(trip.getTripType());

        // Locations
        response.setOriginLocation(trip.getOriginLocation());
        response.setDestinationLocation(trip.getDestinationLocation());

        // Origin details
        response.setOriginStreetAddress(trip.getOriginStreetAddress());
        response.setOriginCity(trip.getOriginCity());
        response.setOriginZipCode(trip.getOriginZipCode());
        response.setOriginProvince(trip.getOriginProvince());
        response.setOriginLatitude(trip.getOriginLatitude());
        response.setOriginLongitude(trip.getOriginLongitude());

        // Destination details
        response.setDestinationStreetAddress(trip.getDestinationStreetAddress());
        response.setDestinationCity(trip.getDestinationCity());
        response.setDestinationZipCode(trip.getDestinationZipCode());
        response.setDestinationProvince(trip.getDestinationProvince());
        response.setDestinationLatitude(trip.getDestinationLatitude());
        response.setDestinationLongitude(trip.getDestinationLongitude());

        // Dates
        response.setPlannedStartDate(trip.getPlannedStartDate());
        response.setPlannedEndDate(trip.getPlannedEndDate());
        response.setActualStartDate(trip.getActualStartDate());
        response.setActualEndDate(trip.getActualEndDate());

        // Status
        response.setStatus(
                trip.getStatus() != null
                        ? trip.getStatus().name()
                        : null
        );
        response.setApprovalStatus(trip.getApprovalStatus());

        // Audit
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());

        // Vehicle
        if (trip.getVehicle() != null) {
            response.setVehicleId(trip.getVehicle().getId());
            response.setVehicleRegistration(
                    trip.getVehicle().getRegistrationNumber()
            );
        }

        // Driver
        if (trip.getDriver() != null) {
            response.setDriverId(trip.getDriver().getId());

            String firstName = trip.getDriver().getFirstName() != null
                    ? trip.getDriver().getFirstName()
                    : "";

            String lastName = trip.getDriver().getLastName() != null
                    ? trip.getDriver().getLastName()
                    : "";

            response.setDriverName(
                    (firstName + " " + lastName).trim()
            );
        }

        // Metrics
        if (trip.getMetrics() != null) {
            response.setMetrics(
                    toMetricsResponse(trip.getMetrics())
            );
        }

        return response;
    }

    private TripMetricsResponse toMetricsResponse(TripMetrics metrics) {
        if (metrics == null) {
            return null;
        }

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

        // Optional location metrics
        dto.setOriginCityTravelTimeHours(
                metrics.getOriginCityTravelTimeHours()
        );

        dto.setDestinationCityTravelTimeHours(
                metrics.getDestinationCityTravelTimeHours()
        );

        dto.setPlannedVsActualDistanceVarianceKm(
                metrics.getPlannedVsActualDistanceVarianceKm()
        );

        dto.setPlannedVsActualDurationVarianceHours(
                metrics.getPlannedVsActualDurationVarianceHours()
        );

        dto.setGeocodingConfidenceScore(
                metrics.getGeocodingConfidenceScore()
        );

        return dto;
    }
}
