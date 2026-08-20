// src/main/java/com/pgsa/trailers/mapper/LoadMapper.java
package com.pgsa.trailers.mapper;

import com.pgsa.trailers.dto.LoadRequestDTO;
import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoadMapper {

    public Load toEntity(LoadRequestDTO dto) {
        if (dto == null) return null;
        
        return Load.builder()
                .loadNumber(dto.getLoadNumber())
                .referenceNumber(dto.getReferenceNumber())
                .description(dto.getDescription())
                .customerId(dto.getCustomerId())
                .weightKg(dto.getWeightKg())
                .volumeCubicM(dto.getVolumeCubicM())
                .loadingDate(dto.getLoadingDate())
                .unloadingDate(dto.getUnloadingDate())
                .commodityType(dto.getCommodityType())
                .palletCount(dto.getPalletCount())
                .containerNumber(dto.getContainerNumber())
                .hazardousMaterial(dto.getHazardousMaterial())
                .specialHandling(dto.getSpecialHandling())
                .estimatedValue(dto.getEstimatedValue())
                .actualValue(dto.getActualValue())
                .priority(dto.getPriority())
                .originLocation(dto.getOriginLocation())
                .destinationLocation(dto.getDestinationLocation())
                .handlingInstructions(dto.getHandlingInstructions())
                .packagingType(dto.getPackagingType())
                .hazardClass(dto.getHazardClass())
                .temperatureRequirements(dto.getTemperatureRequirements())
                .insurancePolicyNumber(dto.getInsurancePolicyNumber())
                .insuranceExpiry(dto.getInsuranceExpiry())
                .customsClearanceStatus(dto.getCustomsClearanceStatus())
                .warehouseId(dto.getWarehouseId())
                .supervisorId(dto.getSupervisorId())
                .build();
    }

    public LoadResponseDTO toResponseDTO(Load load) {
        if (load == null) return null;
        
        // Calculate totals from trips if available
        Integer completedTrips = 0;
        Integer pendingTrips = 0;
        Integer inProgressTrips = 0;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        
        if (load.getTrips() != null && !load.getTrips().isEmpty()) {
            completedTrips = (int) load.getTrips().stream()
                    .filter(t -> t.getStatus() != null && 
                        (t.getStatus().name().equals("COMPLETED") || 
                         t.getStatus().name().equals("FINALIZED")))
                    .count();
            
            pendingTrips = (int) load.getTrips().stream()
                    .filter(t -> t.getStatus() != null && 
                        t.getStatus().name().equals("PLANNED"))
                    .count();
            
            inProgressTrips = (int) load.getTrips().stream()
                    .filter(t -> t.getStatus() != null && 
                        (t.getStatus().name().equals("IN_PROGRESS") || 
                         t.getStatus().name().equals("ACTIVE") ||
                         t.getStatus().name().equals("ASSIGNED")))
                    .count();
            
            totalWeight = load.getTrips().stream()
                    .map(Trip::getCargoWeight)
                    .filter(w -> w != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalValue = load.getTrips().stream()
                    .map(Trip::getCargoValue)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        return LoadResponseDTO.builder()
                .id(load.getId())
                .loadNumber(load.getLoadNumber())
                .referenceNumber(load.getReferenceNumber())
                .description(load.getDescription())
                .customerId(load.getCustomerId())
                .customerName(load.getCustomer() != null ? 
                    (load.getCustomer().getName() != null ? load.getCustomer().getName() : null) : null)
                .weightKg(load.getWeightKg())
                .volumeCubicM(load.getVolumeCubicM())
                .loadingDate(load.getLoadingDate())
                .unloadingDate(load.getUnloadingDate())
                .lastStatusUpdate(load.getLastStatusUpdate())
                .status(load.getStatus() != null ? load.getStatus().name() : null)
                .priority(load.getPriority())
                .commodityType(load.getCommodityType())
                .palletCount(load.getPalletCount())
                .containerNumber(load.getContainerNumber())
                .hazardousMaterial(load.getHazardousMaterial())
                .specialHandling(load.getSpecialHandling())
                .estimatedValue(load.getEstimatedValue())
                .actualValue(load.getActualValue())
                .originLocation(load.getOriginLocation())
                .destinationLocation(load.getDestinationLocation())
                .handlingInstructions(load.getHandlingInstructions())
                .packagingType(load.getPackagingType())
                .hazardClass(load.getHazardClass())
                .temperatureRequirements(load.getTemperatureRequirements())
                .insurancePolicyNumber(load.getInsurancePolicyNumber())
                .insuranceExpiry(load.getInsuranceExpiry())
                .customsClearanceStatus(load.getCustomsClearanceStatus())
                .warehouseId(load.getWarehouseId())
                .supervisorId(load.getSupervisorId())
                .tripsCount(load.getTripsCount())
                .completedTrips(completedTrips)
                .pendingTrips(pendingTrips)
                .inProgressTrips(inProgressTrips)
                .totalDistanceKm(load.getTotalDistanceKm())
                .totalHoursActive(load.getTotalHoursActive())
                .incidentsLogged(load.getIncidentsLogged())
                .totalFromDepotKm(load.getTotalFromDepotKm())
                .totalToDepotKm(load.getTotalToDepotKm())
                .totalDepotKm(load.getTotalDepotKm())
                .totalWeight(totalWeight)
                .totalValue(totalValue)
                .statusDisplay(load.getStatusDisplay())
                .isActive(load.isActive())
                .canAcceptTrip(load.canAcceptTrip())
                .auditTrail(load.getAuditTrail())
                .createdAt(load.getCreatedAt())
                .updatedAt(load.getUpdatedAt())
                .trips(load.getTrips() != null ? 
                    load.getTrips().stream()
                        .map(this::toTripSummaryDTO)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    public TripSummaryDTO toTripSummaryDTO(Trip trip) {
        if (trip == null) return null;
        
        String driverName = null;
        if (trip.getDriver() != null) {
            String firstName = trip.getDriver().getFirstName() != null ? trip.getDriver().getFirstName() : "";
            String lastName = trip.getDriver().getLastName() != null ? trip.getDriver().getLastName() : "";
            driverName = (firstName + " " + lastName).trim();
            if (driverName.isEmpty()) driverName = null;
        }
        
        String vehicleRegistration = null;
        if (trip.getVehicle() != null) {
            vehicleRegistration = trip.getVehicle().getRegistrationNumber();
        }
        
        return TripSummaryDTO.builder()
                .id(trip.getId())
                .tripNumber(trip.getTripNumber())
                .referenceNumber(trip.getReferenceNumber())
                .status(trip.getStatus())
                .vehicleRegistration(vehicleRegistration)
                .driverName(driverName)
                .plannedStartDate(trip.getPlannedStartDate())
                .plannedEndDate(trip.getPlannedEndDate())
                .originLocation(trip.getOriginLocation())
                .destinationLocation(trip.getDestinationLocation())
                .originCity(trip.getOriginCity())
                .destinationCity(trip.getDestinationCity())
                .originZipCode(trip.getOriginZipCode())
                .destinationZipCode(trip.getDestinationZipCode())
                .originStreetAddress(trip.getOriginStreetAddress())
                .destinationStreetAddress(trip.getDestinationStreetAddress())
                .originProvince(trip.getOriginProvince())
                .destinationProvince(trip.getDestinationProvince())
                .fromDepotKm(trip.getFromDepotKm())
                .toDepotKm(trip.getToDepotKm())
                .commodityType(trip.getCommodityType())
                .cargoWeight(trip.getCargoWeight())
                .palletCount(trip.getPalletCount())
                .containerNumber(trip.getContainerNumber())
                .customerId(trip.getCustomerId())
                .customerName(trip.getCustomer() != null ? trip.getCustomer().getName() : null)
                .vehicleId(trip.getVehicle() != null ? trip.getVehicle().getId() : null)
                .driverId(trip.getDriver() != null ? trip.getDriver().getId() : null)
                .loadNumber(trip.getLoadNumber())
                .tripType(trip.getTripType() != null ? trip.getTripType().name() : null)
                .approvalStatus(trip.getApprovalStatus() != null ? trip.getApprovalStatus().name() : null)
                .actualDistanceKm(trip.getActualDistanceKm())
                .plannedDistanceKm(trip.getPlannedDistanceKm())
                .actualStartDate(trip.getActualStartDate())
                .actualEndDate(trip.getActualEndDate())
                .build();
    }
}
