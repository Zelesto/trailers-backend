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

        // Identity
        trip.setTripType(request.getTripType());

        // Required DB fields
        trip.setOriginLocation(request.getOriginLocation());
        trip.setDestinationLocation(request.getDestinationLocation());

        // Planning
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());

        return trip;
    }
}
