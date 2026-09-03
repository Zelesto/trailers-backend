// src/main/java/com/pgsa/trailers/service/TripService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.dto.TripResponse;
import com.pgsa.trailers.dto.UpdateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.CreateTripMapper;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.ops.auto.TripCompletedEvent;
import com.pgsa.trailers.entity.ops.auto.TripPlannedEvent;
import com.pgsa.trailers.entity.ops.auto.TripStartedEvent;
import com.pgsa.trailers.entity.suppliers.TripValidationException;
import com.pgsa.trailers.entity.billing.TripBilling;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.billing.BillingCalculatorService;
import com.pgsa.trailers.service.routing.RoutingEngine;
import com.pgsa.trailers.service.routing.RoutingResult;
import com.pgsa.trailers.service.util.LoadNumberGenerator;
import com.pgsa.trailers.service.util.TripNumberGenerator;
import com.pgsa.trailers.service.util.TripValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation; 

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripService {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    // ============================================================
    // DEPENDENCIES
    // ============================================================
    private final TripRepository tripRepository;
    private final TripMetricsService tripMetricsService;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final LoadRepository loadRepository;
    private final TripNumberGenerator tripNumberGenerator;
    private final LoadNumberGenerator loadNumberGenerator;
    private final CreateTripMapper createTripMapper;
    private final TripResponseMapper tripResponseMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TripValidator tripValidator;
    private final JdbcTemplate jdbcTemplate;
    private final RoutingEngine routingEngine;
    private final LoadService loadService;
    private final BillingCalculatorService billingCalculatorService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String LOAD_REF_SEQUENCE_NAME = "loadref";
    private static final String TRIP_SEQUENCE_NAME = "trip";

    // ============================================================
    // STATUS CONSTANTS
    // ============================================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_ON_HOLD = "ON_HOLD";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FINALIZED = "FINALIZED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DRAFT = "DRAFT";

    public static final String LOAD_STATUS_PENDING = "PENDING";
    public static final String LOAD_STATUS_PLANNED = "PLANNED";
    public static final String LOAD_STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String LOAD_STATUS_DELIVERED = "DELIVERED";
    public static final String LOAD_STATUS_COMPLETED = "COMPLETED";
    public static final String LOAD_STATUS_CANCELLED = "CANCELLED";

    // ============================================================
    // DISTANCE CALCULATION METHODS
    // ============================================================

@Async("taskExecutor")
@Transactional
public CompletableFuture<Trip> calculateTripDistance(Long tripId) {
    log.info("🚗 Calculating distance for Trip ID: {}", tripId);

    try {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        // Skip if already calculated
        if (Boolean.TRUE.equals(trip.getDistanceCalculated()) && trip.getCalculatedDistanceKm() != null) {
            log.info("⏭️ Trip {} already has calculated distance: {} km", tripId, trip.getCalculatedDistanceKm());
            return CompletableFuture.completedFuture(trip);
        }

        // Get origin and destination
        String origin = getOriginAddress(trip);
        String destination = getDestinationAddress(trip);

        if (origin == null || destination == null || origin.isEmpty() || destination.isEmpty()) {
            log.warn("⚠️ Missing address for Trip {}. Origin: {}, Destination: {}", tripId, origin, destination);
            trip.setDistanceCalculated(false);
            trip.setDistanceCalculationError("Missing origin or destination address");
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            tripRepository.save(trip);
            return CompletableFuture.completedFuture(trip);
        }

        if (origin.equalsIgnoreCase(destination)) {
            log.warn("⚠️ Origin and destination are the same for Trip {}", tripId);
            trip.setCalculatedDistanceKm(BigDecimal.ZERO);
            trip.setActualDistanceKm(BigDecimal.ZERO);
            trip.setDistanceCalculated(true);
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            trip.setDistanceCalculationError(null);
            tripRepository.save(trip);
            // ✅ Also update load
            if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
                loadService.updateLoadDistances(trip.getLoadId());
            }
            return CompletableFuture.completedFuture(trip);
        }

        // Calculate with retry
        String vehicleType = trip.getVehicle() != null ? trip.getVehicle().getVehicleType() : "TRUCK";
        BigDecimal distance = calculateWithRetry(origin, destination, vehicleType, tripId);

        if (distance != null && distance.compareTo(BigDecimal.ZERO) > 0) {
            trip.setCalculatedDistanceKm(distance);
            trip.setActualDistanceKm(distance);
            trip.setDistanceCalculated(true);
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            trip.setDistanceCalculationError(null);
            log.info("✅ Trip {} distance calculated: {} km", tripId, distance);
        } else {
            trip.setDistanceCalculated(false);
            trip.setDistanceCalculationError("Failed to calculate distance - API returned null or zero");
            trip.setDistanceCalculatedAt(LocalDateTime.now());
            log.warn("⚠️ Trip {} distance calculation failed", tripId);
        }

        tripRepository.save(trip);

        // ✅ ALWAYS update load if trip is associated with one
        if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
            try {
                log.info("📦 Updating load {} for trip {}", trip.getLoadId(), tripId);
                loadService.updateLoadDistances(trip.getLoadId());
                log.info("✅ Load {} updated successfully", trip.getLoadId());
            } catch (Exception e) {
                log.error("❌ Failed to update load {}: {}", trip.getLoadId(), e.getMessage(), e);
            }
        }

        return CompletableFuture.completedFuture(trip);

    } catch (Exception e) {
        log.error("❌ Error calculating distance for Trip {}: {}", tripId, e.getMessage(), e);
        updateTripErrorStatus(tripId, e.getMessage());
        return CompletableFuture.failedFuture(e);
    }
}
   

    @Transactional
    public void processPendingDistanceCalculations() {
        log.info("🔄 Starting batch distance calculation for pending trips...");

        try {
            List<Trip> pendingTrips = tripRepository.findByDistanceCalculatedFalseOrDistanceCalculatedIsNull();

            if (pendingTrips.isEmpty()) {
                log.info("✅ No pending distance calculations");
                return;
            }

            log.info("📊 Found {} trips needing distance calculation", pendingTrips.size());

            int processed = 0;
            int failed = 0;

            for (Trip trip : pendingTrips) {
                try {
                    if (hasValidAddresses(trip)) {
                        calculateTripDistance(trip.getId());
                        processed++;
                    } else {
                        trip.setDistanceCalculationError("Incomplete address data");
                        trip.setDistanceCalculatedAt(LocalDateTime.now());
                        tripRepository.save(trip);
                        failed++;
                    }

                    if (processed % 10 == 0) {
                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    log.error("Failed to process trip {}: {}", trip.getId(), e.getMessage());
                    failed++;
                }
            }

            log.info("✅ Batch processing complete. Processed: {}, Failed: {}", processed, failed);

        } catch (Exception e) {
            log.error("❌ Error in batch distance calculation: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public long getPendingDistanceCount() {
        return tripRepository.countPendingDistanceCalculations();
    }

    // ============================================================
    // HELPER METHODS FOR DISTANCE CALCULATION - FIXED
    // ============================================================
    
    private String getOriginAddress(Trip trip) {
        // Use originLocation directly
        if (trip.getOriginLocation() != null && !trip.getOriginLocation().isEmpty()) {
            return trip.getOriginLocation();
        }
        // Build from components as fallback
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
        // Use destinationLocation directly
        if (trip.getDestinationLocation() != null && !trip.getDestinationLocation().isEmpty()) {
            return trip.getDestinationLocation();
        }
        // Build from components as fallback
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

    private boolean hasValidAddresses(Trip trip) {
        String origin = getOriginAddress(trip);
        String destination = getDestinationAddress(trip);
        return origin != null && !origin.isEmpty() &&
               destination != null && !destination.isEmpty() &&
               !origin.equalsIgnoreCase(destination);
    }

    private BigDecimal calculateWithRetry(String origin, String destination, String vehicleType, Long tripId) {
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

            } catch (Exception e) {
                log.warn("⚠️ Attempt {} failed for Trip {}: {}", attempt, tripId, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("❌ All attempts failed for Trip {}", tripId);
                }
            }
        }
        return null;
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

    private boolean hasAddressChanged(Trip existing, UpdateTripRequest request) {
        if (request.getOriginLocation() != null && !request.getOriginLocation().equals(existing.getOriginLocation())) {
            return true;
        }
        if (request.getDestinationLocation() != null && !request.getDestinationLocation().equals(existing.getDestinationLocation())) {
            return true;
        }
        return false;
    }

    // ============================================================
    // GENERATE TRIP NUMBER
    // ============================================================

    private String generateTripNumberDirect() {
        try {
            String year = String.valueOf(java.time.Year.now().getValue());
            String prefix = "TRP-" + year + "-";
            
            Long nextNumber = jdbcTemplate.queryForObject(
                "INSERT INTO sequence (table_name, year, next_number, created_at, updated_at) " +
                "VALUES (?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (table_name, year) DO UPDATE SET next_number = sequence.next_number + 1 " +
                "RETURNING next_number - 1",
                new Object[]{TRIP_SEQUENCE_NAME, year},
                Long.class
            );
            
            String tripNumber = prefix + String.format("%03d", nextNumber);
            log.info("✅ Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating trip number: {}", e.getMessage());
            String fallback = "TRP-" + System.currentTimeMillis();
            log.warn("⚠️ Using fallback trip number: {}", fallback);
            return fallback;
        }
    }

    private String generateReferenceNumber() {
        try {
            String year = String.valueOf(java.time.Year.now().getValue());
            String prefix = "REF-" + year + "-";
            
            Long nextNumber = jdbcTemplate.queryForObject(
                "INSERT INTO sequence (table_name, year, next_number, created_at, updated_at) " +
                "VALUES (?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (table_name, year) DO UPDATE SET next_number = sequence.next_number + 1 " +
                "RETURNING next_number - 1",
                new Object[]{LOAD_REF_SEQUENCE_NAME, year},
                Long.class
            );
            
            String referenceNumber = prefix + String.format("%03d", nextNumber);
            log.info("✅ Generated reference number: {}", referenceNumber);
            return referenceNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating reference number: {}", e.getMessage());
            String fallback = "REF-" + System.currentTimeMillis();
            log.warn("⚠️ Using fallback reference number: {}", fallback);
            return fallback;
        }
    }

    private String getOrGenerateReferenceNumber(CreateTripRequest request) {
        if (request.getReferenceNumber() != null && !request.getReferenceNumber().trim().isEmpty()) {
            String refNumber = request.getReferenceNumber().trim();
            log.info("📝 Using user-provided reference number: {}", refNumber);
            return refNumber;
        }
        
        String generatedRef = generateReferenceNumber();
        log.info("📝 Generated reference number: {}", generatedRef);
        return generatedRef;
    }

    private Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + id));
    }

    private Customer validateAndGetCustomer(CreateTripRequest request) {
        if (request.getCustomerId() == null || request.getCustomerId() <= 0) {
            throw new TripValidationException("Customer is required. Please select a customer.");
        }
        
        return customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new TripValidationException(
                        "Customer not found with ID: " + request.getCustomerId()));
    }

    // ============================================================
    // HANDLE LOAD
    // ============================================================

    private Load handleLoad(CreateTripRequest request, Customer customer, Trip trip, Long userId) {
        String referenceNumber = getOrGenerateReferenceNumber(request);
        trip.setReferenceNumber(referenceNumber);
        
        log.info("📦 Looking for existing load with reference number: {}", referenceNumber);
        
        Optional<Load> existingLoad = loadRepository.findByReferenceNumber(referenceNumber);
        
        if (existingLoad.isPresent()) {
            Load load = existingLoad.get();
            log.info("📦 Found existing load with Ref# {}: {}", referenceNumber, load.getLoadNumber());
            
            trip.setLoad(load);
            trip.setLoadId(load.getLoadNumber());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : LOAD_STATUS_PENDING);
            
            if (load.getTrips() == null) {
                load.setTrips(new ArrayList<>());
            }
            load.getTrips().add(trip);
            load.setTripsCount(load.getTrips().size());
            load.setUpdatedAt(LocalDateTime.now());
            load.setLastStatusUpdate(LocalDateTime.now());
            
            if (!load.getTrips().isEmpty()) {
                Trip firstTrip = load.getTrips().get(0);
                
                if (trip.getVehicle() == null && firstTrip.getVehicle() != null) {
                    log.info("🚗 Pre-populating vehicle from first trip: {}", firstTrip.getVehicle().getId());
                    trip.setVehicle(firstTrip.getVehicle());
                }
                
                if (trip.getDriver() == null && firstTrip.getDriver() != null) {
                    log.info("👤 Pre-populating driver from first trip: {}", firstTrip.getDriver().getId());
                    trip.setDriver(firstTrip.getDriver());
                }
                
                if (trip.getSupervisor() == null && firstTrip.getSupervisor() != null) {
                    log.info("👤 Pre-populating supervisor from first trip: {}", firstTrip.getSupervisor().getId());
                    trip.setSupervisor(firstTrip.getSupervisor());
                }
                
                if (trip.getCustomerId() == null && firstTrip.getCustomerId() != null) {
                    trip.setCustomerId(firstTrip.getCustomerId());
                }
            }
            
            return load;
            
        } else {
            log.info("📦 No existing load found for Ref# {}. Creating new load.", referenceNumber);
            
            String loadNumber = loadNumberGenerator.generate();
            
            Load newLoad = new Load();
            newLoad.setLoadNumber(loadNumber);
            newLoad.setReferenceNumber(referenceNumber);
            newLoad.setCustomerId(customer.getId());
            
            String description = request.getCargoDescription() != null ? 
                request.getCargoDescription() : "Load for Ref# " + referenceNumber;
            newLoad.setDescription(description);
            newLoad.setCommodityType(request.getCommodityType());
            
            newLoad.setStatus(LOAD_STATUS_PENDING);
            newLoad.setTripsCount(0);
            newLoad.setCreatedBy(userId != null ? String.valueOf(userId) : "System");
            newLoad.setCreatedAt(LocalDateTime.now());
            newLoad.setUpdatedAt(LocalDateTime.now());
            newLoad.setLastStatusUpdate(LocalDateTime.now());
            newLoad.setAuditTrail("{}");
            
            newLoad.setOriginLocation(request.getOriginLocation());
            newLoad.setDestinationLocation(request.getDestinationLocation());
            
            if (request.getFromDepotKm() != null) {
                newLoad.setTotalFromDepotKm(request.getFromDepotKm());
            }
            if (request.getToDepotKm() != null) {
                newLoad.setTotalToDepotKm(request.getToDepotKm());
            }
            
            newLoad.setTrips(new ArrayList<>());
            newLoad.getTrips().add(trip);
            newLoad.setTripsCount(1);
            
            Load savedLoad = loadRepository.save(newLoad);
            
            trip.setLoad(savedLoad);
            trip.setLoadId(savedLoad.getLoadNumber());
            trip.setLoadNumber(savedLoad.getLoadNumber());
            trip.setLoadType(savedLoad.getCommodityType());
            trip.setLoadDescription(savedLoad.getDescription());
            trip.setLoadStatus(savedLoad.getStatus() != null ? savedLoad.getStatus() : LOAD_STATUS_PENDING);
            
            log.info("✅ Created new load: {} for Ref#: {}", savedLoad.getLoadNumber(), referenceNumber);
            return savedLoad;
        }
    }

    // ============================================================
    // CREATE TRIP
    // ============================================================

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, Long userId) {
        log.debug("Creating trip for vehicle: {}, user: {}", request.getVehicleId(), userId);
        log.info("📝 Creating trip with reference number: {}", request.getReferenceNumber());
    
        tripValidator.validateCreateRequest(request);
    
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new TripValidationException(
                        "Vehicle not found with ID: " + request.getVehicleId()));
    
        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new TripValidationException(
                            "Driver not found with ID: " + request.getDriverId()));
        }
    
        Driver supervisor = null;
        if (request.getSupervisorId() != null) {
            supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new TripValidationException(
                            "Supervisor not found with ID: " + request.getSupervisorId()));
        }
    
        Customer customer = validateAndGetCustomer(request);
        Long customerId = customer.getId();
        log.info("✅ Customer validated: {} (ID: {})", customer.getName(), customerId);
    
        Trip trip = createTripMapper.toEntity(request);
        log.info("🔍 Trip created by mapper, hash: {}", System.identityHashCode(trip));
    
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSupervisor(supervisor);
        trip.setCustomerId(customerId);
    
        // ============================================================
        // Set crane_used and related fields
        // ============================================================
        Boolean craneUsed = request.getCraneUsed() != null ? request.getCraneUsed() : false;
        trip.setCraneUsed(craneUsed);
        
        // If crane is used, set crane type and hours if provided
        if (craneUsed) {
           if (Boolean.TRUE.equals(request.getCraneUsed())) {
            log.info("🏗️ Crane used for trip");
        }
        
        // Optional: Auto-detect crane usage from trip_type if not explicitly set
        if (!craneUsed && request.getTripType() != null) {
            String tripType = request.getTripType().toUpperCase();
            if (tripType.contains("CRANE") || tripType.contains("LIFT") || tripType.contains("HEAVY")) {
                trip.setCraneUsed(true);
                log.info("🏗️ Crane usage auto-detected from trip_type: {}", request.getTripType());
            }
        }
        // ============================================================
    
        if (request.getFromDepotKm() != null) {
            trip.setFromDepotKm(request.getFromDepotKm());
        }
        if (request.getToDepotKm() != null) {
            trip.setToDepotKm(request.getToDepotKm());
        }
        if (request.getDepartedFrom() != null) {
            trip.setDepartedFrom(request.getDepartedFrom());
        }
        if (request.getDepartureLocation() != null) {
            trip.setDepartureLocation(request.getDepartureLocation());
        }
        trip.setIsFromDepot(request.getIsFromDepot() != null ? request.getIsFromDepot() : false);
    
        Load load = handleLoad(request, customer, trip, userId);
    
        String status = request.getStatus() != null ? request.getStatus() : STATUS_DRAFT;
        trip.setStatus(status);
        trip.setCreatedBy(userId);
        trip.setLastStatusUpdate(LocalDateTime.now());
    
        log.info("========================================");
        log.info("🔢 GENERATING TRIP NUMBER");
        log.info("========================================");
        
        String tripNumber = generateTripNumberDirect();
        trip.setTripNumber(tripNumber);
        
        if (trip.getTripNumber() == null || trip.getTripNumber().trim().isEmpty()) {
            trip.setTripNumber("TRP-EMERG-" + System.currentTimeMillis());
            log.error("🚨 Emergency trip number set: {}", trip.getTripNumber());
        }
        log.info("========================================");
    
        log.info("🚀 Pre-save validation:");
        log.info("   - Trip Number: '{}'", trip.getTripNumber());
        log.info("   - Reference Number: '{}'", trip.getReferenceNumber());
        log.info("   - Customer ID: {}", trip.getCustomerId());
        log.info("   - Load ID: {}", trip.getLoadId());
        log.info("   - Crane Used: {}", trip.getCraneUsed());
        log.info("   - Crane Type: {}", trip.getCraneType());
        
        if (trip.getTripNumber() == null || trip.getTripNumber().trim().isEmpty()) {
            throw new TripValidationException("Trip number is null or empty before saving!");
        }
        if (trip.getCustomerId() == null) {
            throw new TripValidationException("Customer ID cannot be null before saving");
        }
        if (trip.getReferenceNumber() == null || trip.getReferenceNumber().trim().isEmpty()) {
            throw new TripValidationException("Reference number cannot be null before saving");
        }
    
        log.info("💾 Saving trip to database...");
        Trip saved = tripRepository.save(trip);
    
        // ✅ Calculate estimated billing immediately after creation
        try {
            billingCalculatorService.calculateTripBilling(saved.getId(), userId);
            log.info("💰 Estimated billing calculated for trip {}", saved.getId());
        } catch (Exception e) {
            log.error("❌ Failed to calculate estimated billing: {}", e.getMessage(), e);
            // Don't fail trip creation
        }
    
        if (load != null && load.getId() != null) {
            load.setTripsCount(load.getTrips().size());
            load.setUpdatedAt(LocalDateTime.now());
            load.setLastStatusUpdate(LocalDateTime.now());
            load.recalculateDepotTotals();
            loadRepository.save(load);
            log.info("📦 Updated existing load {} with new trip {}", load.getLoadNumber(), saved.getId());
        }
    
        entityManager.flush();
    
        log.info("✅ Created trip with ID: {}, Number: {}, Reference: {}, Customer: {}, Load: {}, Crane: {}",
                saved.getId(),
                saved.getTripNumber(),
                saved.getReferenceNumber(),
                customer.getName(),
                load != null ? load.getLoadNumber() : "None",
                saved.getCraneUsed() ? "Yes" : "No"
        );
    
        tripMetricsService.initializeMetrics(saved.getId());
    
        if (STATUS_PLANNED.equals(saved.getStatus())) {
            eventPublisher.publishEvent(new TripPlannedEvent(saved.getId()));
        }
    
        // ✅ Trigger distance calculation
        calculateTripDistance(saved.getId());
    
        return tripResponseMapper.toResponse(saved);
    }

    // ============================================================
    // START TRIP
    // ============================================================

    @Transactional
    public TripResponse startTrip(Long tripId, BigDecimal actualStartOdometer, Long userId) {
        log.debug("Starting trip ID: {} with odometer: {}", tripId, actualStartOdometer);
        
        Trip trip = findTripOrThrow(tripId);
        tripValidator.validateCanStart(trip, actualStartOdometer);

        trip.setActualStartOdometer(actualStartOdometer);
        trip.setActualStartDate(LocalDateTime.now());
        trip.setStatus(STATUS_IN_PROGRESS);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        Trip updated = tripRepository.save(trip);
        eventPublisher.publishEvent(new TripStartedEvent(tripId));
        log.info("Trip {} started", tripId);

        return tripResponseMapper.toResponse(updated);
    }

    // ============================================================
    // END TRIP
    // ============================================================

    @Transactional
public TripResponse endTrip(Long tripId, BigDecimal actualEndOdometer, Long userId) {
    log.debug("Ending trip ID: {} with odometer: {}", tripId, actualEndOdometer);
    
    Trip trip = findTripOrThrow(tripId);
    tripValidator.validateCanEnd(trip, actualEndOdometer);

    BigDecimal startOdo = trip.getActualStartOdometer();
    
    trip.setActualEndOdometer(actualEndOdometer);
    trip.setActualEndDate(LocalDateTime.now());
    trip.setStatus(STATUS_COMPLETED);
    trip.setLastStatusUpdate(LocalDateTime.now());
    trip.setUpdatedAt(LocalDateTime.now());
    trip.setUpdatedBy(userId);
    trip.setActualDistanceKm(actualEndOdometer.subtract(startOdo));
    
    if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
        long hours = java.time.Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toHours();
        trip.setActualDurationHours(BigDecimal.valueOf(hours));
    }

    // ✅ Save trip first - this ensures the trip is updated even if billing fails
    Trip updated = tripRepository.save(trip);
    eventPublisher.publishEvent(new TripCompletedEvent(tripId));
    log.info("Trip {} completed. Distance: {} km", tripId, trip.getActualDistanceKm());

    // ✅ Calculate billing in a separate transaction to avoid rollback issues
    try {
        // Use a new transaction for billing calculation
        billingCalculatorService.calculateTripBillingInNewTransaction(tripId, userId);
        log.info("💰 Billing calculated for trip {}", tripId);
    } catch (Exception e) {
        // ✅ Log error but don't rollback the trip transaction
        log.error("❌ Failed to calculate billing for trip {}: {}", tripId, e.getMessage(), e);
    }

    return tripResponseMapper.toResponse(updated);
}



    // ============================================================
    // READ METHODS
    // ============================================================

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id) {
        Trip trip = tripRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + id));
        return tripResponseMapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(Pageable pageable) {
        log.info("📊 Fetching trips with pageable: {}", pageable);
        
        Page<Trip> trips = tripRepository.findAllOrderByIdDesc(pageable);
        
        log.info("📊 Found {} trips total", trips.getTotalElements());
        log.info("📊 Content size: {}", trips.getContent().size());
        
        trips.getContent().forEach(trip -> {
            if (trip.getCustomer() != null) {
                trip.getCustomer().getName();
                trip.getCustomer().getCustomerCode();
            }
            if (trip.getVehicle() != null) {
                trip.getVehicle().getRegistrationNumber();
                trip.getVehicle().getMake();
                trip.getVehicle().getModel();
            }
            if (trip.getDriver() != null) {
                trip.getDriver().getFirstName();
                trip.getDriver().getLastName();
                trip.getDriver().getLicenseNumber();
            }
            if (trip.getSupervisor() != null) {
                trip.getSupervisor().getFirstName();
                trip.getSupervisor().getLastName();
            }
            if (trip.getLoad() != null) {
                trip.getLoad().getLoadNumber();
                trip.getLoad().getDescription();
            }
            if (trip.getMetrics() != null) {
                trip.getMetrics().getTotalDistanceKm();
            }
        });
        
        if (!trips.getContent().isEmpty()) {
            Trip first = trips.getContent().get(0);
            log.info("📊 First trip: ID={}, Number={}, Status={}, Customer={}, Vehicle={}, Driver={}", 
                first.getId(), 
                first.getTripNumber(), 
                first.getStatus(),
                first.getCustomer() != null ? first.getCustomer().getName() : "null",
                first.getVehicle() != null ? first.getVehicle().getRegistrationNumber() : "null",
                first.getDriver() != null ? first.getDriver().getFirstName() : "null"
            );
        }
        
        return trips.map(tripResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByLoad(String loadId) {
        return tripRepository.findByLoadId(loadId)
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getTripsByCustomerPaginated(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new TripValidationException("Customer not found with ID: " + customerId);
        }
        return tripRepository.findByCustomerId(customerId, pageable)
                .map(tripResponseMapper::toResponse);
    }

    // ============================================================
    // STATUS UPDATE
    // ============================================================

    @Transactional
    public TripResponse updateTripStatus(Long tripId, String newStatus, Long userId) {
        log.debug("Updating trip {} status to: {}", tripId, newStatus);
        
        Trip trip = findTripOrThrow(tripId);
        tripValidator.validateStatusTransition(trip.getStatus(), newStatus);
        
        String oldStatus = trip.getStatus();
        trip.setStatus(newStatus);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        if (STATUS_CANCELLED.equals(newStatus)) {
            trip.setCancelledAt(LocalDateTime.now());
        }
        
        if (STATUS_COMPLETED.equals(newStatus) && !STATUS_COMPLETED.equals(oldStatus)) {
            trip.calculateActualDistance();
            if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
                long hours = java.time.Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toHours();
                trip.setActualDurationHours(BigDecimal.valueOf(hours));
            }
        }

        Trip saved = tripRepository.save(trip);

        if (STATUS_PLANNED.equals(newStatus)) {
            eventPublisher.publishEvent(new TripPlannedEvent(tripId));
        } else if (STATUS_IN_PROGRESS.equals(newStatus)) {
            eventPublisher.publishEvent(new TripStartedEvent(tripId));
        } else if (STATUS_COMPLETED.equals(newStatus)) {
            eventPublisher.publishEvent(new TripCompletedEvent(tripId));
        }
        
        log.info("Trip {} status changed from {} to {}", tripId, oldStatus, newStatus);

        return tripResponseMapper.toResponse(saved);
    }

    // ============================================================
    // CUSTOMER & LOAD MANAGEMENT
    // ============================================================

    @Transactional
    public TripResponse assignCustomerToTrip(Long tripId, Long customerId, Long userId) {
        log.debug("Assigning customer {} to trip {}", customerId, tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        if (customerId == null || customerId <= 0) {
            throw new TripValidationException("Customer ID is required");
        }
        
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + customerId));
        trip.setCustomerId(customer.getId());
        
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        Trip updated = tripRepository.save(trip);
        log.info("Customer assigned to trip {}: {}", tripId, customerId);
        
        return tripResponseMapper.toResponse(updated);
    }

    @Transactional
    public TripResponse assignLoadToTrip(Long tripId, String loadId, Long userId) {
        log.debug("Assigning load {} to trip {}", loadId, tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        if (loadId != null && !loadId.isEmpty()) {
            Load load = loadRepository.findByLoadNumber(loadId)
                    .orElseThrow(() -> new TripValidationException("Load not found with number: " + loadId));
            trip.setLoad(load);
            trip.setLoadId(load.getLoadNumber());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : LOAD_STATUS_PENDING);
        } else {
            trip.setLoad(null);
            trip.setLoadId(null);
            trip.setLoadNumber(null);
            trip.setLoadType(null);
            trip.setLoadDescription(null);
            trip.setLoadStatus(null);
        }
        
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        Trip updated = tripRepository.save(trip);
        log.info("Load assigned to trip {}: {}", tripId, loadId);
        
        return tripResponseMapper.toResponse(updated);
    }

    // ============================================================
    // INCIDENT RULE
    // ============================================================

    public boolean canReportIncident(Trip trip) {
        return trip.isActive();
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Transactional
    public void deleteTrip(Long id) {
        log.debug("Deleting trip ID: {}", id);
        
        Trip trip = findTripOrThrow(id);
        
        String status = trip.getStatus();
        if (STATUS_COMPLETED.equals(status) || STATUS_FINALIZED.equals(status) || STATUS_CANCELLED.equals(status)) {
            throw new TripValidationException("Cannot delete trip with terminal status: " + status);
        }
        
        if (trip.getMetrics() != null) {
            trip.setMetrics(null);
        }
        
        tripRepository.delete(trip);
        log.info("Deleted trip ID: {}", id);
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Transactional
    public TripResponse updateTrip(Long tripId, UpdateTripRequest request, Long userId) {
        log.debug("Updating trip ID: {} with request", tripId);
        
        Trip trip = findTripOrThrow(tripId);
        tripValidator.validateCanUpdate(trip);
        
        LocalDateTime now = LocalDateTime.now();

        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new TripValidationException("Vehicle not found with ID: " + request.getVehicleId()));
            trip.setVehicle(vehicle);
        }

        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new TripValidationException("Driver not found with ID: " + request.getDriverId()));
            trip.setDriver(driver);
        }

        if (request.getSupervisorId() != null) {
            Driver supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new TripValidationException("Supervisor not found with ID: " + request.getSupervisorId()));
            trip.setSupervisor(supervisor);
        }

        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + request.getCustomerId()));
            trip.setCustomerId(customer.getId());
        }

        if (request.getLoadId() != null) {
            if (!request.getLoadId().isEmpty()) {
                Load load = loadRepository.findByLoadNumber(request.getLoadId())
                        .orElseThrow(() -> new TripValidationException("Load not found with number: " + request.getLoadId()));
                trip.setLoad(load);
                trip.setLoadId(load.getLoadNumber());
                trip.setLoadNumber(load.getLoadNumber());
                trip.setLoadType(load.getCommodityType());
                trip.setLoadDescription(load.getDescription());
                trip.setLoadStatus(load.getStatus() != null ? load.getStatus() : LOAD_STATUS_PENDING);
            } else {
                trip.setLoad(null);
                trip.setLoadId(null);
                trip.setLoadNumber(null);
                trip.setLoadType(null);
                trip.setLoadDescription(null);
                trip.setLoadStatus(null);
            }
        }

        if (request.getFromDepotKm() != null) {
            trip.setFromDepotKm(request.getFromDepotKm());
        }
        if (request.getToDepotKm() != null) {
            trip.setToDepotKm(request.getToDepotKm());
        }
        if (request.getDepartedFrom() != null) {
            trip.setDepartedFrom(request.getDepartedFrom());
        }
        if (request.getDepartureLocation() != null) {
            trip.setDepartureLocation(request.getDepartureLocation());
        }
        if (request.getIsFromDepot() != null) {
            trip.setIsFromDepot(request.getIsFromDepot());
        }

        Optional.ofNullable(request.getOriginLocation()).ifPresent(trip::setOriginLocation);
        Optional.ofNullable(request.getDestinationLocation()).ifPresent(trip::setDestinationLocation);
        Optional.ofNullable(request.getOriginStreetAddress()).ifPresent(trip::setOriginStreetAddress);
        Optional.ofNullable(request.getOriginCity()).ifPresent(trip::setOriginCity);
        Optional.ofNullable(request.getOriginZipCode()).ifPresent(trip::setOriginZipCode);
        Optional.ofNullable(request.getOriginProvince()).ifPresent(trip::setOriginProvince);
        Optional.ofNullable(request.getDestinationStreetAddress()).ifPresent(trip::setDestinationStreetAddress);
        Optional.ofNullable(request.getDestinationCity()).ifPresent(trip::setDestinationCity);
        Optional.ofNullable(request.getDestinationZipCode()).ifPresent(trip::setDestinationZipCode);
        Optional.ofNullable(request.getDestinationProvince()).ifPresent(trip::setDestinationProvince);
        Optional.ofNullable(request.getActualStartDate()).ifPresent(trip::setActualStartDate);
        Optional.ofNullable(request.getActualEndDate()).ifPresent(trip::setActualEndDate);
        Optional.ofNullable(request.getActualStartOdometer()).ifPresent(trip::setActualStartOdometer);
        Optional.ofNullable(request.getActualEndOdometer()).ifPresent(trip::setActualEndOdometer);
        
        if (request.getActualStartOdometer() != null && request.getActualEndOdometer() != null) {
            trip.setActualDistanceKm(request.getActualEndOdometer().subtract(request.getActualStartOdometer()));
        }

        Optional.ofNullable(request.getPlannedStartDate()).ifPresent(trip::setPlannedStartDate);
        Optional.ofNullable(request.getPlannedEndDate()).ifPresent(trip::setPlannedEndDate);
        Optional.ofNullable(request.getPlannedDistanceKm()).ifPresent(trip::setPlannedDistanceKm);
        Optional.ofNullable(request.getEstimatedDurationHours()).ifPresent(trip::setEstimatedDurationHours);
        Optional.ofNullable(request.getTollCost()).ifPresent(trip::setTollCost);
        Optional.ofNullable(request.getOtherExpenses()).ifPresent(trip::setOtherExpenses);
        Optional.ofNullable(request.getFuelConsumedLiters()).ifPresent(trip::setFuelConsumedLiters);
        Optional.ofNullable(request.getDriverNotes()).ifPresent(trip::setDriverNotes);

        if (request.getStatus() != null && !request.getStatus().equals(trip.getStatus())) {
            tripValidator.validateStatusTransition(trip.getStatus(), request.getStatus());
            trip.setStatus(request.getStatus());
            trip.setLastStatusUpdate(now);
            
            if (STATUS_CANCELLED.equals(request.getStatus())) {
                trip.setCancelledAt(now);
            }
        }

        trip.setUpdatedAt(now);
        trip.setUpdatedBy(userId);
        trip.updateOriginLocationFromComponents();
        trip.updateDestinationLocationFromComponents();

        // ✅ Check if address changed and recalculate distance
        if (hasAddressChanged(trip, request)) {
            trip.setDistanceCalculated(false);
            calculateTripDistance(tripId);
        }

        if (trip.getLoad() != null) {
            trip.getLoad().recalculateDepotTotals();
            loadRepository.save(trip.getLoad());
        }

        Trip saved = tripRepository.save(trip);
        log.info("Updated trip ID: {}", tripId);

        return tripResponseMapper.toResponse(saved);
    }

    // ============================================================
    // SEARCH METHODS
    // ============================================================

    @Transactional(readOnly = true)
    public Page<TripResponse> searchTrips(String searchTerm, Pageable pageable) {
        log.debug("Searching trips with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return tripRepository.findAllWithCustomer(pageable)
                    .map(tripResponseMapper::toResponse);
        }
        
        return tripRepository.searchTripsWithCustomer(searchTerm.trim(), pageable)
                .map(tripResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> searchTripsWithFilters(
            String searchTerm, 
            String status, 
            String city, 
            String customer,
            Pageable pageable) {
        
        log.debug("Searching trips with filters - term: {}, status: {}, city: {}, customer: {}", 
                searchTerm, status, city, customer);
        
        return tripRepository.findWithFiltersOrderByIdDesc(searchTerm, status, city, customer, pageable)
                .map(tripResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> searchTripsSimple(String searchTerm) {
        log.debug("Simple search trips with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return tripRepository.findAllOrderByIdDesc()
                    .stream()
                    .map(tripResponseMapper::toResponse)
                    .collect(Collectors.toList());
        }
        
        return tripRepository.searchTripsOrderByIdDesc(searchTerm.trim())
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getActiveTrips() {
        return tripRepository.findActiveTripsOrderByIdDesc()
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getCurrentlyRunningTrips() {
        return tripRepository.findCurrentlyRunningTripsOrderByIdDesc()
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }
}
