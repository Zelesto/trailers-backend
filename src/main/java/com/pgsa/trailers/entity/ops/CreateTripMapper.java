package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import org.springframework.stereotype.Component;

@Component
public class CreateTripMapper {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final LoadRepository loadRepository;

    public CreateTripMapper(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            LoadRepository loadRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.loadRepository = loadRepository;
    }

    public Trip toEntity(CreateTripRequest request) {
        if (request == null) {
            return null;
        }

        Trip trip = new Trip();

        /* ========================
           RELATIONSHIPS
           ======================== */
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.getReferenceById(request.getVehicleId());
            trip.setVehicle(vehicle);
        }

        if (request.getDriverId() != null) {
            Driver driver = driverRepository.getReferenceById(request.getDriverId());
            trip.setDriver(driver);
        }

        if (request.getSupervisorId() != null) {
            Driver supervisor = driverRepository.getReferenceById(request.getSupervisorId());
            trip.setSupervisor(supervisor);
        }

        if (request.getLoadId() != null) {
            Load load = loadRepository.getReferenceById(request.getLoadId());
            trip.setLoad(load);
        }

        /* ========================
           IDENTITY
           ======================== */
        trip.setTripType(request.getTripType());

        /* ========================
           WORKFLOW
           ======================== */
        trip.setStatus(request.getStatus());
        trip.setApprovalStatus(request.getApprovalStatus());

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
        trip.setIncidentsLogged(
                request.getIncidentsLogged() != null ? request.getIncidentsLogged() : 0
        );
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

        return trip;
    }
}
