// src/main/java/com/pgsa/trailers/entity/ops/TripResponseMapper.java
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

        // ======================== IDENTITY ========================
        response.setId(trip.getId());
        response.setTripNumber(trip.getTripNumber());
        response.setTripType(trip.getTripType());

        // ======================== CUSTOMER ========================
        if (trip.getCustomer() != null) {
            response.setCustomerId(trip.getCustomer().getId());
            response.setCustomerName(trip.getCustomer().getName());
            response.setCustomerCode(trip.getCustomer().getCustomerCode());
        } else if (trip.getCustomerId() != null) {
            response.setCustomerId(trip.getCustomerId());
        }

        // ======================== LOAD ========================
        if (trip.getLoad() != null) {
            response.setLoadId(trip.getLoad().getLoadNumber());
            response.setLoadNumber(trip.getLoad().getLoadNumber());
            response.setLoadType(trip.getLoad().getCommodityType());
            response.setLoadDescription(trip.getLoad().getDescription());
            response.setLoadStatus(trip.getLoad().getStatus());
        } else if (trip.getLoadId() != null) {
            response.setLoadId(trip.getLoadId());
            response.setLoadNumber(trip.getLoadNumber());
            response.setLoadType(trip.getLoadType());
            response.setLoadDescription(trip.getLoadDescription());
            response.setLoadStatus(trip.getLoadStatus());
        }

        // ======================== LOCATIONS ========================
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

        // ======================== DATES ========================
        response.setPlannedStartDate(trip.getPlannedStartDate());
        response.setPlannedEndDate(trip.getPlannedEndDate());
        response.setActualStartDate(trip.getActualStartDate());
        response.setActualEndDate(trip.getActualEndDate());

        // ======================== STATUS ========================
        response.setStatus(trip.getStatus());
        response.setApprovalStatus(trip.getApprovalStatus());
       // response.setPriority(trip.getPriority());

        // ======================== AUDIT ========================
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());
        response.setCreatedBy(trip.getCreatedBy());
        response.setUpdatedBy(trip.getUpdatedBy());

        // ======================== VEHICLE ========================
        if (trip.getVehicle() != null) {
            response.setVehicleId(trip.getVehicle().getId());
            response.setVehicleRegistration(
                    trip.getVehicle().getRegistrationNumber()
            );
            response.setVehicleMake(trip.getVehicle().getMake());
            response.setVehicleModel(trip.getVehicle().getModel());
        }

        // ======================== DRIVER ========================
        if (trip.getDriver() != null) {
            response.setDriverId(trip.getDriver().getId());
            String firstName = trip.getDriver().getFirstName() != null
                    ? trip.getDriver().getFirstName()
                    : "";
            String lastName = trip.getDriver().getLastName() != null
                    ? trip.getDriver().getLastName()
                    : "";
            response.setDriverName((firstName + " " + lastName).trim());
        }

        // ======================== SUPERVISOR ========================
        if (trip.getSupervisor() != null) {
            response.setSupervisorId(trip.getSupervisor().getId());
            String firstName = trip.getSupervisor().getFirstName() != null
                    ? trip.getSupervisor().getFirstName()
                    : "";
            String lastName = trip.getSupervisor().getLastName() != null
                    ? trip.getSupervisor().getLastName()
                    : "";
            response.setSupervisorName((firstName + " " + lastName).trim());
        }

        // ======================== CARGO ========================
        response.setCommodityType(trip.getCommodityType());
        response.setCargoDescription(trip.getCargoDescription());
        response.setCargoWeight(trip.getCargoWeight());
        response.setCargoValue(trip.getCargoValue());
        response.setPalletCount(trip.getPalletCount());
        response.setContainerNumber(trip.getContainerNumber());

        // ======================== PLANNING ========================
        response.setPlannedDistanceKm(trip.getPlannedDistanceKm());
        response.setPlannedDurationHours(trip.getPlannedDurationHours());
        response.setEstimatedDurationHours(trip.getEstimatedDurationHours());

        // ======================== EXECUTION ========================
        response.setActualStartOdometer(trip.getActualStartOdometer());
        response.setActualEndOdometer(trip.getActualEndOdometer());
        response.setActualDistanceKm(trip.getActualDistanceKm());
        response.setActualDurationHours(trip.getActualDurationHours());

        // ======================== COSTS ========================
        response.setTollCost(trip.getTollCost());
        response.setOtherExpenses(trip.getOtherExpenses());
        response.setCostAmount(trip.getCostAmount());
        response.setRevenueAmount(trip.getRevenueAmount());

        // ======================== FUEL ========================
        response.setFuelConsumedLiters(trip.getFuelConsumedLiters());

        // ======================== ROUTE ========================
        response.setGpsStartLocation(trip.getGpsStartLocation());
        response.setGpsEndLocation(trip.getGpsEndLocation());
        response.setRouteDetails(trip.getRouteDetails());
        response.setCheckpoints(trip.getCheckpoints());

        // ======================== NOTES ========================
        response.setNotes(trip.getNotes());
        response.setSpecialInstructions(trip.getSpecialInstructions());
        response.setDriverNotes(trip.getDriverNotes());

        // ======================== REFERENCES ========================
        response.setReferenceNumber(trip.getReferenceNumber());
        response.setPurchaseOrderNumber(trip.getPurchaseOrderNumber());

        // ======================== OPERATIONS ========================
        response.setIncidentsLogged(trip.getIncidentsLogged());
        response.setCancellationReason(trip.getCancellationReason());

        // ======================== METRICS ========================
        if (trip.getMetrics() != null) {
            response.setMetrics(toMetricsResponse(trip.getMetrics()));
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
        dto.setOriginCityTravelTimeHours(metrics.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(metrics.getGeocodingConfidenceScore());

        return dto;
    }
}
