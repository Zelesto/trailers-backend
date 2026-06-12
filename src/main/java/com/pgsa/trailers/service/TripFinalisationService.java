package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.exception.TripValidationException;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.PodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripFinalisationService {

    private final TripRepository tripRepository;
    private final PodRepository podRepository;
    private final TripMetricsService tripMetricsService;

    @Transactional
    public void finalizeTrip(Long tripId) {
        log.debug("Finalizing trip ID: {}", tripId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + tripId));

        if (TripStatus.FINALIZED.equals(trip.getStatus())) {
            throw new TripValidationException("Trip already FINALIZED");
        }

        if (!TripStatus.COMPLETED.equals(trip.getStatus())) {
            throw new TripValidationException(
                String.format("Cannot finalize trip with status: %s. Trip must be COMPLETED first.", trip.getStatus())
            );
        }

        TripMetrics metrics = trip.getMetrics();

        if (metrics == null) {
            log.warn("Trip metrics missing for trip {}, creating new metrics", tripId);
            tripMetricsService.initializeMetrics(tripId);
            metrics = trip.getMetrics();
        }

        long podCount = podRepository.countByTripId(tripId);
        log.debug("Trip {} has {} PODs", tripId, podCount);

        if (podCount == 0) {
            throw new TripValidationException(
                "Cannot finalize trip without PODs (Proof of Delivery documents)"
            );
        }

        // Calculate final metrics
        if (trip.getActualDistanceKm() != null && !metrics.isFinalized()) {
            metrics.setTotalDistanceKm(trip.getActualDistanceKm());
        }

        if (trip.getActualDurationHours() != null && !metrics.isFinalized()) {
            metrics.setTotalDurationHours(trip.getActualDurationHours());
        }

        // Calculate variances
        if (trip.getPlannedDistanceKm() != null && metrics.getTotalDistanceKm() != null) {
            BigDecimal variance = metrics.getTotalDistanceKm().subtract(trip.getPlannedDistanceKm());
            metrics.setPlannedVsActualDistanceVarianceKm(variance);
        }

        if (trip.getPlannedDurationHours() != null && metrics.getTotalDurationHours() != null) {
            BigDecimal variance = metrics.getTotalDurationHours().subtract(trip.getPlannedDurationHours());
            metrics.setPlannedVsActualDurationVarianceHours(variance);
        }

        metrics.setFinalized(true);
        metrics.setFinalizedAt(LocalDateTime.now());

        trip.setStatus(TripStatus.FINALIZED);
        trip.setLastStatusUpdate(LocalDateTime.now());

        tripRepository.save(trip);
        
        log.info("Successfully finalized trip ID: {}", tripId);
    }

    @Transactional
    public boolean canFinalize(Long tripId) {
        try {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new TripValidationException("Trip not found"));
            
            if (!TripStatus.COMPLETED.equals(trip.getStatus())) {
                return false;
            }
            
            long podCount = podRepository.countByTripId(tripId);
            return podCount > 0;
            
        } catch (Exception e) {
            log.error("Error checking if trip {} can be finalized: {}", tripId, e.getMessage());
            return false;
        }
    }
}
