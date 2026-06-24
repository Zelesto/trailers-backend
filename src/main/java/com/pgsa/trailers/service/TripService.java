package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.dto.TripResponse;
import com.pgsa.trailers.dto.UpdateTripRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.CreateTripMapper;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.ops.auto.TripCompletedEvent;
import com.pgsa.trailers.entity.ops.auto.TripPlannedEvent;
import com.pgsa.trailers.entity.ops.auto.TripStartedEvent;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.entity.suppliers.TripValidationException;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.util.TripNumberGenerator;
import com.pgsa.trailers.service.util.TripValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripMetricsService tripMetricsService;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CustomerRepository customerRepository;
    private final LoadRepository loadRepository;
    private final TripNumberGenerator tripNumberGenerator;
    private final CreateTripMapper createTripMapper;
    private final TripResponseMapper tripResponseMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TripValidator tripValidator;

    /* ========================
       CREATE
       ======================== */
    @Transactional
    public TripResponse createTrip(CreateTripRequest request, Long userId) {

        log.debug("Creating trip for vehicle: {}, user: {}", request.getVehicleId(), userId);

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

        // Validate customer if provided
        Customer customer = null;
        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new TripValidationException(
                            "Customer not found with ID: " + request.getCustomerId()));
        }

        Trip trip = createTripMapper.toEntity(request);

        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSupervisor(supervisor);
        
        // Set customer
        if (customer != null) {
            trip.setCustomerId(customer.getId());
        }

        // Handle load ID if provided - FIXED: loadId is now Long
        if (request.getLoadId() != null && request.getLoadId() > 0) {
            // Fetch the load entity to set on the trip
            Load load = loadRepository.findById(request.getLoadId())
                    .orElseThrow(() -> new TripValidationException(
                            "Load not found with ID: " + request.getLoadId()));
            trip.setLoad(load);
            trip.setLoadId(load.getId());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus());
        }

        trip.setTripNumber(tripNumberGenerator.generate());
        trip.setStatus(request.getStatus() != null ? request.getStatus() : TripStatus.DRAFT);
        trip.setCreatedBy(userId);
        trip.setLastStatusUpdate(LocalDateTime.now());

        // Save trip first
        Trip saved = tripRepository.save(trip);

        log.info("Created trip with ID: {}, Number: {}, Customer: {}",
                saved.getId(),
                saved.getTripNumber(),
                customer != null ? customer.getName() : "None"
        );

        // Create initial metrics record
        tripMetricsService.initializeMetrics(saved.getId());

        if (saved.getStatus() == TripStatus.PLANNED) {
            eventPublisher.publishEvent(new TripPlannedEvent(saved.getId()));
        }

        return tripResponseMapper.toResponse(saved);
    }

    /* ========================
       START TRIP
       ======================== */
    @Transactional
    public TripResponse startTrip(Long tripId, BigDecimal actualStartOdometer, Long userId) {
        log.debug("Starting trip ID: {} with odometer: {}", tripId, actualStartOdometer);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateCanStart(trip, actualStartOdometer);

        trip.setActualStartOdometer(actualStartOdometer);
        trip.setActualStartDate(LocalDateTime.now());
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);

        Trip updated = tripRepository.save(trip);
        
        eventPublisher.publishEvent(new TripStartedEvent(tripId));
        log.info("Trip {} started", tripId);

        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       END TRIP
       ======================== */
    @Transactional
    public TripResponse endTrip(Long tripId, BigDecimal actualEndOdometer, Long userId) {
        log.debug("Ending trip ID: {} with odometer: {}", tripId, actualEndOdometer);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateCanEnd(trip, actualEndOdometer);

        BigDecimal startOdo = trip.getActualStartOdometer();
        
        trip.setActualEndOdometer(actualEndOdometer);
        trip.setActualEndDate(LocalDateTime.now());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        trip.setActualDistanceKm(actualEndOdometer.subtract(startOdo));
        
        if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
            long hours = java.time.Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toHours();
            trip.setActualDurationHours(BigDecimal.valueOf(hours));
        }

        Trip updated = tripRepository.save(trip);
        
        eventPublisher.publishEvent(new TripCompletedEvent(tripId));
        log.info("Trip {} completed. Distance: {} km", tripId, trip.getActualDistanceKm());

        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       READ
       ======================== */
    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id) {
        Trip trip = findTripOrThrow(id);
        return tripResponseMapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> listTrips(Pageable pageable) {
        return tripRepository.findAll(pageable)
                .map(tripResponseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new TripValidationException("Customer not found with ID: " + customerId);
        }
        return tripRepository.findByCustomerId(customerId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(tripResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    // FIXED: Changed parameter type from String to Long
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByLoad(Long loadId) {
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

    /* ========================
       STATUS UPDATE
       ======================== */
    @Transactional
    public TripResponse updateTripStatus(Long tripId, TripStatus newStatus, Long userId) {
        log.debug("Updating trip {} status to: {}", tripId, newStatus);
        
        Trip trip = findTripOrThrow(tripId);
        
        tripValidator.validateStatusTransition(trip.getStatus(), newStatus);
        
        TripStatus oldStatus = trip.getStatus();
        trip.setStatus(newStatus);
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        if (newStatus == TripStatus.CANCELLED) {
            trip.setCancelledAt(LocalDateTime.now());
        }
        
        if (newStatus == TripStatus.COMPLETED && oldStatus != TripStatus.COMPLETED) {
            trip.calculateActualDistance();
            if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
                long hours = java.time.Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toHours();
                trip.setActualDurationHours(BigDecimal.valueOf(hours));
            }
        }

        Trip saved = tripRepository.save(trip);

        switch (newStatus) {
            case PLANNED -> eventPublisher.publishEvent(new TripPlannedEvent(tripId));
            case IN_PROGRESS -> eventPublisher.publishEvent(new TripStartedEvent(tripId));
            case COMPLETED -> eventPublisher.publishEvent(new TripCompletedEvent(tripId));
            default -> log.debug("Status changed from {} to {}, no event published", oldStatus, newStatus);
        }
        
        log.info("Trip {} status changed from {} to {}", tripId, oldStatus, newStatus);

        return tripResponseMapper.toResponse(saved);
    }

    /* ========================
       CUSTOMER & LOAD MANAGEMENT
       ======================== */
    
    /**
     * Assign customer to a trip
     */
    @Transactional
    public TripResponse assignCustomerToTrip(Long tripId, Long customerId, Long userId) {
        log.debug("Assigning customer {} to trip {}", customerId, tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + customerId));
            trip.setCustomerId(customer.getId());
        } else {
            trip.setCustomerId(null);
        }
        
        trip.setUpdatedAt(LocalDateTime.now());
        trip.setUpdatedBy(userId);
        
        Trip updated = tripRepository.save(trip);
        log.info("Customer assigned to trip {}: {}", tripId, customerId);
        
        return tripResponseMapper.toResponse(updated);
    }

    /**
     * Assign load to a trip - FIXED: loadId is now Long
     */
    @Transactional
    public TripResponse assignLoadToTrip(Long tripId, Long loadId, Long userId) {
        log.debug("Assigning load {} to trip {}", loadId, tripId);
        
        Trip trip = findTripOrThrow(tripId);
        
        if (loadId != null && loadId > 0) {
            // Validate load exists by ID
            Load load = loadRepository.findById(loadId)
                    .orElseThrow(() -> new TripValidationException("Load not found with ID: " + loadId));
            trip.setLoad(load);
            trip.setLoadId(load.getId());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus());
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

    /**
     * Update trip with customer and load information in one call - FIXED: loadId is now Long
     */
    @Transactional
    public TripResponse updateTripCustomerAndLoad(Long tripId, Long customerId, Long loadId, Long userId) {
        log.debug("Updating trip {} with customer: {} and load: {}", tripId, customerId, loadId);
        
        Trip trip = findTripOrThrow(tripId);
        
        // Update customer
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + customerId));
            trip.setCustomerId(customer.getId());
        } else {
            trip.setCustomerId(null);
        }
        
        // Update load
        if (loadId != null && loadId > 0) {
            Load load = loadRepository.findById(loadId)
                    .orElseThrow(() -> new TripValidationException("Load not found with ID: " + loadId));
            trip.setLoad(load);
            trip.setLoadId(load.getId());
            trip.setLoadNumber(load.getLoadNumber());
            trip.setLoadType(load.getCommodityType());
            trip.setLoadDescription(load.getDescription());
            trip.setLoadStatus(load.getStatus());
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
        log.info("Trip {} updated with customer: {} and load: {}", tripId, customerId, loadId);
        
        return tripResponseMapper.toResponse(updated);
    }

    /* ========================
       INCIDENT RULE
       ======================== */
    public boolean canReportIncident(Trip trip) {
        return trip.isActive();
    }

    /* ========================
       DELETE
       ======================== */
    @Transactional
    public void deleteTrip(Long id) {
        log.debug("Deleting trip ID: {}", id);
        
        Trip trip = findTripOrThrow(id);
        
        if (trip.getStatus().isTerminal()) {
            throw new TripValidationException("Cannot delete trip with terminal status: " + trip.getStatus());
        }
        
        if (trip.getMetrics() != null) {
            trip.setMetrics(null);
        }
        
        tripRepository.delete(trip);
        log.info("Deleted trip ID: {}", id);
    }

    /* ========================
       UPDATE (PATCH SAFE)
       ======================== */
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

        // Update customer if provided
        if (request.getCustomerId() != null && request.getCustomerId() > 0) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new TripValidationException("Customer not found with ID: " + request.getCustomerId()));
            trip.setCustomerId(customer.getId());
        }

        // Update load if provided - FIXED: loadId is now Long
        if (request.getLoadId() != null) {
            if (request.getLoadId() > 0) {
                Load load = loadRepository.findById(request.getLoadId())
                        .orElseThrow(() -> new TripValidationException("Load not found with ID: " + request.getLoadId()));
                trip.setLoad(load);
                trip.setLoadId(load.getId());
                trip.setLoadNumber(load.getLoadNumber());
                trip.setLoadType(load.getCommodityType());
                trip.setLoadDescription(load.getDescription());
                trip.setLoadStatus(load.getStatus());
            } else {
                trip.setLoad(null);
                trip.setLoadId(null);
                trip.setLoadNumber(null);
                trip.setLoadType(null);
                trip.setLoadDescription(null);
                trip.setLoadStatus(null);
            }
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

        if (request.getStatus() != null && request.getStatus() != trip.getStatus()) {
            tripValidator.validateStatusTransition(trip.getStatus(), request.getStatus());
            trip.setStatus(request.getStatus());
            trip.setLastStatusUpdate(now);
            
            if (request.getStatus() == TripStatus.CANCELLED) {
                trip.setCancelledAt(now);
            }
        }

        trip.setUpdatedAt(now);
        trip.setUpdatedBy(userId);
        
        trip.updateOriginLocationFromComponents();
        trip.updateDestinationLocationFromComponents();

        Trip saved = tripRepository.save(trip);
        log.info("Updated trip ID: {}", tripId);

        return tripResponseMapper.toResponse(saved);
    }
    
    /* ========================
       PRIVATE HELPERS
       ======================== */
    private Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new TripValidationException("Trip not found with ID: " + id));
    }
}
