package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class LoadResponseMapper {

    public LoadResponseDTO toResponse(Load load) {
        if (load == null) {
            return null;
        }

        return LoadResponseDTO.builder()
                .id(load.getId())
                .loadNumber(load.getLoadNumber())
                .referenceNumber(load.getReferenceNumber())
                .description(load.getDescription())
                .customerId(load.getCustomerId())
                .customerName(load.getCustomer() != null ? load.getCustomer().getName() : null)
                .weightKg(load.getWeightKg())
                .volumeCubicM(load.getVolumeCubicM())
                .loadingDate(load.getLoadingDate())
                .unloadingDate(load.getUnloadingDate())
                // ✅ FIXED: Remove .name() - status is now a String
                .status(load.getStatus())
                .commodityType(load.getCommodityType())
                .palletCount(load.getPalletCount())
                .containerNumber(load.getContainerNumber())
                .hazardousMaterial(load.getHazardousMaterial())
                .specialHandling(load.getSpecialHandling())
                .estimatedValue(load.getEstimatedValue())
                .actualValue(load.getActualValue())
                .priority(load.getPriority())
                // ✅ FIXED: Use tripsCount instead of tripCount
                .tripsCount(load.getTripCount())
                .trips(load.getTrips() != null ? 
                    load.getTrips().stream().map(this::toTripSummary).collect(Collectors.toList()) : 
                    null)
                .createdAt(load.getCreatedAt())
                .updatedAt(load.getUpdatedAt())
                
                .originLocation(load.getOriginLocation())
                .destinationLocation(load.getDestinationLocation())
                .handlingInstructions(load.getHandlingInstructions())
                .packagingType(load.getPackagingType())
                .hazardClass(load.getHazardClass())
                .temperatureRequirements(load.getTemperatureRequirements())
                
                .tripsCount(load.getTripsCount())
                .totalDistanceKm(load.getTotalDistanceKm())
                .totalHoursActive(load.getTotalHoursActive())
                .incidentsLogged(load.getIncidentsLogged())
                .completedTrips(load.getCompletedTrips())
                // ✅ FIXED: Use String comparison instead of .name()
                .pendingTrips(load.getTrips() != null ? 
                    (int) load.getTrips().stream()
                        .filter(t -> t.getStatus() != null && "PLANNED".equals(t.getStatus()))
                        .count() : 0)
                .inProgressTrips(load.getTrips() != null ? 
                    (int) load.getTrips().stream()
                        .filter(t -> t.getStatus() != null && "IN_PROGRESS".equals(t.getStatus()))
                        .count() : 0)
                
                .insurancePolicyNumber(load.getInsurancePolicyNumber())
                .insuranceExpiry(load.getInsuranceExpiry())
                .customsClearanceStatus(load.getCustomsClearanceStatus())
                
                .warehouseId(load.getWarehouseId())
                .supervisorId(load.getSupervisorId())
                
                .lastStatusUpdate(load.getLastStatusUpdate())
                .auditTrail(load.getAuditTrail())
                
                .totalFromDepotKm(load.getTotalFromDepotKm())
                .totalToDepotKm(load.getTotalToDepotKm())
                .totalDepotKm(load.getTotalDepotKm())
                
                .totalWeight(load.getTotalWeight())
                .totalValue(load.getTotalValue())
                .statusDisplay(load.getStatusDisplay())
                .isActive(load.isActive())
                .canAcceptTrip(load.canAcceptTrip())
                
                .mergeSuggestion(false)
                .mergeMessage(null)
                
                .build();
    }

    /**
     * Map Load to LoadResponseDTO with merge suggestion
     */
    public LoadResponseDTO toResponseWithMergeSuggestion(Load load, boolean mergeSuggestion, String mergeMessage) {
        LoadResponseDTO response = toResponse(load);
        response.setMergeSuggestion(mergeSuggestion);
        response.setMergeMessage(mergeMessage);
        return response;
    }

    /**
     * Helper method to get driver full name
     */
    private String getDriverFullName(Trip trip) {
        if (trip.getDriver() == null) {
            return null;
        }
        Driver driver = trip.getDriver();
        String firstName = driver.getFirstName() != null ? driver.getFirstName() : "";
        String lastName = driver.getLastName() != null ? driver.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }

    private TripSummaryDTO toTripSummary(Trip trip) {
        if (trip == null) {
            return null;
        }

        return TripSummaryDTO.builder()
                .id(trip.getId())
                .tripNumber(trip.getTripNumber())
                .referenceNumber(trip.getReferenceNumber())
                .status(trip.getStatus())
                .vehicleRegistration(trip.getVehicle() != null ? trip.getVehicle().getRegistrationNumber() : null)
                .driverName(getDriverFullName(trip))
                .plannedStartDate(trip.getPlannedStartDate())
                .plannedEndDate(trip.getPlannedEndDate())
                .originLocation(trip.getOriginLocation())
                .destinationLocation(trip.getDestinationLocation())
                .fromDepotKm(trip.getFromDepotKm())
                .toDepotKm(trip.getToDepotKm())
                .originCity(trip.getOriginCity())
                .destinationCity(trip.getDestinationCity())
                .originZipCode(trip.getOriginZipCode())
                .destinationZipCode(trip.getDestinationZipCode())
                .commodityType(trip.getCommodityType())
                .cargoWeight(trip.getCargoWeight())
                .palletCount(trip.getPalletCount())
                .containerNumber(trip.getContainerNumber())
                .customerId(trip.getCustomerId())
                .customerName(trip.getCustomer() != null ? trip.getCustomer().getName() : null)
                .vehicleId(trip.getVehicle() != null ? trip.getVehicle().getId() : null)
                .driverId(trip.getDriver() != null ? trip.getDriver().getId() : null)
                .loadNumber(trip.getLoadNumber())
                .tripType(trip.getTripType())
                .approvalStatus(trip.getApprovalStatus())
                .actualDistanceKm(trip.getActualDistanceKm())
                .plannedDistanceKm(trip.getPlannedDistanceKm())
                .actualStartDate(trip.getActualStartDate())
                .actualEndDate(trip.getActualEndDate())
                .originStreetAddress(trip.getOriginStreetAddress())
                .destinationStreetAddress(trip.getDestinationStreetAddress())
                .originProvince(trip.getOriginProvince())
                .destinationProvince(trip.getDestinationProvince())
                .build();
    }
}
