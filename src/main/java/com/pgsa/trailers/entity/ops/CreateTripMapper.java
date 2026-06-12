package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import org.springframework.stereotype.Component;

@Component
public class CreateTripMapper {

    public Trip toEntity(CreateTripRequest request) {
        if (request == null) {
            return null;
        }

        Trip trip = new Trip();

        // Core
        trip.setTripType(request.getTripType());
        trip.setStatus(request.getStatus());
        trip.setApprovalStatus(request.getApprovalStatus());
        trip.setPriority(request.getPriority());

        // Planning
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());
        trip.setEstimatedDuration(request.getEstimatedDuration());
        trip.setPlannedDistanceKm(request.getPlannedDistanceKm());
        trip.setPlannedDurationHours(request.getPlannedDurationHours());

        // Costs
        trip.setTollCost(request.getTollCost());
        trip.setOtherExpenses(request.getOtherExpenses());

        // Cargo
        trip.setCommodityType(request.getCommodityType());
        trip.setCargoDescription(request.getCargoDescription());
        trip.setCargoWeight(request.getCargoWeight());
        trip.setCargoValue(request.getCargoValue());
        trip.setPalletCount(request.getPalletCount());
        trip.setContainerNumber(request.getContainerNumber());

        // Origin
        trip.setOriginStreetAddress(request.getOriginStreetAddress());
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginZipCode(request.getOriginZipCode());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());
        trip.setOriginLocation(request.getOriginLocation());

        // Destination
        trip.setDestinationStreetAddress(request.getDestinationStreetAddress());
        trip.setDestinationCity(request.getDestinationCity());
        trip.setDestinationZipCode(request.getDestinationZipCode());
        trip.setDestinationProvince(request.getDestinationProvince());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());
        trip.setDestinationLocation(request.getDestinationLocation());

        // Notes
        trip.setNotes(request.getNotes());
        trip.setSpecialInstructions(request.getSpecialInstructions());
        trip.setDriverNotes(request.getDriverNotes());

        // References
        trip.setReferenceNumber(request.getReferenceNumber());
        trip.setPurchaseOrderNumber(request.getPurchaseOrderNumber());

        // Other
        trip.setCancellationReason(request.getCancellationReason());
        trip.setAuditTrail(request.getAuditTrail());
        trip.setIncidentsLogged(request.getIncidentsLogged());

        // Route settings
        trip.setVehicleType(request.getVehicleType());

        // Relationships are set in service layer:
        // vehicle
        // driver
        // supervisor
        // load
        // tripNumber generation

        return trip;
    }
}
