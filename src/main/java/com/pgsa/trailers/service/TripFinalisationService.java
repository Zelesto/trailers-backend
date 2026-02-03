package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.*;
import com.pgsa.trailers.enums.*;
import com.pgsa.trailers.entity.ops.TripMetrics;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.PodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripFinalisationService {

    private final TripRepository tripRepo;
    private final PodRepository podRepo;

    public TripFinalisationService(
            TripRepository tripRepo,
            PodRepository podRepo
    ) {
        this.tripRepo = tripRepo;
        this.podRepo = podRepo;
    }

    @Transactional
    public void finalizeTrip(Long tripId) {

        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new IllegalStateException("Trip not found"));

        if (TripStatus.FINALIZED.equals(trip.getStatus())) {
            throw new IllegalStateException("Trip already FINALIZED");
        }

        TripMetrics metrics = trip.getMetrics();

        if (metrics == null) {
            throw new IllegalStateException("Trip metrics missing");
        }

        long podCount = podRepo.countByTripId(tripId);

        if (podCount == 0) {
            throw new IllegalStateException("Trip has no PODs");
        }

        trip.setStatus(TripStatus.FINALIZED);
        tripRepo.save(trip);
    }

}
