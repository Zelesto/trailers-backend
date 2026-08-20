// src/main/java/com/pgsa/trailers/controller/TripController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.service.TripService;
import com.pgsa.trailers.service.TripFinalisationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;
    private final TripFinalisationService tripFinalisationService;
    private final AppUserRepository appUserRepository;
    private final TripRepository tripRepository;
    private final TripResponseMapper tripResponseMapper;

    /* ============================================================
       HEALTH CHECK ENDPOINTS
       ============================================================ */

    /**
     * Health check endpoint to verify database connection
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("🏥 Health check requested");
        try {
            long count = tripRepository.count();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("tripCount", count);
            response.put("database", "Connected");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            log.info("✅ Health check: {} trips found", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
        }
    }

    /**
     * Debug endpoint to check trip data
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugTrips() {
        log.info("🐛 Debug endpoint called");
        try {
            long totalCount = tripRepository.count();
            log.info("📊 Total trips in database: {}", totalCount);
            
            List<Trip> sampleTrips = tripRepository.findAll(PageRequest.of(0, 5)).getContent();
            
            List<Object[]> statusCount = tripRepository.countByStatusGrouped();
            Map<String, Long> statusBreakdown = new HashMap<>();
            for (Object[] row : statusCount) {
                String status = row[0] != null ? row[0].toString() : "NULL";
                Long count = (Long) row[1];
                statusBreakdown.put(status, count);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalTrips", totalCount);
            response.put("statusBreakdown", statusBreakdown);
            response.put("sampleTrips", sampleTrips.stream()
                .map(t -> {
                    Map<String, Object> tripInfo = new HashMap<>();
                    tripInfo.put("id", t.getId());
                    tripInfo.put("tripNumber", t.getTripNumber());
                    tripInfo.put("status", t.getStatus());
                    tripInfo.put("customerId", t.getCustomerId());
                    tripInfo.put("vehicleId", t.getVehicle() != null ? t.getVehicle().getId() : null);
                    tripInfo.put("driverId", t.getDriver() != null ? t.getDriver().getId() : null);
                    tripInfo.put("createdAt", t.getCreatedAt());
                    return tripInfo;
                })
                .toList());
            
            log.info("✅ Debug data: {} trips, {} statuses", totalCount, statusBreakdown.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Debug endpoint failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /* ============================================================
       CREATE
       ============================================================ */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> createTrip(
            @RequestBody @Valid CreateTripRequest request,
            Authentication authentication
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        log.debug("Creating trip for user: {}", user.getEmail());

        TripResponse response = tripService.createTrip(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ============================================================
       READ - SINGLE TRIP
       ============================================================ */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long id) {
        log.debug("Fetching trip id: {}", id);
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    /* ============================================================
       LIST TRIPS - MAIN ENDPOINT
       ============================================================ */
    @GetMapping
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
@Transactional(readOnly = true)
public ResponseEntity<Page<TripResponse>> listTrips(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String customer,
        Pageable pageable
) {
    log.info("========================================");
    log.info("📊 listTrips called");
    log.info("   Status: {}", status);
    log.info("   Search: {}", search);
    log.info("   CustomerId: {}", customerId);
    log.info("   City: {}", city);
    log.info("   Customer: {}", customer);
    log.info("   Page: {}", pageable.getPageNumber());
    log.info("   Size: {}", pageable.getPageSize());
    log.info("   Sort: {}", pageable.getSort());
    log.info("========================================");
    
    try {
        long totalTrips = tripRepository.count();
        log.info("📊 Total trips in database: {}", totalTrips);
        
        if (totalTrips == 0) {
            log.warn("⚠️ No trips found in database!");
            return ResponseEntity.ok(Page.empty(pageable));
        }
        
        Page<Trip> trips;
        
        // 1. Search filter
        if (search != null && !search.trim().isEmpty()) {
            log.info("🔍 Searching trips with: {}", search);
            trips = tripRepository.searchTrips(search.trim(), pageable);
            log.info("✅ Search returned: {} of {} total trips", 
                trips.getContent().size(), trips.getTotalElements());
            return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        }
        
        // 2. Customer ID filter
        if (customerId != null) {
            log.info("👤 Filtering by customerId: {}", customerId);
            trips = tripRepository.findByCustomerId(customerId, pageable);
            log.info("✅ Customer filter returned: {} of {} total trips", 
                trips.getContent().size(), trips.getTotalElements());
            return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        }
        
        // 3. Status filter
        if (status != null && !status.trim().isEmpty()) {
            log.info("🏷️ Filtering by status: {}", status);
            List<TripStatus> statuses = parseStatuses(status);
            if (statuses.isEmpty()) {
                log.warn("⚠️ No valid statuses found in: {}", status);
                return ResponseEntity.ok(Page.empty(pageable));
            }
            trips = tripRepository.findByStatusIn(statuses, pageable);
            log.info("✅ Status filter returned: {} of {} total trips", 
                trips.getContent().size(), trips.getTotalElements());
            return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        }
        
        // 4. City filter
        if (city != null && !city.trim().isEmpty()) {
            log.info("🏙️ Filtering by city: {}", city);
            trips = tripRepository.findByOriginCityOrDestinationCity(city, pageable);
            log.info("✅ City filter returned: {} of {} total trips", 
                trips.getContent().size(), trips.getTotalElements());
            return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        }
        
        // 5. Customer name filter
        if (customer != null && !customer.trim().isEmpty()) {
            log.info("👤 Filtering by customer name: {}", customer);
            trips = tripRepository.findByCustomerNameContaining(customer, pageable);
            log.info("✅ Customer name filter returned: {} of {} total trips", 
                trips.getContent().size(), trips.getTotalElements());
            return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        }
        
        // 6. ✅ FIXED: No filters - Return all trips using findAllTrips
        log.info("📋 Returning all trips (no filters)");
        trips = tripRepository.findAllTrips(pageable);  // ✅ FIXED
        log.info("✅ Returned: {} of {} total trips", 
            trips.getContent().size(), trips.getTotalElements());
        
        log.info("📄 Page {} of {} (total: {} items)", 
            pageable.getPageNumber() + 1, 
            trips.getTotalPages(), 
            trips.getTotalElements());
        
        return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        
    } catch (Exception e) {
        log.error("❌ Error listing trips: {}", e.getMessage(), e);
        return ResponseEntity.ok(Page.empty(pageable));
    }
}

    /* ============================================================
       GET TRIPS WITHOUT LOAD
       ============================================================ */
    @GetMapping("/without-load")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<Page<TripResponse>> getTripsWithoutLoad(
            @RequestParam(required = false) String status,
            Pageable pageable
    ) {
        log.info("📋 Fetching trips without load assigned, status: {}", status);
        
        try {
            if (status != null && !status.trim().isEmpty()) {
                List<TripStatus> statuses = parseStatuses(status);
                if (!statuses.isEmpty()) {
                    Page<Trip> trips = tripRepository.findByLoadIdIsNullAndStatusIn(statuses, pageable);
                    log.info("✅ Found {} trips without load with status filter", trips.getTotalElements());
                    return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
                }
            }
            
            Page<Trip> trips = tripRepository.findByLoadIdIsNull(pageable);
            log.info("✅ Found {} trips without load", trips.getTotalElements());
            return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
            
        } catch (Exception e) {
            log.error("❌ Error fetching trips without load: {}", e.getMessage(), e);
            return ResponseEntity.ok(Page.empty(pageable));
        }
    }
    
    /* ============================================================
       SEARCH ENDPOINTS - MOVED BEFORE @PathVariable to avoid conflicts
       ============================================================ */
    @GetMapping("/search")
    public ResponseEntity<Page<TripResponse>> searchTrips(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String customer) {
        
        log.info("🔍 Search trips - term: {}, status: {}, city: {}, customer: {}", 
            searchTerm, status, city, customer);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        if (status != null || city != null || customer != null) {
            return ResponseEntity.ok(
                tripService.searchTripsWithFilters(searchTerm, status, city, customer, pageable)
            );
        }
        
        return ResponseEntity.ok(tripService.searchTrips(searchTerm, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<TripResponse>> getActiveTrips() {
        log.info("📋 Fetching active trips");
        return ResponseEntity.ok(tripService.getActiveTrips());
    }

    @GetMapping("/running")
    public ResponseEntity<List<TripResponse>> getCurrentlyRunningTrips() {
        log.info("📋 Fetching currently running trips");
        return ResponseEntity.ok(tripService.getCurrentlyRunningTrips());
    }

    /* ============================================================
       FINALIZE TRIP
       ============================================================ */
    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<Void> finalizeTrip(@PathVariable Long id) {
        log.info("📨 Received finalize request for trip: {}", id);
        try {
            tripFinalisationService.finalizeTrip(id);
            log.info("✅ Trip {} finalized successfully", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Error finalizing trip {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /* ============================================================
       CAN FINALIZE CHECK
       ============================================================ */
    @GetMapping("/{id}/can-finalize")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<Boolean> canFinalize(@PathVariable Long id) {
        log.info("📨 Checking if trip {} can be finalized", id);
        boolean canFinalize = tripFinalisationService.canFinalize(id);
        return ResponseEntity.ok(canFinalize);
    }

    /* ============================================================
       UPDATE STATUS
       ============================================================ */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> updateTripStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication
    ) {
        log.debug("Updating status for trip {} to {}", id, status);
        
        AppUser user = getAuthenticatedUser(authentication);
        
        TripStatus newStatus;
        try {
            newStatus = TripStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
        
        TripResponse response = tripService.updateTripStatus(id, newStatus, user.getId());
        return ResponseEntity.ok(response);
    }

    /* ============================================================
       UPDATE TRIP
       ============================================================ */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTripRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        TripResponse updated = tripService.updateTrip(id, request, user.getId());
        return ResponseEntity.ok(updated);
    }

    /* ============================================================
       START TRIP (ODO START)
       ============================================================ */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<TripResponse> startTrip(
            @PathVariable Long id,
            @RequestBody @Valid StartTripRequest request,
            Authentication authentication
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        log.debug("Driver {} starting trip {} with odo {}",
                user.getId(), id, request.actualStartOdometer());

        TripResponse response = tripService.startTrip(
                id,
                request.actualStartOdometer(),
                user.getId()
        );

        return ResponseEntity.ok(response);
    }

    /* ============================================================
       END TRIP (ODO END)
       ============================================================ */
    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<TripResponse> endTrip(
            @PathVariable Long id,
            @RequestBody @Valid EndTripRequest request,
            Authentication authentication
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        log.debug("Driver {} ending trip {} with odo {}",
                user.getId(), id, request.actualEndOdometer());

        TripResponse response = tripService.endTrip(
                id,
                request.actualEndOdometer(),
                user.getId()
        );

        return ResponseEntity.ok(response);
    }

    /* ============================================================
       DELETE TRIP
       ============================================================ */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Long id) {
        log.debug("Deleting trip id: {}", id);
        tripService.deleteTrip(id);
        log.debug("Trip and associated metrics deleted for id: {}", id);
    }


    /* ============================================================
   GET TRIPS BY VEHICLE
   ============================================================ */
@GetMapping("/vehicle/{vehicleId}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
@Transactional(readOnly = true)
public ResponseEntity<?> getTripsByVehicle(
        @PathVariable Long vehicleId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size,
        @RequestParam(required = false) String status
) {
    log.info("========================================");
    log.info("📊 getTripsByVehicle called");
    log.info("   Vehicle ID: {}", vehicleId);
    log.info("   Page: {}", page);
    log.info("   Size: {}", size);
    log.info("   Status: {}", status);
    log.info("========================================");
    
    try {
        // Validate vehicle ID
        if (vehicleId == null || vehicleId <= 0) {
            log.warn("⚠️ Invalid vehicle ID: {}", vehicleId);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid vehicle ID"));
        }
        
        // Build pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        Page<Trip> trips;
        
        // Filter by status if provided
        if (status != null && !status.trim().isEmpty()) {
            List<TripStatus> statuses = parseStatuses(status);
            if (!statuses.isEmpty()) {
                trips = tripRepository.findTripsByVehicleIdAndStatusIn(vehicleId, statuses, pageable);
            } else {
                trips = tripRepository.findTripsByVehicleId(vehicleId, pageable);
            }
        } else {
            trips = tripRepository.findTripsByVehicleId(vehicleId, pageable);
        }
        
        log.info("✅ Found {} trips for vehicle {}", trips.getTotalElements(), vehicleId);
        log.info("📄 Page {} of {} (total: {} items)", 
            page + 1, 
            trips.getTotalPages(), 
            trips.getTotalElements());
        
        return ResponseEntity.ok(trips.map(tripResponseMapper::toResponse));
        
    } catch (Exception e) {
        log.error("❌ Error fetching trips for vehicle {}: {}", vehicleId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Failed to fetch trips for vehicle",
                "vehicleId", vehicleId,
                "message", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
    }
}
    

    /* ============================================================
   GET TRIPS BY DRIVER - ADD THIS ENDPOINT
   ============================================================ */
// In TripController.java - Update getTripsByDriver method

// In TripController.java - update getTripsByDriver method

@GetMapping("/driver/{driverId}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
@Transactional(readOnly = true)
public ResponseEntity<?> getTripsByDriver(
        @PathVariable Long driverId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "100") int size,
        @RequestParam(required = false) String status
) {
    log.info("========================================");
    log.info("📊 getTripsByDriver called");
    log.info("   Driver ID: {}", driverId);
    log.info("   Page: {}", page);
    log.info("   Size: {}", size);
    log.info("   Status: {}", status);
    log.info("========================================");
    
    try {
        // Validate driver ID
        if (driverId == null || driverId <= 0) {
            log.warn("⚠️ Invalid driver ID: {}", driverId);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid driver ID"));
        }
        
        // Build pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        Page<Trip> trips;
        
        // Try using native query first (more reliable)
        if (status != null && !status.trim().isEmpty()) {
            String[] statusArray = status.split(",");
            List<String> statuses = Arrays.asList(statusArray);
            trips = tripRepository.findTripsByDriverIdAndStatusInNative(driverId, statuses, pageable);
        } else {
            trips = tripRepository.findTripsByDriverIdNative(driverId, pageable);
        }
        
        log.info("✅ Found {} trips for driver {}", trips.getTotalElements(), driverId);
        log.info("📄 Page {} of {} (total: {} items)", 
            page + 1, 
            trips.getTotalPages(), 
            trips.getTotalElements());
        
        // Return the content as a list instead of a paginated response
        // This matches what the frontend expects
        List<TripResponse> tripResponses = trips.getContent().stream()
            .map(tripResponseMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(tripResponses);
        
    } catch (Exception e) {
        log.error("❌ Error fetching trips for driver {}: {}", driverId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Failed to fetch trips for driver",
                "driverId", driverId,
                "message", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            ));
    }
}
    
    
    /* ============================================================
       HELPER METHODS
       ============================================================ */
    private AppUser getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    /**
     * Parse comma-separated status string into list of TripStatus enums
     */
    private List<TripStatus> parseStatuses(String status) {
        List<TripStatus> statuses = new ArrayList<>();
        if (status == null || status.trim().isEmpty()) {
            return statuses;
        }
        
        String[] statusArray = status.split(",");
        for (String s : statusArray) {
            try {
                statuses.add(TripStatus.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value: {}, skipping", s);
            }
        }
        return statuses;
    }
}
