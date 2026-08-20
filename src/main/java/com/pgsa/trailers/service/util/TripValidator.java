package com.pgsa.trailers.service.util;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.dto.UpdateTripRequest;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.suppliers.TripValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class TripValidator {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ON_HOLD = "ON_HOLD";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FINALIZED = "FINALIZED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Terminal statuses (cannot be changed)
    private static final Set<String> TERMINAL_STATUSES = Set.of(
        STATUS_COMPLETED, STATUS_FINALIZED, STATUS_CLOSED, STATUS_CANCELLED
    );

    // ✅ FIXED: Use HashMap instead of Map.of() for more than 10 entries
    private static final Map<String, Set<String>> VALID_TRANSITIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(STATUS_DRAFT, Set.of(STATUS_PLANNED, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_PLANNED, Set.of(STATUS_ASSIGNED, STATUS_IN_PROGRESS, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_ASSIGNED, Set.of(STATUS_IN_PROGRESS, STATUS_ON_HOLD, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_IN_PROGRESS, Set.of(STATUS_COMPLETED, STATUS_ON_HOLD, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_ACTIVE, Set.of(STATUS_COMPLETED, STATUS_ON_HOLD, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_ON_HOLD, Set.of(STATUS_IN_PROGRESS, STATUS_ACTIVE, STATUS_COMPLETED, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_PENDING, Set.of(STATUS_PLANNED, STATUS_ASSIGNED, STATUS_CANCELLED));
        VALID_TRANSITIONS.put(STATUS_COMPLETED, Set.of(STATUS_FINALIZED, STATUS_CLOSED));
        VALID_TRANSITIONS.put(STATUS_FINALIZED, Set.of());
        VALID_TRANSITIONS.put(STATUS_CLOSED, Set.of());
        VALID_TRANSITIONS.put(STATUS_CANCELLED, Set.of());
    }

    /**
     * Validates create trip request
     */
    public void validateCreateRequest(CreateTripRequest request) {
        if (request == null) {
            throw new TripValidationException("CreateTripRequest cannot be null");
        }

        if (request.getVehicleId() == null) {
            throw new TripValidationException("Vehicle ID is required for trip creation");
        }
        
        if (request.getCustomerId() == null || request.getCustomerId() <= 0) {
            throw new TripValidationException("Customer is required. Please select a customer.");
        }

        if (request.getOriginLocation() == null || request.getOriginLocation().isBlank()) {
            throw new TripValidationException("Origin location is required");
        }

        if (request.getDestinationLocation() == null || request.getDestinationLocation().isBlank()) {
            throw new TripValidationException("Destination location is required");
        }

        // Validate planned dates
        if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null) {
            if (request.getPlannedEndDate().isBefore(request.getPlannedStartDate())) {
                throw new TripValidationException("Planned end date cannot be before planned start date");
            }
        }

        // Validate planned distance
        if (request.getPlannedDistanceKm() != null && 
            request.getPlannedDistanceKm().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TripValidationException("Planned distance must be greater than zero");
        }

        // Validate planned duration
        if (request.getPlannedDurationHours() != null && 
            request.getPlannedDurationHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TripValidationException("Planned duration must be greater than zero");
        }

        // Validate coordinates if provided
        if (request.getOriginLatitude() != null && 
            (request.getOriginLatitude() < -90 || request.getOriginLatitude() > 90)) {
            throw new TripValidationException("Origin latitude must be between -90 and 90");
        }

        if (request.getOriginLongitude() != null && 
            (request.getOriginLongitude() < -180 || request.getOriginLongitude() > 180)) {
            throw new TripValidationException("Origin longitude must be between -180 and 180");
        }

        if (request.getDestinationLatitude() != null && 
            (request.getDestinationLatitude() < -90 || request.getDestinationLatitude() > 90)) {
            throw new TripValidationException("Destination latitude must be between -90 and 90");
        }

        if (request.getDestinationLongitude() != null && 
            (request.getDestinationLongitude() < -180 || request.getDestinationLongitude() > 180)) {
            throw new TripValidationException("Destination longitude must be between -180 and 180");
        }
    }

    /**
     * Validates if a trip can be started
     */
    public void validateCanStart(Trip trip, BigDecimal actualStartOdometer) {
        if (trip == null) {
            throw new TripValidationException("Trip cannot be null");
        }

        String status = trip.getStatus();
        if (!STATUS_PLANNED.equals(status) && !STATUS_DRAFT.equals(status)) {
            throw new TripValidationException(
                String.format("Cannot start trip with status: %s. Trip must be PLANNED or DRAFT", status)
            );
        }

        if (trip.getActualStartOdometer() != null) {
            throw new TripValidationException("Trip has already been started");
        }

        if (actualStartOdometer == null) {
            throw new TripValidationException("Actual start odometer is required");
        }

        if (actualStartOdometer.compareTo(BigDecimal.ZERO) < 0) {
            throw new TripValidationException("Actual start odometer cannot be negative");
        }
        
        if (trip.getVehicle() == null) {
            throw new TripValidationException("Trip has no assigned vehicle");
        }
    }

    /**
     * Validates if a trip can be ended
     */
    public void validateCanEnd(Trip trip, BigDecimal actualEndOdometer) {
        if (trip == null) {
            throw new TripValidationException("Trip cannot be null");
        }

        String status = trip.getStatus();
        if (!STATUS_IN_PROGRESS.equals(status) && !STATUS_ACTIVE.equals(status)) {
            throw new TripValidationException(
                String.format("Cannot end trip with status: %s. Trip must be IN_PROGRESS or ACTIVE", status)
            );
        }

        if (trip.getActualStartOdometer() == null) {
            throw new TripValidationException("Trip has not been started yet");
        }

        if (actualEndOdometer == null) {
            throw new TripValidationException("Actual end odometer is required");
        }

        if (actualEndOdometer.compareTo(trip.getActualStartOdometer()) < 0) {
            throw new TripValidationException(
                String.format("End odometer (%.2f) cannot be less than start odometer (%.2f)",
                    actualEndOdometer, trip.getActualStartOdometer())
            );
        }
    }

    /**
     * Validates if a trip can be updated
     */
    public void validateCanUpdate(Trip trip) {
        if (trip == null) {
            throw new TripValidationException("Trip cannot be null");
        }

        String status = trip.getStatus();
        if (TERMINAL_STATUSES.contains(status)) {
            throw new TripValidationException(
                String.format("Cannot update trip with terminal status: %s", status)
            );
        }
    }

    /**
     * Validates trip update request
     */
    public void validateUpdateRequest(UpdateTripRequest request, Trip existingTrip) {
        if (request == null) {
            throw new TripValidationException("UpdateTripRequest cannot be null");
        }

        // Validate dates if both provided
        if (request.getPlannedStartDate() != null && request.getPlannedEndDate() != null) {
            if (request.getPlannedEndDate().isBefore(request.getPlannedStartDate())) {
                throw new TripValidationException("Planned end date cannot be before planned start date");
            }
        }

        if (request.getActualStartDate() != null && request.getActualEndDate() != null) {
            if (request.getActualEndDate().isBefore(request.getActualStartDate())) {
                throw new TripValidationException("Actual end date cannot be before actual start date");
            }
        }

        // Validate odometer readings
        if (request.getActualStartOdometer() != null && request.getActualEndOdometer() != null) {
            if (request.getActualEndOdometer().compareTo(request.getActualStartOdometer()) < 0) {
                throw new TripValidationException("End odometer cannot be less than start odometer");
            }
        }

        // Validate status transition if status is being changed
        if (request.getStatus() != null && !request.getStatus().equals(existingTrip.getStatus())) {
            validateStatusTransition(existingTrip.getStatus(), request.getStatus());
        }
    }

    /**
     * Validates trip status transition
     */
    public void validateStatusTransition(String from, String to) {
        if (from == null || to == null) {
            throw new TripValidationException("Status cannot be null");
        }

        if (from.equals(to)) {
            throw new TripValidationException("Trip is already in status: " + from);
        }

        // Check if transition is valid
        Set<String> allowedTransitions = VALID_TRANSITIONS.get(from);
        if (allowedTransitions == null || !allowedTransitions.contains(to)) {
            throw new TripValidationException(
                String.format("Invalid status transition from %s to %s", from, to)
            );
        }
    }

    /**
     * Validates if a trip can be deleted
     */
    public void validateCanDelete(Trip trip) {
        if (trip == null) {
            throw new TripValidationException("Trip cannot be null");
        }

        String status = trip.getStatus();
        if (TERMINAL_STATUSES.contains(status)) {
            throw new TripValidationException(
                String.format("Cannot delete trip with terminal status: %s", status)
            );
        }

        if (STATUS_IN_PROGRESS.equals(status) || STATUS_ACTIVE.equals(status)) {
            throw new TripValidationException("Cannot delete an active trip. Please cancel it first.");
        }
    }

    /**
     * Validates odometer readings sequence
     */
    public void validateOdometerSequence(BigDecimal startOdometer, BigDecimal endOdometer) {
        if (startOdometer != null && endOdometer != null) {
            if (endOdometer.compareTo(startOdometer) < 0) {
                throw new TripValidationException(
                    String.format("End odometer (%.2f) must be greater than start odometer (%.2f)", 
                        endOdometer, startOdometer)
                );
            }
        }
    }

    /**
     * Validates date sequence
     */
    public void validateDateSequence(LocalDateTime startDate, LocalDateTime endDate, String type) {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new TripValidationException(
                    String.format("%s end date cannot be before start date", type)
                );
            }
        }
    }

    /**
     * Validates location coordinates
     */
    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new TripValidationException("Latitude must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new TripValidationException("Longitude must be between -180 and 180");
        }
    }
}
