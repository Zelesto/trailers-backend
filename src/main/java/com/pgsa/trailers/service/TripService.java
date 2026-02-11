package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.dto.TripResponse;
import com.pgsa.trailers.dto.UpdateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.CreateTripMapper;
import com.pgsa.trailers.entity.ops.*;
import com.pgsa.trailers.repository.*;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.ops.auto.TripCompletedEvent;
import com.pgsa.trailers.entity.ops.auto.TripPlannedEvent;
import com.pgsa.trailers.entity.ops.auto.TripStartedEvent;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.util.SecurityUtil;
import com.pgsa.trailers.service.util.TripNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripMetricsRepository metricsRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripFinalisationService tripFinalisationService;
    private final TripMetricsService tripMetricsService;
    private final TripNumberGenerator tripNumberGenerator;
    private final CreateTripMapper createTripMapper;
    private final TripResponseMapper tripResponseMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtil securityUtil;

    /* ========================
       CREATE
       ======================== */
    public TripResponse createTrip(CreateTripRequest request, Long userId) {
        Trip trip = createTripMapper.toEntity(request);

        // Set vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
        trip.setVehicle(vehicle);

        // Set driver if present
        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            trip.setDriver(driver);
        }

        trip.setTripNumber(tripNumberGenerator.generate());
        trip.setStatus(TripStatus.PLANNED);

        // Audit info
        trip.setCreatedAt(LocalDateTime.now());
        trip.setCreatedBy(userId);
        trip.setLastStatusUpdate(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);

        // Initialize metrics
        tripMetricsService.initializeMetrics(saved.getId());

        return tripResponseMapper.toResponse(saved);
    }

    @Transactional
    public TripResponse startTrip(Long tripId, BigDecimal actualStartOdometer, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Trip must be PLANNED to start");
        }

        if (trip.getActualStartOdometer() != null) {
            throw new IllegalStateException("Trip has already been started");
        }

        trip.setActualStartOdometer(actualStartOdometer);
        trip.setActualStartDate(LocalDateTime.now());
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        Trip updated = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStartedEvent(tripId));
        log.info("Trip {} started with odo {}", tripId, actualStartOdometer);

        return tripResponseMapper.toResponse(updated);
    }

    @Transactional
    public TripResponse endTrip(Long tripId, BigDecimal actualEndOdometer, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Trip must be IN_PROGRESS to end");
        }

        BigDecimal startOdo = trip.getActualStartOdometer();
        if (startOdo == null) {
            throw new IllegalStateException("Trip start odo missing");
        }

        if (actualEndOdometer.compareTo(startOdo) < 0) {
            throw new IllegalArgumentException("End odometer cannot be less than start odometer");
        }

        trip.setActualEndOdometer(actualEndOdometer);
        trip.setActualEndDate(LocalDateTime.now());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        // Calculate actual distance
        trip.setActualDistanceKm(actualEndOdometer.subtract(startOdo));

        Trip updated = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripCompletedEvent(tripId));
        log.info("Trip {} completed with odo {} -> {}", tripId, startOdo, actualEndOdometer);

        return tripResponseMapper.toResponse(updated);
    }

    /**
     * Determines if incidents can be reported for a trip
     * Allows incidents for trips that are active, in progress, on hold, or delayed
     */
    public boolean canReportIncident(Trip trip) {
        return trip.getStatus() == TripStatus.IN_PROGRESS || 
               trip.getStatus() == TripStatus.ACTIVE ||
               trip.getStatus() == TripStatus.ON_HOLD ||
               trip.getStatus() == TripStatus.DELAYED;
    }

    /* ========================
       READ
       ======================== */
    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));
        return tripResponseMapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(Pageable pageable) {
        return tripRepository.findAll(pageable)
                .map(tripResponseMapper::toResponse);
    }

    /* ========================
       STATUS
       ======================== */
    @Transactional
    public TripResponse updateStatus(Long tripId, String newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        TripStatus status = TripStatus.valueOf(newStatus.toUpperCase());
        trip.setStatus(status);

        if (status == TripStatus.COMPLETED) {
            tripFinalisationService.finalizeTrip(trip.getId());
        }

        return tripResponseMapper.toResponse(tripRepository.save(trip));
    }

    @Transactional
    public void updateTripStatus(Long tripId, TripStatus newStatus) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        // assign enum directly
        trip.setStatus(newStatus);
        tripRepository.save(trip);

        // publish events
        switch (newStatus) {
            case PLANNED -> eventPublisher.publishEvent(new TripPlannedEvent(tripId));
            case IN_PROGRESS -> eventPublisher.publishEvent(new TripStartedEvent(tripId));
            case COMPLETED -> eventPublisher.publishEvent(new TripCompletedEvent(tripId));
        }
    }

   /* ========================
   DELETE
   ======================== */
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        // Access metrics to ensure JPA deletes it
        if (trip.getMetrics() != null) {
            log.debug("Deleting associated TripMetrics for trip id: {}", id);
            trip.setMetrics(null); // optional if orphanRemoval = true
        }

        tripRepository.delete(trip);
    }

    @Transactional
    public TripResponse updateTrip(Long tripId, UpdateTripRequest request, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Trip not found with ID: " + tripId
                ));

        // Vehicle
        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            trip.setVehicle(vehicle);
        }

        // Driver
        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            trip.setDriver(driver);
        }

        // Route
        Optional.ofNullable(request.getOriginLocation())
                .ifPresent(trip::setOriginLocation);

        Optional.ofNullable(request.getDestinationLocation())
                .ifPresent(trip::setDestinationLocation);

        // Execution dates
        Optional.ofNullable(request.getActualStartDate())
                .ifPresent(trip::setActualStartDate);

        Optional.ofNullable(request.getActualEndDate())
                .ifPresent(trip::setActualEndDate);

        // Status
        if (request.getStatus() != null &&
                !request.getStatus().equals(trip.getStatus())) {

            trip.setStatus(request.getStatus());
            trip.setLastStatusUpdate(LocalDateTime.now());
        }

        // Audit
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        Trip updated = tripRepository.save(trip);

        return tripResponseMapper.toResponse(updated);
    }
}
