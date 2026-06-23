// src/main/java/com/pgsa/trailers/entity/ops/CreateTripMapper.java
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

import java.time.LocalDateTime;

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
           RELATIONSHIPS
           ======================== */
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + request.getVehicleId()));
            trip.setVehicle(vehicle);
        }

        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + request.getDriverId()));
            trip.setDriver(driver);
        }

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
            // Load ID is a String (loadNumber), not a Long
            Load load = loadRepository.findByLoadNumber(request.getLoadId())
                    .orElseThrow(() -> new IllegalArgumentException("Load not found with number: " + request.getLoadId()));
            trip.setLoad(load);
            trip.setLoadId(load.getLoadNumber());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus());
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
        trip.setPriority(request.getPriority());

        /* ========================
           PLANNING
           ======================== */
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());
        trip.setPlannedDistanceKm(request.getPlannedDistanceKm());
        trip.setPlannedDurationHours(request.getPlannedDurationHours());
        trip.setEstimatedDurationHours(request.getEstimatedDurationHours());

        /* ========================
           COSTS
           ======================== */
        trip.setTollCost(request.getTollCost());
        trip.setOtherExpenses(request.getOtherExpenses());

        /* ========================
           CARGO
           ======================== */
        trip.setCommodityType(request.getCommodityType());
        trip.setCargoDescription(request.getCargoDescription());
        trip.setCargoWeight(request.getCargoWeight());
        trip.setCargoValue(request.getCargoValue());
        trip.setPalletCount(request.getPalletCount());
        trip.setContainerNumber(request.getContainerNumber());

        trip.setDistanceKm(request.getDistanceKm());
        trip.setFuelConsumedLiters(request.getFuelConsumedLiters());

        /* ========================
           ORIGIN
           ======================== */
        trip.setOriginLocation(request.getOriginLocation());
        trip.setOriginStreetAddress(request.getOriginStreetAddress());
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginZipCode(request.getOriginZipCode());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());

        /* ========================
           DESTINATION
           ======================== */
        trip.setDestinationLocation(request.getDestinationLocation());
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
        
        // Build location strings from components if needed
        if (request.getOriginLocation() == null && trip.buildOriginAddress() != null && !trip.buildOriginAddress().isEmpty()) {
            trip.updateOriginLocationFromComponents();
        }
        
        if (request.getDestinationLocation() == null && trip.buildDestinationAddress() != null && !trip.buildDestinationAddress().isEmpty()) {
            trip.updateDestinationLocationFromComponents();
        }

        log.debug("Mapped CreateTripRequest to Trip entity");
        
        return trip;
    }
}
