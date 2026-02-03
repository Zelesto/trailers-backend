package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pgsa.trailers.dto.UpdateTripRequest;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;
    private final AppUserRepository appUserRepository;

    /* ========================
       CREATE
       ======================== */
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

    /* ========================
       READ
       ======================== */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<TripResponse> getTrip(@PathVariable Long id) {
        log.debug("Fetching trip id: {}", id);
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<Page<TripResponse>> listTrips(Pageable pageable) {
        log.debug("Listing trips with pageable: {}", pageable);
        return ResponseEntity.ok(tripService.listTrips(pageable));
    }

    /* ========================
       UPDATE STATUS
       ======================== */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<TripResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        log.debug("Updating status for trip {} to {}", id, status);
        return ResponseEntity.ok(tripService.updateStatus(id, status));
    }

    /* ========================
       UPDATE
       ======================== */
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

    /* ========================
   START TRIP (ODO START)
   ======================== */
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

    /* ========================
   END TRIP (ODO END)
   ======================== */
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

    /* ========================
       DELETE
       ======================== */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Long id) {
        log.debug("Deleting trip id: {}", id);
        tripService.deleteTrip(id);
    }

    /* ========================
       HELPER METHODS
       ======================== */
    private AppUser getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();
        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
