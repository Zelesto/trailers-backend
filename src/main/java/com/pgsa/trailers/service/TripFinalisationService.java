package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.entity.suppliers.TripValidationException;
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
        log.info("🔍 Starting finalization for trip ID: {}", tripId);

        // 1. Find the trip
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + tripId));
        log.debug("✅ Found trip: {} with status: {}", trip.getTripNumber(), trip.getStatus());

        // 2. Check if already finalized
        if (TripStatus.FINALIZED.equals(trip.getStatus())) {
            throw new TripValidationException("Trip already FINALIZED");
        }

        // 3. Check if trip is COMPLETED
        if (!TripStatus.COMPLETED.equals(trip.getStatus())) {
            throw new TripValidationException(
                String.format("Cannot finalize trip with status: %s. Trip must be COMPLETED first.", trip.getStatus())
            );
        }

        // 4. Handle missing location data
        if (trip.getOriginLocation() == null || trip.getOriginLocation().isEmpty()) {
            log.warn("⚠️ Trip {} has no origin location, using placeholder", tripId);
            trip.setOriginLocation("Unknown Origin");
        }
        if (trip.getDestinationLocation() == null || trip.getDestinationLocation().isEmpty()) {
            log.warn("⚠️ Trip {} has no destination location, using placeholder", tripId);
            trip.setDestinationLocation("Unknown Destination");
        }

        // 5. Handle metrics
        TripMetrics metrics = trip.getMetrics();
        if (metrics == null) {
            log.warn("⚠️ Trip metrics missing for trip {}, creating new metrics", tripId);
            metrics = tripMetricsService.initializeMetrics(tripId);
            // Re-fetch trip to get updated metrics
            trip = tripRepository.findById(tripId).orElseThrow();
            metrics = trip.getMetrics();
            log.info("✅ Metrics created for trip {}", tripId);
        }

        // 6. Check PODs
        long podCount = 0;
        try {
            podCount = podRepository.countByTripId(tripId);
            log.debug("📦 Trip {} has {} PODs", tripId, podCount);
        } catch (Exception e) {
            log.error("❌ Error counting PODs for trip {}: {}", tripId, e.getMessage());
            // Continue with finalization - don't block
        }

        // 7. POD validation - now optional (warns but doesn't block)
        if (podCount == 0) {
            log.warn("⚠️ Trip {} has no PODs. Proceeding with finalization (POD check is optional)", tripId);
            // Uncomment the line below if you want to enforce POD requirement
            // throw new TripValidationException("Cannot finalize trip without PODs (Proof of Delivery documents)");
        }

        // 8. Calculate final metrics
        boolean isFinalized = metrics.isFinalized();
        log.debug("Metrics finalized status: {}", isFinalized);

        if (trip.getActualDistanceKm() != null && !isFinalized) {
            metrics.setTotalDistanceKm(trip.getActualDistanceKm());
            log.debug("✅ Set total distance: {} km", trip.getActualDistanceKm());
        }

        if (trip.getActualDurationHours() != null && !isFinalized) {
            metrics.setTotalDurationHours(trip.getActualDurationHours());
            log.debug("✅ Set total duration: {} hours", trip.getActualDurationHours());
        }

        // 9. Calculate variances
        if (trip.getPlannedDistanceKm() != null && metrics.getTotalDistanceKm() != null) {
            BigDecimal variance = metrics.getTotalDistanceKm().subtract(trip.getPlannedDistanceKm());
            metrics.setPlannedVsActualDistanceVarianceKm(variance);
            log.debug("✅ Distance variance: {} km", variance);
        }

        if (trip.getPlannedDurationHours() != null && metrics.getTotalDurationHours() != null) {
            BigDecimal variance = metrics.getTotalDurationHours().subtract(trip.getPlannedDurationHours());
            metrics.setPlannedVsActualDurationVarianceHours(variance);
            log.debug("✅ Duration variance: {} hours", variance);
        }

        // 10. Mark as finalized
        metrics.setFinalized(true);
        metrics.setFinalizedAt(LocalDateTime.now());

        trip.setStatus(TripStatus.FINALIZED);
        trip.setLastStatusUpdate(LocalDateTime.now());

        // 11. Save
        tripRepository.save(trip);
        
        log.info("✅ Successfully finalized trip ID: {} - {}", tripId, trip.getTripNumber());
    }

    @Transactional
    public boolean canFinalize(Long tripId) {
        try {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new TripValidationException("Trip not found"));
            
            // Check if trip is COMPLETED
            if (!TripStatus.COMPLETED.equals(trip.getStatus())) {
                log.debug("Trip {} cannot be finalized - status is {}", tripId, trip.getStatus());
                return false;
            }
            
            // Check for PODs (optional)
            long podCount = podRepository.countByTripId(tripId);
            boolean hasPods = podCount > 0;
            
            if (!hasPods) {
                log.debug("Trip {} has no PODs", tripId);
            }
            
            // Allow finalization even without PODs (for now)
            return true;
            
        } catch (Exception e) {
            log.error("Error checking if trip {} can be finalized: {}", tripId, e.getMessage());
            return false;
        }
    }
}
