// src/main/java/com/pgsa/trailers/service/BatchDistanceService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.service.routing.RoutingEngine;
import com.pgsa.trailers.service.routing.RoutingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchDistanceService {

    private final TripRepository tripRepository;
    private final RoutingEngine routingEngine;
    private final LoadService loadService;

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    // Track progress
    private final ConcurrentHashMap<String, BatchProgress> progressMap = new ConcurrentHashMap<>();

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<BatchResult> recalculateAllTripDistances() {
        String jobId = "batch-" + System.currentTimeMillis();
        BatchProgress progress = new BatchProgress(jobId);
        progressMap.put(jobId, progress);

        log.info("🚀 Starting batch distance recalculation for all trips. Job ID: {}", jobId);

        try {
            // Get all trips that need distance calculation (NULL or 0)
            List<Trip> trips = tripRepository.findByCalculatedDistanceKmIsNullOrZero();
            
            if (trips.isEmpty()) {
                log.info("✅ No trips need distance calculation");
                progress.setCompleted(true);
                progress.setTotalTrips(0);
                progress.setMessage("No trips need distance calculation");
                return CompletableFuture.completedFuture(new BatchResult(jobId, 0, 0, 0));
            }

            progress.setTotalTrips(trips.size());
            log.info("📊 Found {} trips needing distance calculation", trips.size());

            List<Trip> processedTrips = new ArrayList<>();
            AtomicInteger succeeded = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            // Process in batches
            for (int i = 0; i < trips.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, trips.size());
                List<Trip> batch = trips.subList(i, end);

                for (Trip trip : batch) {
                    try {
                        BigDecimal distance = calculateTripDistanceWithRetry(trip);
                        
                        if (distance != null && distance.compareTo(BigDecimal.ZERO) > 0) {
                            trip.setCalculatedDistanceKm(distance);
                            trip.setActualDistanceKm(distance);
                            trip.setDistanceCalculated(true);
                            trip.setDistanceCalculatedAt(LocalDateTime.now());
                            trip.setDistanceCalculationError(null);
                            succeeded.incrementAndGet();
                            log.info("✅ Trip {} distance calculated: {} km", trip.getId(), distance);
                        } else {
                            trip.setCalculatedDistanceKm(BigDecimal.ZERO);
                            trip.setActualDistanceKm(BigDecimal.ZERO);
                            trip.setDistanceCalculated(false);
                            trip.setDistanceCalculationError("Failed to calculate distance");
                            trip.setDistanceCalculatedAt(LocalDateTime.now());
                            failed.incrementAndGet();
                            log.warn("⚠️ Trip {} distance calculation failed", trip.getId());
                        }
                        
                        tripRepository.save(trip);
                        processedTrips.add(trip);
                        
                        // Update progress
                        progress.setProcessed(processedTrips.size());
                        progress.setSucceeded(succeeded.get());
                        progress.setFailed(failed.get());

                    } catch (Exception e) {
                        log.error("❌ Error processing trip {}: {}", trip.getId(), e.getMessage());
                        failed.incrementAndGet();
                        updateTripErrorStatus(trip.getId(), e.getMessage());
                    }
                }

                // Small delay between batches to avoid rate limiting
                if (i + BATCH_SIZE < trips.size()) {
                    Thread.sleep(500);
                }
            }

            // Update all loads after processing
            log.info("📦 Updating all load distances...");
            for (Trip trip : processedTrips) {
                if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
                    try {
                        loadService.updateLoadDistances(trip.getLoadId());
                    } catch (Exception e) {
                        log.error("❌ Failed to update load {}: {}", trip.getLoadId(), e.getMessage());
                    }
                }
            }

            progress.setCompleted(true);
            progress.setMessage(String.format("Completed: %d succeeded, %d failed out of %d trips",
                    succeeded.get(), failed.get(), trips.size()));

            log.info("✅ Batch distance recalculation complete. Job ID: {}. Succeeded: {}, Failed: {}",
                    jobId, succeeded.get(), failed.get());

            return CompletableFuture.completedFuture(new BatchResult(jobId, trips.size(), succeeded.get(), failed.get()));

        } catch (Exception e) {
            log.error("❌ Batch distance recalculation failed: {}", e.getMessage(), e);
            progress.setCompleted(true);
            progress.setMessage("Failed: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private BigDecimal calculateTripDistanceWithRetry(Trip trip) {
        String origin = getOriginAddress(trip);
        String destination = getDestinationAddress(trip);

        if (origin == null || destination == null || origin.isEmpty() || destination.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (origin.equalsIgnoreCase(destination)) {
            return BigDecimal.ZERO;
        }

        String vehicleType = trip.getVehicle() != null ? trip.getVehicle().getVehicleType() : "TRUCK";

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("🔄 Attempt {}/{} for Trip {}", attempt, MAX_RETRIES, trip.getId());
                
                RoutingResult result = routingEngine.calculateRoute(origin, destination, vehicleType);
                
                if (result != null && result.getDistanceKm() != null &&
                    result.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
                    return result.getDistanceKm();
                }
                
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                }
                
            } catch (Exception e) {
                log.warn("⚠️ Attempt {} failed for Trip {}: {}", attempt, trip.getId(), e.getMessage());
            }
        }
        return null;
    }

    private String getOriginAddress(Trip trip) {
        if (trip.getOriginLocation() != null && !trip.getOriginLocation().isEmpty()) {
            return trip.getOriginLocation();
        }
        StringBuilder sb = new StringBuilder();
        if (trip.getOriginStreetAddress() != null && !trip.getOriginStreetAddress().isEmpty()) {
            sb.append(trip.getOriginStreetAddress()).append(", ");
        }
        if (trip.getOriginCity() != null && !trip.getOriginCity().isEmpty()) {
            sb.append(trip.getOriginCity());
        }
        if (trip.getOriginProvince() != null && !trip.getOriginProvince().isEmpty()) {
            sb.append(", ").append(trip.getOriginProvince());
        }
        if (trip.getOriginZipCode() != null && !trip.getOriginZipCode().isEmpty()) {
            sb.append(" ").append(trip.getOriginZipCode());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String getDestinationAddress(Trip trip) {
        if (trip.getDestinationLocation() != null && !trip.getDestinationLocation().isEmpty()) {
            return trip.getDestinationLocation();
        }
        StringBuilder sb = new StringBuilder();
        if (trip.getDestinationStreetAddress() != null && !trip.getDestinationStreetAddress().isEmpty()) {
            sb.append(trip.getDestinationStreetAddress()).append(", ");
        }
        if (trip.getDestinationCity() != null && !trip.getDestinationCity().isEmpty()) {
            sb.append(trip.getDestinationCity());
        }
        if (trip.getDestinationProvince() != null && !trip.getDestinationProvince().isEmpty()) {
            sb.append(", ").append(trip.getDestinationProvince());
        }
        if (trip.getDestinationZipCode() != null && !trip.getDestinationZipCode().isEmpty()) {
            sb.append(" ").append(trip.getDestinationZipCode());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void updateTripErrorStatus(Long tripId, String error) {
        try {
            Trip trip = tripRepository.findById(tripId).orElse(null);
            if (trip != null) {
                trip.setDistanceCalculated(false);
                trip.setDistanceCalculationError(error);
                trip.setDistanceCalculatedAt(LocalDateTime.now());
                tripRepository.save(trip);
            }
        } catch (Exception e) {
            log.error("Failed to update trip error status: {}", e.getMessage());
        }
    }

    public BatchProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    public List<BatchProgress> getAllProgress() {
        return new ArrayList<>(progressMap.values());
    }

