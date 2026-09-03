package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
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
        response.setCustomer(toCustomerResponseDTO(trip.getCustomer()));
        if (trip.getCustomer() != null) {
            response.setCustomerId(trip.getCustomer().getId());
            response.setCustomerName(trip.getCustomer().getName());
            response.setCustomerCode(trip.getCustomer().getCustomerCode());
        } else if (trip.getCustomerId() != null) {
            response.setCustomerId(trip.getCustomerId());
        }

        // ======================== LOAD ========================
        response.setLoad(toLoadResponseDTO(trip.getLoad()));
        if (trip.getLoad() != null) {
            response.setLoadId(trip.getLoad().getLoadNumber());
            response.setLoadNumber(trip.getLoad().getLoadNumber());
            response.setLoadType(trip.getLoad().getCommodityType());
            response.setLoadDescription(trip.getLoad().getDescription());
            // ✅ FIXED: Remove .name() - status is now a String
            response.setLoadStatus(trip.getLoad().getStatus());
        } else if (trip.getLoadId() != null) {
            response.setLoadId(trip.getLoadId());
            response.setLoadNumber(trip.getLoadNumber());
            response.setLoadType(trip.getLoadType());
            response.setLoadDescription(trip.getLoadDescription());
            response.setLoadStatus(trip.getLoadStatus());
        }

        // ======================== VEHICLE ========================
        response.setVehicle(toVehicleDTO(trip.getVehicle()));
        if (trip.getVehicle() != null) {
            response.setVehicleId(trip.getVehicle().getId());
            response.setVehicleRegistration(trip.getVehicle().getRegistrationNumber());
            response.setVehicleMake(trip.getVehicle().getMake());
            response.setVehicleModel(trip.getVehicle().getModel());
        }

        // ======================== CRANE USAGE ========================
        response.setCraneUsed(trip.getCraneUsed());

        // ======================== DRIVER ========================
        response.setDriver(toDriverDTO(trip.getDriver()));
        if (trip.getDriver() != null) {
            response.setDriverId(trip.getDriver().getId());
            String firstName = trip.getDriver().getFirstName() != null 
                ? trip.getDriver().getFirstName() 
                : "";
            String lastName = trip.getDriver().getLastName() != null 
                ? trip.getDriver().getLastName() 
                : "";
            String fullName = (firstName + " " + lastName).trim();
            response.setDriverName(fullName.isEmpty() ? null : fullName);
            response.setDriverLicenseNumber(trip.getDriver().getLicenseNumber());
        }

        // ======================== SUPERVISOR ========================
        response.setSupervisor(toDriverDTO(trip.getSupervisor()));
        if (trip.getSupervisor() != null) {
            response.setSupervisorId(trip.getSupervisor().getId());
            String firstName = trip.getSupervisor().getFirstName() != null 
                ? trip.getSupervisor().getFirstName() 
                : "";
            String lastName = trip.getSupervisor().getLastName() != null 
                ? trip.getSupervisor().getLastName() 
                : "";
            String fullName = (firstName + " " + lastName).trim();
            response.setSupervisorName(fullName.isEmpty() ? null : fullName);
        }

        // ======================== LOCATIONS ========================
        response.setOriginLocation(trip.getOriginLocation());
        response.setOriginStreetAddress(trip.getOriginStreetAddress());
        response.setOriginCity(trip.getOriginCity());
        response.setOriginZipCode(trip.getOriginZipCode());
        response.setOriginProvince(trip.getOriginProvince());
        response.setOriginLatitude(trip.getOriginLatitude());
        response.setOriginLongitude(trip.getOriginLongitude());

        response.setDestinationLocation(trip.getDestinationLocation());
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
        response.setApprovedAt(trip.getApprovedAt());

        // ======================== AUDIT ========================
        response.setCreatedAt(trip.getCreatedAt());
        response.setUpdatedAt(trip.getUpdatedAt());
        response.setCreatedBy(trip.getCreatedBy());
        response.setUpdatedBy(trip.getUpdatedBy());
        response.setLastStatusUpdate(trip.getLastStatusUpdate());

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

        // ======================== METRICS ========================
        response.setDistanceKm(trip.getActualDistanceKm());
        response.setFuelConsumedLiters(trip.getFuelConsumedLiters());

        // ======================== COSTS ========================
        response.setTollCost(trip.getTollCost());
        response.setOtherExpenses(trip.getOtherExpenses());
        response.setCostAmount(trip.getCostAmount());
        response.setRevenueAmount(trip.getRevenueAmount());

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

        // ======================== DEPOT TRACKING ========================
        response.setFromDepotKm(trip.getFromDepotKm());
        response.setToDepotKm(trip.getToDepotKm());
        response.setDepartedFrom(trip.getDepartedFrom());
        response.setDepartureLocation(trip.getDepartureLocation());
        response.setIsFromDepot(trip.getIsFromDepot());

        // ======================== OPERATIONS ========================
        response.setIncidentsLogged(trip.getIncidentsLogged());
        response.setCancellationReason(trip.getCancellationReason());
        response.setCancelledAt(trip.getCancelledAt());

        // ======================== METRICS ========================
        if (trip.getMetrics() != null) {
            response.setMetrics(toTripMetricsResponse(trip.getMetrics()));
        }

        return response;
    }

    // ======================== DTO CONVERTERS ========================

    /**
     * Convert Customer entity to CustomerResponseDTO
     */
    private CustomerResponseDTO toCustomerResponseDTO(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .name(customer.getName())
                .registrationNumber(customer.getRegistrationNumber())
                .vatNumber(customer.getVatNumber())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .city(customer.getCity())
                .province(customer.getProvince())
                .postalCode(customer.getPostalCode())
                .country(customer.getCountry())
                .contactPerson(customer.getContactPerson())
                .contactPhone(customer.getContactPhone())
                .contactEmail(customer.getContactEmail())
                .paymentTerms(customer.getPaymentTerms())
                .creditLimit(customer.getCreditLimit())
                .isActive(customer.getIsActive())
                .notes(customer.getNotes())
                .createdAt(customer.getCreatedAt())
                .createdBy(customer.getCreatedBy())
                .updatedAt(customer.getUpdatedAt())
                .updatedBy(customer.getUpdatedBy())
                .build();
    }

    /**
     * Convert Vehicle entity to VehicleDTO - Safe version with null/exception handling
     */
    private VehicleDTO toVehicleDTO(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        
        try {
            dto.setRegistrationNumber(vehicle.getRegistrationNumber());
        } catch (Exception e) {
            log.warn("Could not load registration number for vehicle {}: {}", vehicle.getId(), e.getMessage());
        }
        
        try {
            dto.setMake(vehicle.getMake());
        } catch (Exception e) {
            log.warn("Could not load make for vehicle {}: {}", vehicle.getId(), e.getMessage());
        }
        
        try {
            dto.setModel(vehicle.getModel());
        } catch (Exception e) {
            log.warn("Could not load model for vehicle {}: {}", vehicle.getId(), e.getMessage());
        }
        
        try {
            dto.setIsActive(vehicle.getIsActive());
        } catch (Exception e) {
            log.warn("Could not load isActive for vehicle {}: {}", vehicle.getId(), e.getMessage());
        }
        
        return dto;
    }

    /**
     * Convert Driver entity to DriverDTO
     */
    private DriverDTO toDriverDTO(Driver driver) {
        if (driver == null) {
            return null;
        }

        DriverDTO dto = new DriverDTO();
        dto.setId(driver.getId());
        dto.setFirstName(driver.getFirstName());
        dto.setLastName(driver.getLastName());
        dto.setLicenseNumber(driver.getLicenseNumber());
        dto.setIsActive(driver.getIsActive());
        
        return dto;
    }

    /**
     * Convert Load entity to LoadResponseDTO - FIXED
     */
    private LoadResponseDTO toLoadResponseDTO(Load load) {
        if (load == null) {
            return null;
        }

        LoadResponseDTO.LoadResponseDTOBuilder builder = LoadResponseDTO.builder()
                .id(load.getId())
                .loadNumber(load.getLoadNumber())
                .referenceNumber(load.getReferenceNumber())
                .customerId(load.getCustomerId())
                .description(load.getDescription())
                .commodityType(load.getCommodityType())
                // ✅ FIXED: Remove .name() - status is now a String
                .status(load.getStatus())
                .tripsCount(load.getTripsCount())
                .originLocation(load.getOriginLocation())
                .destinationLocation(load.getDestinationLocation())
                .totalFromDepotKm(load.getTotalFromDepotKm())
                .totalToDepotKm(load.getTotalToDepotKm())
                .totalDepotKm(load.getTotalDepotKm())
                .createdAt(load.getCreatedAt())
                .updatedAt(load.getUpdatedAt())
                .lastStatusUpdate(load.getLastStatusUpdate());

        return builder.build();
    }

    /**
     * Convert TripMetrics entity to TripMetricsResponse
     */
    private TripMetricsResponse toTripMetricsResponse(TripMetrics metrics) {
        if (metrics == null) {
            return null;
        }

        TripMetricsResponse dto = new TripMetricsResponse();

        // Basic metrics
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());

        // Incident & tasks
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());

        // Financial
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());

        // Variance
        dto.setOriginCityTravelTimeHours(metrics.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(metrics.getGeocodingConfidenceScore());

        // Audit
        dto.setCreatedAt(metrics.getCreatedAt());
        dto.setUpdatedAt(metrics.getUpdatedAt());

        return dto;
    }
}
