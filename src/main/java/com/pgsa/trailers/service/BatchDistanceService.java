// src/main/java/com/pgsa/trailers/service/BatchDistanceService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
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

    // ============================================================
    // MAIN BATCH PROCESSING
    // ============================================================

    @Async("taskExecutor")
    public CompletableFuture<Void> recalculateAllTripDistancesAsync(String jobId) {
        log.info("🚀 Starting batch distance recalculation. Job ID: {}, Thread: {}", 
                 jobId, Thread.currentThread().getName());
        
        BatchProgress progress = new BatchProgress(jobId);
        progressMap.put(jobId, progress);

        try {
            // Step 1: Get all trips that need distance calculation
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

            // Step 2: Process trips in batches
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

            // Step 3: Update all loads with calculated distances
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

            // Step 4: Update vehicle mileage based on latest trips using updatedAt
            log.info("🚗 Updating vehicle mileage...");
            int vehicleUpdates = 0;
            Set<Long> processedVehicleIds = new HashSet<>();
            
            for (Trip trip : processedTrips) {
                Vehicle vehicle = trip.getVehicle();
                if (vehicle != null && vehicle.getId() != null) {
                    Long vehicleId = vehicle.getId();
                    if (!processedVehicleIds.contains(vehicleId)) {
                        try {
                            self.updateVehicleMileage(vehicleId);
                            processedVehicleIds.add(vehicleId);
                            vehicleUpdates++;
                        } catch (Exception e) {
                            log.error("❌ Failed to update vehicle {}: {}", vehicleId, e.getMessage());
                        }
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

    // ============================================================
    // SINGLE TRIP PROCESSING (Each in its own transaction)
    // ============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleTrip(Trip trip) {
        // Get origin and destination addresses
        String origin = getOriginAddress(trip);
        String destination = getDestinationAddress(trip);
        
        // If addresses are missing, try to build from components
        if ((origin == null || origin.isEmpty()) && trip.getOriginLocation() == null) {
            trip.updateOriginLocationFromComponents();
        }
        if ((destination == null || destination.isEmpty()) && trip.getDestinationLocation() == null) {
            trip.updateDestinationLocationFromComponents();
        }
        
        // Re-get addresses after potential update
        origin = getOriginAddress(trip);
        destination = getDestinationAddress(trip);

        if (origin == null || destination == null || origin.isEmpty() || destination.isEmpty()) {
            log.warn("⚠️ Missing address for Trip {}. Origin: {}, Destination: {}", 
                trip.getId(), origin, destination);
            trip.setCalculatedDistanceKm(BigDecimal.ZERO);
            trip.setActualDistanceKm(BigDecimal.ZERO);
            trip.setDistanceCalculated(false);
            trip.setDistanceCalculationError("Missing origin or destination address");
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            tripRepository.save(trip);
            return;
        }

        if (origin.equalsIgnoreCase(destination)) {
            log.warn("⚠️ Origin and destination are the same for Trip {}", trip.getId());
            trip.setCalculatedDistanceKm(BigDecimal.ZERO);
            trip.setActualDistanceKm(BigDecimal.ZERO);
            trip.setDistanceCalculated(true);
            trip.setDistanceCalculationError(null);
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            tripRepository.save(trip);
            return;
        }

        // Get vehicle type for routing
        String vehicleType = trip.getVehicle() != null ? trip.getVehicle().getVehicleType() : "TRUCK";
        
        // Calculate distance with retry
        BigDecimal distance = calculateTripDistanceWithRetry(origin, destination, vehicleType, trip.getId());
        
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
            trip.setDistanceCalculationError("Failed to calculate distance after " + MAX_RETRIES + " attempts");
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            log.warn("⚠️ Trip {} distance calculation failed", trip.getId());
        }
        
        tripRepository.save(trip);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTripError(Long tripId, String error) {
        tripRepository.findById(tripId).ifPresent(trip -> {
            trip.setDistanceCalculated(false);
            trip.setDistanceCalculationError(error);
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            tripRepository.save(trip);
        });
    }

    // ============================================================
    // SINGLE LOAD PROCESSING (Each in its own transaction)
    // ============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleLoad(String loadId) {
        Optional<Load> loadOpt = loadRepository.findByLoadNumber(loadId);
        if (loadOpt.isEmpty()) {
            log.warn("Load not found: {}", loadId);
            return;
        }
        
        Load load = loadOpt.get();
        
        // Get all trips for this load
        List<Trip> trips = tripRepository.findByLoadId(loadId);
        
        if (trips.isEmpty()) {
            log.warn("No trips found for load {}", loadId);
            load.setDistanceCalculated(true);
            load.setDistanceCalculatedAt(LocalDateTime.now());
            loadRepository.save(load);
            return;
        }
        
        // Calculate total distance from all trips that have been calculated
        BigDecimal totalTripDistance = trips.stream()
                .filter(t -> t.getDistanceCalculated() != null && t.getDistanceCalculated())
                .map(Trip::getCalculatedDistanceKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate distance from depot to first pickup (using isFromDepot flag)
        BigDecimal depotToPickupDistance = calculateDepotToPickupDistance(load, trips);
        
        // Total load distance = depot to pickup + all trip distances
        BigDecimal totalLoadDistance = depotToPickupDistance.add(totalTripDistance);
        
        // Calculate individual trip distances for the load
        BigDecimal tripCalculated = trips.stream()
                .filter(t -> t.getDistanceCalculated() != null && t.getDistanceCalculated())
                .map(Trip::getCalculatedDistanceKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal tripActual = trips.stream()
                .map(Trip::getActualDistanceKm)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Update load with all distance fields
        load.setTotalCalculatedDistanceKm(totalLoadDistance);
        load.setTotalActualDistanceKm(tripActual);
        load.setTotalCalculatedDistance(totalLoadDistance);
        load.setTotalActualDistance(tripActual);
        load.setTotalEstimatedDistance(tripCalculated);
        load.setDistanceCalculated(true);
        load.setDistanceCalculatedAt(LocalDateTime.now());
        
        // Also update total distance field if it exists
        if (load.getTotalDistanceKm() != null) {
            load.setTotalDistanceKm(totalLoadDistance.intValue());
        }
        
        loadRepository.save(load);
        log.info("✅ Load {} updated: total={} km, trips={} km, depotToPickup={} km", 
                 loadId, totalLoadDistance, totalTripDistance, depotToPickupDistance);
    }

    // ============================================================
    // DEPOT TO PICKUP DISTANCE CALCULATION
    // ============================================================

    private BigDecimal calculateDepotToPickupDistance(Load load, List<Trip> trips) {
        // Get depot location from load's origin
        String depotAddress = load.getOriginLocation();
        if (depotAddress == null || depotAddress.isEmpty()) {
            log.warn("No depot location (origin) for load {}", load.getId());
            return BigDecimal.ZERO;
        }
        
        // Find the first pickup trip - use isFromDepot = false OR find by earliest date
        // Sort trips by createdAt or plannedStartDate to get the first one
        Trip firstTrip = trips.stream()
                .filter(t -> t.getIsFromDepot() != null && !t.getIsFromDepot())
                .min(Comparator.comparing(Trip::getCreatedAt))
                .orElse(trips.stream()
                        .min(Comparator.comparing(Trip::getCreatedAt))
                        .orElse(trips.get(0)));
        
        String pickupAddress = getOriginAddress(firstTrip);
        if (pickupAddress == null || pickupAddress.isEmpty()) {
            log.warn("No pickup address for trip {}", firstTrip.getId());
            return BigDecimal.ZERO;
        }
        
        // If depot and pickup are the same, return 0
        if (depotAddress.equalsIgnoreCase(pickupAddress)) {
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

    // ============================================================
    // VEHICLE MILEAGE UPDATE - Using updatedAt for latest trip
    // ============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateVehicleMileage(Long vehicleId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            log.warn("Vehicle not found: {}", vehicleId);
            return;
        }
        
        Vehicle vehicle = vehicleOpt.get();
        
        // Find the latest completed or finalized trip for this vehicle using updatedAt
        Trip latestTrip = tripRepository.findTopByVehicleIdAndStatusInOrderByUpdatedAtDesc(
                vehicleId, 
                List.of("COMPLETED", "FINALIZED")
        );
        
        if (latestTrip != null && latestTrip.getActualDistanceKm() != null) {
            // Get current mileage from vehicle
            BigDecimal currentMileage = vehicle.getCurrentMileage() != null ? 
                    vehicle.getCurrentMileage() : BigDecimal.ZERO;
            
            // Calculate new total mileage
            BigDecimal newMileage = currentMileage.add(latestTrip.getActualDistanceKm());
            
            vehicle.setCurrentMileage(newMileage);
            vehicle.setCurrentOdometer(newMileage);
            vehicle.setLastFuelUpdate(LocalDateTime.now());
            
            vehicleRepository.save(vehicle);
            log.info("🚗 Vehicle {} mileage updated: {} -> {} km (trip {}, updated at {})", 
                     vehicleId, currentMileage, newMileage, latestTrip.getId(), latestTrip.getUpdatedAt());
        } else {
            log.debug("No completed trips found for vehicle {}", vehicleId);
        }
    }

    // ============================================================
    // DISTANCE CALCULATION WITH RETRY
    // ============================================================

    private BigDecimal calculateTripDistanceWithRetry(String origin, String destination, 
                                                       String vehicleType, Long tripId) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("🔄 Attempt {}/{} for Trip {}", attempt, MAX_RETRIES, tripId);
                
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
                log.warn("⚠️ Attempt {} failed for Trip {}: {}", attempt, tripId, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("❌ All attempts failed for Trip {}", tripId);
                }
            }
        }
        return null;
    }

    // ============================================================
    // ADDRESS HELPER METHODS
    // ============================================================

    private String getOriginAddress(Trip trip) {
        if (trip.getOriginLocation() != null && !trip.getOriginLocation().isEmpty()) {
            return trip.getOriginLocation();
        }
        return trip.buildOriginAddress();
    }

    private String getDestinationAddress(Trip trip) {
        if (trip.getDestinationLocation() != null && !trip.getDestinationLocation().isEmpty()) {
            return trip.getDestinationLocation();
        }
        return trip.buildDestinationAddress();
    }

    // ============================================================
    // PROGRESS TRACKING
    // ============================================================

    public BatchProgress getProgress(String jobId) {
        return progressMap.get(jobId);
    }

    public List<BatchProgress> getAllProgress() {
        return new ArrayList<>(progressMap.values());
    }
}
