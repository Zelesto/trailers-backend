package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.stereotype.Component;

@Component
public class CreateTripMapper {

    public Trip toEntity(CreateTripRequest request) {
        if (request == null) return null;

        Trip trip = new Trip();

        trip.setTripType(request.getTripType());
        trip.setOriginLocation(request.getOriginLocation());
        trip.setDestinationLocation(request.getDestinationLocation());
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());

        // vehicle, driver, load, status, tripNumber
        // are set in the SERVICE layer

        return trip;
    }
}

