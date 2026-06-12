package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.dto.TripResponse;
import com.pgsa.trailers.dto.UpdateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.CreateTripMapper;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.ops.auto.TripCompletedEvent;
import com.pgsa.trailers.entity.ops.auto.TripPlannedEvent;
import com.pgsa.trailers.entity.ops.auto.TripStartedEvent;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.util.TripNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripMetricsService tripMetricsService;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripFinalisationService tripFinalisationService;
    private final TripNumberGenerator tripNumberGenerator;
    private final CreateTripMapper createTripMapper;
    private final TripResponseMapper tripResponseMapper;
    private final ApplicationEventPublisher eventPublisher;

    /* ========================
       CREATE
       ======================== */
    public TripResponse createTrip(CreateTripRequest request, Long userId) {

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        Driver driver = null;
        if (request.getDriverId() != null) {
            driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        }

        Driver supervisor = null;
        if (request.getSupervisorId() != null) {
            supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new IllegalArgumentException("Supervisor not found"));
        }

        Trip trip = createTripMapper.toEntity(
                request,
                vehicle,
                driver,
                supervisor,
                userId
        );

        trip.setTripNumber(tripNumberGenerator.generate());
        trip.setStatus(TripStatus.PLANNED);
        trip.setLastStatusUpdate(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);

        tripMetricsService.initializeMetrics(saved.getId());

        return tripResponseMapper.toResponse(saved);
    }

    /* ========================
       START TRIP
       ======================== */
    @Transactional
    public TripResponse startTrip(Long tripId, BigDecimal actualStartOdometer, Long userId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Trip must be PLANNED to start");
        }

        trip.setActualStartOdometer(actualStartOdometer);
        trip.setActualStartDate(LocalDateTime.now());
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        Trip updated = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStartedEvent(tripId));

        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       END TRIP
       ======================== */
    @Transactional
    public TripResponse endTrip(Long tripId, BigDecimal actualEndOdometer, Long userId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new IllegalStateException("Trip must be IN_PROGRESS to end");
        }

        BigDecimal startOdo = trip.getActualStartOdometer();

        if (startOdo == null) {
            throw new IllegalStateException("Start odometer missing");
        }

        if (actualEndOdometer.compareTo(startOdo) < 0) {
            throw new IllegalArgumentException("End odometer cannot be less than start");
        }

        trip.setActualEndOdometer(actualEndOdometer);
        trip.setActualEndDate(LocalDateTime.now());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        trip.setActualDistanceKm(actualEndOdometer.subtract(startOdo));

        Trip updated = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripCompletedEvent(tripId));

        return tripResponseMapper.toResponse(updated);
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
       STATUS UPDATE (CLEAN)
       ======================== */
    @Transactional
    public TripResponse updateTripStatus(Long tripId, TripStatus newStatus) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        trip.setStatus(newStatus);
        trip.setLastStatusUpdate(LocalDateTime.now());

        Trip saved = tripRepository.save(trip);

        switch (newStatus) {
            case PLANNED -> eventPublisher.publishEvent(new TripPlannedEvent(tripId));
            case IN_PROGRESS -> eventPublisher.publishEvent(new TripStartedEvent(tripId));
            case COMPLETED -> eventPublisher.publishEvent(new TripCompletedEvent(tripId));
        }

        return tripResponseMapper.toResponse(saved);
    }

    /* ========================
       INCIDENT RULE
       ======================== */
    public boolean canReportIncident(Trip trip) {
        return trip.getStatus() == TripStatus.IN_PROGRESS
                || trip.getStatus() == TripStatus.ACTIVE
                || trip.getStatus() == TripStatus.PLANNED;
    }

    /* ========================
       DELETE
       ======================== */
    @Transactional
    public void deleteTrip(Long id) {

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        tripRepository.delete(trip);
    }

    /* ========================
       UPDATE (PATCH SAFE)
       ======================== */
    @Transactional
    public TripResponse updateTrip(Long tripId, UpdateTripRequest request, Long userId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found"));

        LocalDateTime now = LocalDateTime.now();

        if (request.getVehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));
            trip.setVehicle(vehicle);
        }

        if (request.getDriverId() != null) {
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
            trip.setDriver(driver);
        }

        if (request.getSupervisorId() != null) {
            Driver supervisor = driverRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new IllegalArgumentException("Supervisor not found"));
            trip.setSupervisor(supervisor);
        }

        Optional.ofNullable(request.getOriginLocation()).ifPresent(trip::setOriginLocation);
        Optional.ofNullable(request.getDestinationLocation()).ifPresent(trip::setDestinationLocation);

        Optional.ofNullable(request.getActualStartDate()).ifPresent(trip::setActualStartDate);
        Optional.ofNullable(request.getActualEndDate()).ifPresent(trip::setActualEndDate);

        Optional.ofNullable(request.getActualStartOdometer()).ifPresent(trip::setActualStartOdometer);
        Optional.ofNullable(request.getActualEndOdometer()).ifPresent(trip::setActualEndOdometer);
        Optional.ofNullable(request.getActualDistanceKm()).ifPresent(trip::setActualDistanceKm);

        Optional.ofNullable(request.getPlannedStartDate()).ifPresent(trip::setPlannedStartDate);
        Optional.ofNullable(request.getPlannedEndDate()).ifPresent(trip::setPlannedEndDate);
        Optional.ofNullable(request.getPlannedDistanceKm()).ifPresent(trip::setPlannedDistanceKm);
        Optional.ofNullable(request.getEstimatedDurationHours()).ifPresent(trip::setEstimatedDurationHours);

        Optional.ofNullable(request.getTollCost()).ifPresent(trip::setTollCost);
        Optional.ofNullable(request.getOtherExpenses()).ifPresent(trip::setOtherExpenses);

        if (request.getStatus() != null && request.getStatus() != trip.getStatus()) {
            trip.setStatus(request.getStatus());
            trip.setLastStatusUpdate(now);
        }

        trip.setUpdatedAt(now);
        trip.setUpdatedBy(userId);

        Trip saved = tripRepository.save(trip);

        return tripResponseMapper.toResponse(saved);
    }
}
