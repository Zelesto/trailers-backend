package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreateTripMapper {

    public Trip toEntity(CreateTripRequest request,
                         Vehicle vehicle,
                         Driver driver,
                         Driver supervisor,
                         Long createdBy) {

        if (request == null) return null;

        Trip trip = new Trip();

        /* REQUIRED RELATIONSHIPS */
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSupervisor(supervisor);

        /* IDENTITY */
        trip.setTripType(request.getTripType());

        /* STATUS */
        trip.setStatus(request.getStatus());
        trip.setApprovalStatus(request.getApprovalStatus());

        /* PLANNING */
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());

        /* LOCATION */
        trip.setOriginLocation(request.getOriginLocation());
        trip.setDestinationLocation(request.getDestinationLocation());

        /* OPTIONAL COMPONENTS (if used) */
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setDestinationCity(request.getDestinationCity());
        trip.setDestinationProvince(request.getDestinationProvince());

        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());

        /* COSTS */
        trip.setTollCost(request.getTollCost());
        trip.setOtherExpenses(request.getOtherExpenses());

        /* METRICS */
        trip.setDistanceKm(request.getDistanceKm());
        trip.setEstimatedDurationHours(request.getEstimatedDurationHours());
        trip.setFuelConsumedLiters(request.getFuelConsumedLiters());

        /* NOTES */
        trip.setDriverNotes(request.getDriverNotes());

        /* INCIDENTS */
        trip.setIncidentsLogged(
                request.getIncidentsLogged() != null ? request.getIncidentsLogged() : 0
        );

        /* CANCELLATION */
        trip.setCancellationReason(request.getCancellationReason());

        /* ROUTE */
        trip.setGpsStartLocation(request.getGpsStartLocation());
        trip.setGpsEndLocation(request.getGpsEndLocation());
        trip.setRouteDetails(request.getRouteDetails());
        trip.setCheckpoints(request.getCheckpoints());

        /* AUDIT */
        LocalDateTime now = LocalDateTime.now();
        trip.setCreatedAt(now);
        trip.setUpdatedAt(now);
        trip.setCreatedBy(createdBy);
        trip.setUpdatedBy(createdBy);

        return trip;
    }
}
