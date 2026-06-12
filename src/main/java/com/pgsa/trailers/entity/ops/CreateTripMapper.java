package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreateTripMapper {

    public Trip toEntity(
            CreateTripRequest request,
            Vehicle vehicle,
            Driver driver,
            Driver supervisor,
            Long userId
    ) {
        if (request == null) return null;

        Trip trip = new Trip();

        /* ========================
           RELATIONSHIPS
           ======================== */
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSupervisor(supervisor);

        // loadId if applicable in entity
        trip.setLoadId(request.getLoadId());

        /* ========================
           IDENTITY
           ======================== */
        trip.setTripType(request.getTripType());
        trip.setReferenceNumber(request.getReferenceNumber());
        trip.setPurchaseOrderNumber(request.getPurchaseOrderNumber());

        /* ========================
           WORKFLOW
           ======================== */
        trip.setStatus(request.getStatus());
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

        /* ========================
           SYSTEM FIELDS
           ======================== */
        trip.setCreatedAt(LocalDateTime.now());
        trip.setCreatedBy(userId);
        trip.setLastStatusUpdate(LocalDateTime.now());

        return trip;
    }
}
