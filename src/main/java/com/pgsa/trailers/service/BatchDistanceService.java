// src/main/java/com/pgsa/trailers/service/BatchDistanceService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Vehicle;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.routing.RoutingEngine;
import com.pgsa.trailers.service.routing.RoutingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchDistanceService {

    private final TripRepository tripRepository;
    private final LoadRepository loadRepository;
    private final VehicleRepository vehicleRepository;
    private final RoutingEngine routingEngine;
    private final LoadService loadService;

    @Lazy
    @Autowired
    private BatchDistanceService self;

    private static final int BATCH_SIZE = 10;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    private final ConcurrentHashMap<String, BatchProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * Start batch recalculation asynchronously
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> recalculateAllTripDistancesAsync(String jobId) {
        log.info("🚀 Starting batch distance recalculation. Job ID: {}, Thread: {}", 
                 jobId, Thread.currentThread().getName());
        
        BatchProgress progress = new BatchProgress(jobId);
        progressMap.put(jobId, progress);

        try {
            // Step 1: Process all trips without a transaction
            List<Trip> trips = tripRepository.findByCalculatedDistanceKmIsNullOrZero();
            
            if (trips.isEmpty()) {
                log.info("✅ No trips need distance calculation");
                progress.setCompleted(true);
                progress.setTotalTrips(0);
                progress.setMessage("No trips need distance calculation");
                return CompletableFuture.completedFuture(null);
            }

            progress.setTotalTrips(trips.size());
            log.info("📊 Found {} trips needing distance calculation", trips.size());

            List<Trip> processedTrips = new ArrayList<>();
            AtomicInteger succeeded = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            // Process trips in batches
            for (int i = 0; i < trips.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, trips.size());
                List<Trip> batch = trips.subList(i, end);

                for (Trip trip : batch) {
                    try {
                        // Process each trip in its own transaction
                        self.processSingleTrip(trip);
                        succeeded.incrementAndGet();
                        processedTrips.add(trip);
                        log.info("✅ Trip {} processed successfully", trip.getId());
                    } catch (Exception e) {
                        log.error("❌ Error processing trip {}: {}", trip.getId(), e.getMessage());
                        failed.incrementAndGet();
                        // Update error status in its own transaction
                        self.updateTripError(trip.getId(), e.getMessage());
                    }
                    
                    progress.setProcessed(processedTrips.size() + failed.get());
                    progress.setSucceeded(succeeded.get());
                    progress.setFailed(failed.get());
                }

                if (i + BATCH_SIZE < trips.size()) {
                    Thread.sleep(500);
                }
            }

            // Step 2: After all trips are processed, update ALL loads
            log.info("📦 Starting load updates...");
            int loadSuccess = 0;
            int loadFailures = 0;
            
            // Get all unique load IDs from processed trips
            Set<String> loadIds = new HashSet<>();
            for (Trip trip : processedTrips) {
                if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
                    loadIds.add(trip.getLoadId());
                }
            }
            
            log.info("📦 Found {} unique loads to update", loadIds.size());
            
            for (String loadId : loadIds) {
                try {
                    self.processSingleLoad(loadId);
                    loadSuccess++;
                    log.info("✅ Load {} updated successfully", loadId);
                } catch (Exception e) {
                    log.error("❌ Failed to update load {}: {}", loadId, e.getMessage());
                    loadFailures++;
                }
            }

            // Step 3: Update vehicle mileage
            log.info("🚗 Updating vehicle mileage...");
            int vehicleUpdates = 0;
            for (Trip trip : processedTrips) {
                if (trip.getVehicle() != null && trip.getVehicle().getId() != null) {
                    try {
                        self.updateVehicleMileage(trip.getVehicle().getId());
                        vehicleUpdates++;
                    } catch (Exception e) {
                        log.error("❌ Failed to update vehicle {}: {}", trip.getVehicle().getId(), e.getMessage());
                    }
                }
            }

            progress.setCompleted(true);
            progress.setMessage(String.format(
                "Completed: %d trips succeeded, %d failed | %d loads updated | %d vehicles updated",
                succeeded.get(), failed.get(), loadSuccess, vehicleUpdates
            ));

            log.info("✅ Batch complete. Job ID: {}. Trips: {}/{}, Loads: {}, Vehicles: {}", 
                     jobId, succeeded.get(), failed.get(), loadSuccess, vehicleUpdates);

        } catch (Exception e) {
            log.error("❌ Batch failed: {}", e.getMessage(), e);
            progress.setCompleted(true);
            progress.setMessage("Failed: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process a single trip - each in its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleTrip(Trip trip) {
        BigDecimal distance = calculateTripDistanceWithRetry(trip);
        
        if (distance != null && distance.compareTo(BigDecimal.ZERO) > 0) {
            trip.setCalculatedDistanceKm(distance);
            trip.setActualDistanceKm(distance);
            trip.setDistanceCalculated(true);
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            trip.setDistanceCalculationError(null);
            log.info("✅ Trip {} distance: {} km", trip.getId(), distance);
        } else {
            trip.setCalculatedDistanceKm(BigDecimal.ZERO);
            trip.setActualDistanceKm(BigDecimal.ZERO);
            trip.setDistanceCalculated(false);
            trip.setDistanceCalculationError("Failed to calculate distance");
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            log.warn("⚠️ Trip {} distance calculation failed", trip.getId());
        }
        
        tripRepository.save(trip);
    }

    /**
     * Update trip error status - each in its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTripError(Long tripId, String error) {
        tripRepository.findById(tripId).ifPresent(trip -> {
            trip.setDistanceCalculated(false);
            trip.setDistanceCalculationError(error);
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            tripRepository.save(trip);
        });
    }

    /**
     * Process a single load - each in its own transaction
     * Updates load with trip distances and calculates depot to first pickup
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleLoad(String loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new RuntimeException("Load not found: " + loadId));
        
        // Get all trips for this load
        List<Trip> trips = tripRepository.findByLoadId(loadId);
        
        if (trips.isEmpty()) {
            log.warn("No trips found for load {}", loadId);
            return;
        }
        
        // Calculate total distance from all trips
        BigDecimal totalTripDistance = trips.stream()
                .filter(Trip::isDistanceCalculated)
                .map(Trip::getCalculatedDistanceKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate distance from depot to first pickup
        BigDecimal depotToPickupDistance = calculateDepotToPickupDistance(load, trips);
        
        // Total load distance = depot to pickup + all trip distances
        BigDecimal totalLoadDistance = depotToPickupDistance.add(totalTripDistance);
        
        // Update load
        load.setTotalDistanceKm(totalLoadDistance);
        load.setTripDistanceKm(totalTripDistance);
        load.setDepotToPickupDistanceKm(depotToPickupDistance);
        load.setDistanceCalculated(true);
        load.setDistanceCalculatedAt(LocalDateTime.now());
        
        loadRepository.save(load);
        log.info("✅ Load {} updated: total={} km, trips={} km, depotToPickup={} km", 
                 loadId, totalLoadDistance, totalTripDistance, depotToPickupDistance);
    }

    /**
     * Calculate distance from depot to first pickup point
     */
    private BigDecimal calculateDepotToPickupDistance(Load load, List<Trip> trips) {
        // Get depot location
        String depotAddress = load.getDepotLocation() != null ? load.getDepotLocation() : null;
        if (depotAddress == null || depotAddress.isEmpty()) {
            log.warn("No depot location for load {}", load.getId());
            return BigDecimal.ZERO;
        }
        
        // Find the first pickup trip
        Trip firstTrip = trips.stream()
                .filter(t -> t.getStopSequence() != null && t.getStopSequence() == 1)
                .findFirst()
                .orElse(trips.get(0)); // Fallback to first trip
        
        String pickupAddress = getOriginAddress(firstTrip);
        if (pickupAddress == null || pickupAddress.isEmpty()) {
            log.warn("No pickup address for trip {}", firstTrip.getId());
            return BigDecimal.ZERO;
        }
        
        // Calculate route
        try {
            String vehicleType = firstTrip.getVehicle() != null ? 
                    firstTrip.getVehicle().getVehicleType() : "TRUCK";
                    
            RoutingResult result = routingEngine.calculateRoute(depotAddress, pickupAddress, vehicleType);
            if (result != null && result.getDistanceKm() != null) {
                return result.getDistanceKm();
            }
        } catch (Exception e) {
            log.error("Failed to calculate depot to pickup distance: {}", e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Update vehicle mileage based on latest completed trip
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateVehicleMileage(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
        
        // Find the latest completed trip for this vehicle
        Trip latestTrip = tripRepository.findTopByVehicleIdAndStatusOrderByEndDateDesc(
                vehicleId, "COMPLETED");
        
        if (latestTrip != null && latestTrip.getActualDistanceKm() != null) {
            // Calculate new total mileage
            BigDecimal currentMileage = vehicle.getMileage() != null ? vehicle.getMileage() : BigDecimal.ZERO;
            BigDecimal newMileage = currentMileage.add(latestTrip.getActualDistanceKm());
            
            vehicle.setMileage(newMileage);
            vehicle.setLastMileageUpdate(LocalDateTime.now());
            vehicle.setLastTripId(latestTrip.getId());
            
            vehicleRepository.save(vehicle);
            log.info("🚗 Vehicle {} mileage updated: {} -> {} km (trip {})", 
                     vehicleId, currentMileage, newMileage, latestTrip.getId());
        } else {
            log.debug("No completed trips found for vehicle {}", vehicleId);
        }
    }

    /**
     * Calculate distance for a single trip with retry logic
     */
    private BigDecimal calculateTripDistanceWithRetry(Trip trip) {
        String origin = getOriginAddress(trip);
        String destination = getDestinationAddress(trip);

        if (origin == null || destination == null || origin.isEmpty() || destination.isEmpty()) {
            log.warn("⚠️ Missing address for Trip {}. Origin: {}, Destination: {}", 
                trip.getId(), origin, destination);
            return BigDecimal.ZERO;
        }

        if (origin.equalsIgnoreCase(destination)) {
            log.warn("⚠️ Origin and destination are the same for Trip {}", trip.getId());
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
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("⚠️ Attempt {} failed for Trip {}: {}", attempt, trip.getId(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("❌ All attempts failed for Trip {}", trip.getId());
                }
            }
        }
        return null;
    }

    /**
     * Get origin address from trip (handles nulls)
     */
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

    /**
     * Get destination address from trip (handles nulls)
     */
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

    // Progress tracking methods
    public BatchProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    public List<BatchProgress> getAllProgress() {
        return new ArrayList<>(progressMap.values());
    }
}
