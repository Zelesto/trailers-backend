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
        log.info("🔍 STARTING finalization for trip ID: {}", tripId);

        try {
            // 1. Find the trip
            log.debug("Step 1: Finding trip...");
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + tripId));
            log.info("✅ Found trip: ID={}, Number={}, Status={}", trip.getId(), trip.getTripNumber(), trip.getStatus());

            // 2. Check if already finalized
            log.debug("Step 2: Checking if already finalized...");
            if (TripStatus.FINALIZED.equals(trip.getStatus())) {
                throw new TripValidationException("Trip already FINALIZED");
            }

            // 3. Check if trip is COMPLETED
            log.debug("Step 3: Checking if trip is COMPLETED...");
            if (!TripStatus.COMPLETED.equals(trip.getStatus())) {
                throw new TripValidationException(
                    String.format("Cannot finalize trip with status: %s. Trip must be COMPLETED first.", trip.getStatus())
                );
            }
            log.info("✅ Trip status is COMPLETED");

            // 4. Handle missing location data
            log.debug("Step 4: Checking location data...");
            if (trip.getOriginLocation() == null || trip.getOriginLocation().isEmpty()) {
                log.warn("⚠️ Trip {} has no origin location, using placeholder", tripId);
                trip.setOriginLocation("Unknown Origin");
            }
            if (trip.getDestinationLocation() == null || trip.getDestinationLocation().isEmpty()) {
                log.warn("⚠️ Trip {} has no destination location, using placeholder", tripId);
                trip.setDestinationLocation("Unknown Destination");
            }

            // 5. Handle metrics
            log.debug("Step 5: Checking metrics...");
            TripMetrics metrics = trip.getMetrics();
            if (metrics == null) {
                log.warn("⚠️ Trip metrics missing for trip {}, creating new metrics", tripId);
                try {
                    metrics = tripMetricsService.initializeMetrics(tripId);
                    log.info("✅ Metrics created for trip {}", tripId);
                } catch (Exception e) {
                    log.error("❌ Failed to create metrics for trip {}: {}", tripId, e.getMessage(), e);
                    throw new TripValidationException("Failed to create metrics: " + e.getMessage());
                }
            } else {
                log.info("✅ Metrics found for trip {}", tripId);
            }

            // 6. Check PODs
            log.debug("Step 6: Checking PODs...");
            long podCount = 0;
            try {
                podCount = podRepository.countByTripId(tripId);
                log.info("📦 Trip {} has {} PODs", tripId, podCount);
            } catch (Exception e) {
                log.error("❌ Error counting PODs for trip {}: {}", tripId, e.getMessage(), e);
                // Continue with finalization - don't block
                log.warn("⚠️ Proceeding with finalization despite POD count error");
            }

            // 7. POD validation - optional
            if (podCount == 0) {
                log.warn("⚠️ Trip {} has no PODs. Proceeding with finalization (POD check is optional)", tripId);
            }

            // 8. Calculate final metrics
            log.debug("Step 7: Calculating final metrics...");
            boolean isFinalized = metrics.isFinalized();
            log.info("Metrics finalized status: {}", isFinalized);

            if (trip.getActualDistanceKm() != null && !isFinalized) {
                metrics.setTotalDistanceKm(trip.getActualDistanceKm());
                log.info("✅ Set total distance: {} km", trip.getActualDistanceKm());
            } else if (trip.getActualDistanceKm() == null) {
                log.warn("⚠️ Trip has no actual distance, skipping");
            }

            if (trip.getActualDurationHours() != null && !isFinalized) {
                metrics.setTotalDurationHours(trip.getActualDurationHours());
                log.info("✅ Set total duration: {} hours", trip.getActualDurationHours());
            } else if (trip.getActualDurationHours() == null) {
                log.warn("⚠️ Trip has no actual duration, skipping");
            }

            // 9. Calculate variances
            log.debug("Step 8: Calculating variances...");
            if (trip.getPlannedDistanceKm() != null && metrics.getTotalDistanceKm() != null) {
                BigDecimal variance = metrics.getTotalDistanceKm().subtract(trip.getPlannedDistanceKm());
                metrics.setPlannedVsActualDistanceVarianceKm(variance);
                log.info("✅ Distance variance: {} km", variance);
            }

            if (trip.getPlannedDurationHours() != null && metrics.getTotalDurationHours() != null) {
                BigDecimal variance = metrics.getTotalDurationHours().subtract(trip.getPlannedDurationHours());
                metrics.setPlannedVsActualDurationVarianceHours(variance);
                log.info("✅ Duration variance: {} hours", variance);
            }

            // 10. Mark as finalized
            log.debug("Step 9: Marking as finalized...");
            metrics.setFinalized(true);
            metrics.setFinalizedAt(LocalDateTime.now());

            trip.setStatus(TripStatus.FINALIZED);
            trip.setLastStatusUpdate(LocalDateTime.now());

            // 11. Save
            log.debug("Step 10: Saving trip...");
            tripRepository.save(trip);
            
            log.info("✅ SUCCESSFULLY finalized trip ID: {} - {}", tripId, trip.getTripNumber());
            
        } catch (TripValidationException e) {
            log.error("❌ Validation error finalizing trip {}: {}", tripId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error finalizing trip {}: {}", tripId, e.getMessage(), e);
            throw new TripValidationException("Failed to finalize trip: " + e.getMessage());
        }
    }

    @Transactional
    public boolean canFinalize(Long tripId) {
        try {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new TripValidationException("Trip not found"));
            
            if (!TripStatus.COMPLETED.equals(trip.getStatus())) {
                log.debug("Trip {} cannot be finalized - status is {}", tripId, trip.getStatus());
                return false;
            }
            
            long podCount = podRepository.countByTripId(tripId);
            boolean hasPods = podCount > 0;
            
            if (!hasPods) {
                log.debug("Trip {} has no PODs", tripId);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error checking if trip {} can be finalized: {}", tripId, e.getMessage());
            return false;
        }
    }
}
