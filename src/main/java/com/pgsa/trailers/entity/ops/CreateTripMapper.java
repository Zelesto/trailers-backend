package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateTripMapper {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final LoadRepository loadRepository;
    private final CustomerRepository customerRepository;

    public Trip toEntity(CreateTripRequest request) {
        if (request == null) {
            return null;
        }

        Trip trip = new Trip();

        /* ========================
           GENERATE TRIP NUMBER (REQUIRED)
           ======================== */
        trip.setTripNumber(generateTripNumber());

        /* ========================
           REQUIRED RELATIONSHIPS
           ======================== */
        // Vehicle is REQUIRED (nullable = false)
        if (request.getVehicleId() == null) {
            throw new IllegalArgumentException("Vehicle ID is required for trip creation");
        }
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + request.getVehicleId()));
        trip.setVehicle(vehicle);

        // Driver is optional
        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + request.getDriverId()));
            trip.setDriver(driver);
        }

        // Supervisor is optional
        if (request.getSupervisorId() != null) {
            Driver supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new IllegalArgumentException("Supervisor not found with ID: " + request.getSupervisorId()));
            trip.setSupervisor(supervisor);
        }

        /* ========================
           CUSTOMER RELATIONSHIP
           ======================== */
        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + request.getCustomerId()));
            trip.setCustomerId(customer.getId());
        }

        /* ========================
           LOAD RELATIONSHIP
           ======================== */
        if (request.getLoadId() != null && !request.getLoadId().isEmpty()) {
            try {
                Load load = loadRepository.findByLoadNumber(request.getLoadId())
                        .orElse(null);
                if (load != null) {
                    trip.setLoad(load);
                    trip.setLoadId(load.getLoadNumber());
                    trip.setLoadNumber(load.getLoadNumber());
                    trip.setLoadType(load.getCommodityType());
                    trip.setLoadDescription(load.getDescription());
                    trip.setLoadStatus(load.getStatus());
                } else {
                    log.warn("Load not found with number: {}", request.getLoadId());
                }
            } catch (Exception e) {
                log.warn("Error loading load with number: {}", request.getLoadId(), e);
            }
        }

        /* ========================
           REQUIRED LOCATIONS (nullable = false)
           ======================== */
        // Set origin location - must not be null
        if (request.getOriginLocation() != null && !request.getOriginLocation().isEmpty()) {
            trip.setOriginLocation(request.getOriginLocation());
        } else {
            // Build from components or set a default
            String builtOrigin = buildLocationFromComponents(
                request.getOriginStreetAddress(),
                request.getOriginCity(),
                request.getOriginZipCode(),
                request.getOriginProvince()
            );
            trip.setOriginLocation(builtOrigin.isEmpty() ? "Unknown Origin" : builtOrigin);
        }

        // Set destination location - must not be null
        if (request.getDestinationLocation() != null && !request.getDestinationLocation().isEmpty()) {
            trip.setDestinationLocation(request.getDestinationLocation());
        } else {
            // Build from components or set a default
            String builtDestination = buildLocationFromComponents(
                request.getDestinationStreetAddress(),
                request.getDestinationCity(),
                request.getDestinationZipCode(),
                request.getDestinationProvince()
            );
            trip.setDestinationLocation(builtDestination.isEmpty() ? "Unknown Destination" : builtDestination);
        }

        /* ========================
           IDENTITY
           ======================== */
        trip.setTripType(request.getTripType());

        /* ========================
           WORKFLOW
           ======================== */
        trip.setStatus(request.getStatus() != null ? request.getStatus() : TripStatus.DRAFT);
        trip.setApprovalStatus(request.getApprovalStatus());

        /* ========================
           PLANNING
           ======================== */
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());
        
        if (request.getPlannedDistanceKm() != null) {
            trip.setPlannedDistanceKm(BigDecimal.valueOf(request.getPlannedDistanceKm()));
        }
        
        if (request.getPlannedDurationHours() != null) {
            trip.setPlannedDurationHours(BigDecimal.valueOf(request.getPlannedDurationHours()));
        }
        
        if (request.getEstimatedDurationHours() != null) {
            trip.setEstimatedDurationHours(BigDecimal.valueOf(request.getEstimatedDurationHours()));
        }

        /* ========================
           COSTS
           ======================== */
        if (request.getTollCost() != null) {
            trip.setTollCost(BigDecimal.valueOf(request.getTollCost()));
        }
        if (request.getOtherExpenses() != null) {
            trip.setOtherExpenses(BigDecimal.valueOf(request.getOtherExpenses()));
        }

        /* ========================
           CARGO
           ======================== */
        trip.setCommodityType(request.getCommodityType());
        trip.setCargoDescription(request.getCargoDescription());
        
        if (request.getCargoWeight() != null) {
            trip.setCargoWeight(BigDecimal.valueOf(request.getCargoWeight()));
        }
        
        if (request.getCargoValue() != null) {
            trip.setCargoValue(BigDecimal.valueOf(request.getCargoValue()));
        }
        
        trip.setPalletCount(request.getPalletCount());
        trip.setContainerNumber(request.getContainerNumber());

        if (request.getDistanceKm() != null) {
            trip.setDistanceKm(BigDecimal.valueOf(request.getDistanceKm()));
        }
        
        if (request.getFuelConsumedLiters() != null) {
            trip.setFuelConsumedLiters(BigDecimal.valueOf(request.getFuelConsumedLiters()));
        }

        /* ========================
           ORIGIN DETAILS
           ======================== */
        trip.setOriginStreetAddress(request.getOriginStreetAddress());
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginZipCode(request.getOriginZipCode());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());

        /* ========================
           DESTINATION DETAILS
           ======================== */
        trip.setDestinationStreetAddress(request.getDestinationStreetAddress());
        trip.setDestinationCity(request.getDestinationCity());
        trip.setDestinationZipCode(request.getDestinationZipCode());
        trip.setDestinationProvince(request.getDestinationProvince());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());

        /* ========================
           NOTES
           ======================== */
        trip.setNotes(request.getNotes());
        trip.setSpecialInstructions(request.getSpecialInstructions());
        trip.setDriverNotes(request.getDriverNotes());

        /* ========================
           REFERENCES
           ======================== */
        trip.setReferenceNumber(request.getReferenceNumber());
        trip.setPurchaseOrderNumber(request.getPurchaseOrderNumber());

        /* ========================
           OPERATIONS
           ======================== */
        trip.setIncidentsLogged(request.getIncidentsLogged() != null ? request.getIncidentsLogged() : 0);
        trip.setCancellationReason(request.getCancellationReason());

        /* ========================
           ROUTE DATA
           ======================== */
        trip.setGpsStartLocation(request.getGpsStartLocation());
        trip.setGpsEndLocation(request.getGpsEndLocation());
        trip.setRouteDetails(request.getRouteDetails());
        trip.setCheckpoints(request.getCheckpoints());

        /* ========================
           AUDIT
           ======================== */
        trip.setAuditTrail(request.getAuditTrail());

        /* ========================
           DEFAULT VALUES
           ======================== */
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setIsActive(true);

        log.debug("Mapped CreateTripRequest to Trip entity with tripNumber: {}", trip.getTripNumber());
        
        return trip;
    }

    /**
     * Generate a unique trip number
     * Format: TRP-YYYYMMDD-XXXXX
     */
    private String generateTripNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "TRP-" + datePart + "-" + randomPart;
    }

    /**
     * Build a location string from components
     */
    private String buildLocationFromComponents(String street, String city, String zip, String province) {
        StringBuilder location = new StringBuilder();
        
        if (street != null && !street.isBlank()) {
            location.append(street);
        }
        
        if (city != null && !city.isBlank()) {
            if (!location.isEmpty()) location.append(", ");
            location.append(city);
        }
        
        if (zip != null && !zip.isBlank()) {
            if (!location.isEmpty()) location.append(" ");
            location.append(zip);
        }
        
        if (province != null && !province.isBlank()) {
            if (!location.isEmpty()) location.append(", ");
            location.append(province);
        }
        
        return location.toString();
    }
}
