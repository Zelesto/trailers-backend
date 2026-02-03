package com.pgsa.trailers.helpers;


import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TripSpecifications {

    public static Specification<Trip> filter(
            Long driverId,
            Long vehicleId,
            String status,
            String fromLocation,
            String toLocation,
            LocalDateTime startDateFrom,
            LocalDateTime startDateTo
    ) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if(driverId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("driver").get("id"), driverId));
            }
            if(vehicleId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("vehicle").get("id"), vehicleId));
            }
            if(status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if(fromLocation != null) {
                predicates = cb.and(predicates, cb.equal(root.get("originLocation"), fromLocation));
            }
            if(toLocation != null) {
                predicates = cb.and(predicates, cb.equal(root.get("destinationLocation"), toLocation));
            }
            if(startDateFrom != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
            }
            if(startDateTo != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("startDate"), startDateTo));
            }

            return predicates;
        };
    }
}
