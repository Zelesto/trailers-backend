// src/main/java/com/pgsa/trailers/controller/LoadController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.LoadRequestDTO;
import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.service.LoadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loads")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
public class LoadController {

    private final LoadService loadService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<LoadResponseDTO> createLoad(@Valid @RequestBody LoadRequestDTO request) {
        log.info("Creating new load");
        return ResponseEntity.status(HttpStatus.CREATED).body(loadService.createLoad(request, getCurrentUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<LoadResponseDTO> updateLoad(
            @PathVariable Long id,
            @Valid @RequestBody LoadRequestDTO request) {
        log.info("Updating load with ID: {}", id);
        return ResponseEntity.ok(loadService.updateLoad(id, request, getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoadResponseDTO> getLoadById(@PathVariable Long id) {
        log.info("Fetching load with ID: {}", id);
        return ResponseEntity.ok(loadService.getLoadById(id));
    }

    @GetMapping("/number/{loadNumber}")
    public ResponseEntity<LoadResponseDTO> getLoadByNumber(@PathVariable String loadNumber) {
        log.info("Fetching load with number: {}", loadNumber);
        return ResponseEntity.ok(loadService.getLoadByNumber(loadNumber));
    }

    @GetMapping
    public ResponseEntity<Page<LoadResponseDTO>> getAllLoads(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all loads");
        return ResponseEntity.ok(loadService.getAllLoads(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<LoadResponseDTO>> searchLoads(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching loads with term: {}", search);
        return ResponseEntity.ok(loadService.searchLoads(search, pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoadResponseDTO>> getLoadsByCustomer(@PathVariable Long customerId) {
        log.info("Fetching loads for customer ID: {}", customerId);
        return ResponseEntity.ok(loadService.getLoadsByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoadResponseDTO>> getLoadsByStatus(@PathVariable String status) {
        log.info("Fetching loads with status: {}", status);
        return ResponseEntity.ok(loadService.getLoadsByStatus(status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteLoad(@PathVariable Long id) {
        log.info("Deleting load with ID: {}", id);
        loadService.deleteLoad(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Smart Merge Endpoints (Elevated Access Only)
    // =============================================

    @PostMapping("/smart-merge")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<LoadResponseDTO> smartMergeTrips(
            @RequestParam Long customerId,
            @RequestParam LocalDateTime plannedDate) {
        log.info("Smart merging trips for customer {} on {}", customerId, plannedDate);
        return ResponseEntity.ok(loadService.smartMergeTrips(customerId, plannedDate, getCurrentUserId()));
    }

    @GetMapping("/merge-candidates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<List<TripSummaryDTO>> findMergeCandidates(
            @RequestParam Long customerId,
            @RequestParam LocalDateTime plannedDate) {
        log.info("Finding merge candidates for customer {} on {}", customerId, plannedDate);
        List<Trip> trips = loadService.findMergeableTrips(customerId, plannedDate);
        List<TripSummaryDTO> summaries = trips.stream()
                .map(trip -> TripSummaryDTO.builder()
                        .id(trip.getId())
                        .tripNumber(trip.getTripNumber())
                        .origin(trip.getOriginCity() != null ? trip.getOriginCity() : trip.getOriginLocation())
                        .destination(trip.getDestinationCity() != null ? trip.getDestinationCity() : trip.getDestinationLocation())
                        .plannedStartDate(trip.getPlannedStartDate())
                        .plannedEndDate(trip.getPlannedEndDate())
                        .status(trip.getStatus())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    @PostMapping("/{loadNumber}/trips")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public ResponseEntity<LoadResponseDTO> addTripsToLoad(
            @PathVariable String loadNumber,
            @RequestBody List<Long> tripIds) {
        log.info("Adding trips to load {}", loadNumber);
        return ResponseEntity.ok(loadService.addTripsToLoad(loadNumber, tripIds, getCurrentUserId()));
    }

    // Helper method to get current user ID
    private Long getCurrentUserId() {
        // This should be implemented based on your authentication context
        // For now, return a default value or get from SecurityContext
        return 1L; // Placeholder
    }
}
