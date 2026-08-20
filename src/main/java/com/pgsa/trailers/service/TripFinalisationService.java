package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import com.pgsa.trailers.entity.suppliers.TripValidationException;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.PodRepository;
import com.pgsa.trailers.repository.LoadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripFinalisationService {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FINALIZED = "FINALIZED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_ON_HOLD = "ON_HOLD";

    public static final String LOAD_STATUS_PENDING = "PENDING";
    public static final String LOAD_STATUS_PLANNED = "PLANNED";
    public static final String LOAD_STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String LOAD_STATUS_DELIVERED = "DELIVERED";
    public static final String LOAD_STATUS_COMPLETED = "COMPLETED";
    public static final String LOAD_STATUS_CANCELLED = "CANCELLED";

    private final TripRepository tripRepository;
    private final PodRepository podRepository;
    private final LoadRepository loadRepository;
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
            if (STATUS_FINALIZED.equals(trip.getStatus())) {
                throw new TripValidationException("Trip already FINALIZED");
            }

            // 3. Check if trip is COMPLETED
            log.debug("Step 3: Checking if trip is COMPLETED...");
            if (!STATUS_COMPLETED.equals(trip.getStatus())) {
                throw new TripValidationException(
                    String.format("Cannot finalize trip with status: %s. Trip must be COMPLETED first.", trip.getStatus())
                );
            }
            log.info("✅ Trip status is COMPLETED");

            // 4. CRITICAL: Check if trip has at least one POD
            log.debug("Step 4: Checking PODs...");
            long podCount = podRepository.countByTripId(tripId);
            log.info("📦 Trip {} has {} PODs", tripId, podCount);
            
            if (podCount == 0) {
                log.error("❌ Cannot finalize trip {}: No PODs found", tripId);
                throw new TripValidationException(
                    "Cannot finalize trip. The trip must have at least one POD document. " +
                    "Please upload a POD before finalizing."
                );
            }
            log.info("✅ Trip has {} POD(s)", podCount);

            // 5. Check if all PODs are in valid status (DELIVERED or VERIFIED)
            log.debug("Step 5: Checking POD statuses...");
            long validPods = podRepository.countByTripIdAndStatusIn(
                tripId, List.of("DELIVERED", "VERIFIED")
            );
            
            if (validPods < podCount) {
                long invalidPods = podCount - validPods;
                log.warn("⚠️ {} POD(s) are not in DELIVERED or VERIFIED status", invalidPods);
                throw new TripValidationException(
                    String.format(
                        "Cannot finalize trip. %d out of %d POD(s) are not in DELIVERED or VERIFIED status. " +
                        "Please ensure all PODs are properly debriefed.",
                        invalidPods, podCount
                    )
                );
            }
            log.info("✅ All {} PODs are in valid status (DELIVERED/VERIFIED)", podCount);

            // 6. Handle missing location data
            log.debug("Step 6: Checking location data...");
            if (trip.getOriginLocation() == null || trip.getOriginLocation().isEmpty()) {
                log.warn("⚠️ Trip {} has no origin location, using placeholder", tripId);
                trip.setOriginLocation("Unknown Origin");
            }
            if (trip.getDestinationLocation() == null || trip.getDestinationLocation().isEmpty()) {
                log.warn("⚠️ Trip {} has no destination location, using placeholder", tripId);
                trip.setDestinationLocation("Unknown Destination");
            }

            // 7. Handle metrics
            log.debug("Step 7: Checking metrics...");
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

            // 8. Calculate final metrics
            log.debug("Step 8: Calculating final metrics...");
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
            log.debug("Step 9: Calculating variances...");
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
            log.debug("Step 10: Marking as finalized...");
            metrics.setFinalized(true);
            metrics.setFinalizedAt(LocalDateTime.now());

            trip.setStatus(STATUS_FINALIZED);
            trip.setLastStatusUpdate(LocalDateTime.now());

            // 11. Update Load - mark trip as completed in load
            log.debug("Step 11: Updating Load...");
            try {
                if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
                    try {
                        Long loadIdLong = null;
                        try {
                            loadIdLong = Long.parseLong(trip.getLoadId());
                        } catch (NumberFormatException e) {
                            log.debug("LoadId is a string: {}", trip.getLoadId());
                        }
                        
                        Load load = null;
                        if (loadIdLong != null) {
                            load = loadRepository.findById(loadIdLong).orElse(null);
                        }
                        if (load == null) {
                            load = loadRepository.findByLoadNumber(trip.getLoadId()).orElse(null);
                        }
                        
                        if (load != null) {
                            load.setTripsCount(load.getTrips() != null ? load.getTrips().size() : 0);
                            
                            boolean allCompleted = true;
                            if (load.getTrips() != null) {
                                for (Trip t : load.getTrips()) {
                                    if (!STATUS_COMPLETED.equals(t.getStatus()) && !STATUS_FINALIZED.equals(t.getStatus())) {
                                        allCompleted = false;
                                        break;
                                    }
                                }
                            }
                            
                            // ✅ FIXED: Use String constant instead of LoadStatus enum
                            if (allCompleted && load.getTripsCount() > 0) {
                                load.setStatus(LOAD_STATUS_COMPLETED);
                                log.info("✅ All trips in load {} are completed", load.getLoadNumber());
                            }
                            
                            load.setLastStatusUpdate(LocalDateTime.now());
                            loadRepository.save(load);
                            log.info("✅ Load {} updated successfully", load.getLoadNumber());
                        } else {
                            log.warn("⚠️ Load not found for trip {}: loadId={}", tripId, trip.getLoadId());
                        }
                    } catch (Exception e) {
                        log.error("❌ Error updating load for trip {}: {}", tripId, e.getMessage());
                    }
                } else {
                    log.warn("⚠️ Trip {} has no load assigned", tripId);
                }
            } catch (Exception e) {
                log.error("❌ Error in load update: {}", e.getMessage());
            }

            // 12. Save trip
            log.debug("Step 12: Saving trip...");
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

    @Transactional(readOnly = true)
    public boolean canFinalize(Long tripId) {
        try {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new TripValidationException("Trip not found"));
            
            // Check if trip is COMPLETED
            if (!STATUS_COMPLETED.equals(trip.getStatus())) {
                log.debug("Trip {} cannot be finalized - status is {}", tripId, trip.getStatus());
                return false;
            }
            
            // Check if trip has at least one POD
            long podCount = podRepository.countByTripId(tripId);
            if (podCount == 0) {
                log.debug("Trip {} has no PODs - cannot finalize", tripId);
                return false;
            }
            
            // Check if all PODs are in valid status
            long validPods = podRepository.countByTripIdAndStatusIn(
                tripId, List.of("DELIVERED", "VERIFIED")
            );
            
            if (validPods < podCount) {
                log.debug("Trip {} has {} invalid POD(s)", tripId, podCount - validPods);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error checking if trip {} can be finalized: {}", tripId, e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public TripFinalizationStatus getFinalizationStatus(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + tripId));
        
        long podCount = podRepository.countByTripId(tripId);
        boolean hasPods = podCount > 0;
        
        long validPods = podRepository.countByTripIdAndStatusIn(
            tripId, List.of("DELIVERED", "VERIFIED")
        );
        long invalidPods = podCount - validPods;
        
        boolean canFinalize = hasPods && invalidPods == 0 && STATUS_COMPLETED.equals(trip.getStatus());
        
        return TripFinalizationStatus.builder()
            .tripId(tripId)
            .tripNumber(trip.getTripNumber())
            .currentStatus(trip.getStatus())  // Now a String
            .canBeFinalized(canFinalize)
            .hasPods(hasPods)
            .podCount(podCount)
            .invalidPods(invalidPods)
            .message(getFinalizationMessage(hasPods, invalidPods, trip.getStatus()))
            .build();
    }

    private String getFinalizationMessage(boolean hasPods, long invalidPods, String status) {
        if (!STATUS_COMPLETED.equals(status)) {
            return String.format("Cannot finalize: Trip is %s. Must be COMPLETED first.", status);
        }
        if (!hasPods) {
            return "Cannot finalize: No POD documents uploaded. Please upload at least one POD.";
        }
        if (invalidPods > 0) {
            return String.format("Cannot finalize: %d POD(s) are not in DELIVERED or VERIFIED status.", invalidPods);
        }
        return "Trip is ready for finalization.";
    }
}
