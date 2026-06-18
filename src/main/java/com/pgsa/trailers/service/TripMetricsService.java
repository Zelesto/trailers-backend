package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.RouteCalculationRequestDTO;
import com.pgsa.trailers.dto.TripMetricsDTO;
import com.pgsa.trailers.dto.TripMetricsUpdateRequest;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.entity.ResourceNotFoundException;
import com.pgsa.trailers.repository.TripMetricsRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pgsa.trailers.service.routing.RoutingResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripMetricsService {

    private static final BigDecimal FUEL_PRICE_PER_LITER = new BigDecimal("23.50");
    private static final BigDecimal DEFAULT_FUEL_RATE = new BigDecimal("35"); // L per 100km
    private static final BigDecimal DEFAULT_AVG_SPEED = new BigDecimal("60"); // km/h

    private final TripMetricsRepository tripMetricsRepository;
    private final TripRepository tripRepository;
    private final RoutingService routingService;

    /* =========================================================
       AUTO CALCULATION (routing-based)
       ========================================================= */
    @Transactional
    public TripMetricsDTO calculateAndSaveMetrics(
            Long tripId,
            RouteCalculationRequestDTO request
    ) {
        Trip trip = getTrip(tripId);
        TripMetrics metrics = getOrCreateMetrics(trip);

        RoutingResult routing = routingService.calculateRoute(
                request.getOriginLocation(),
                request.getDestinationLocation(),
                request.getVehicleType()
        );

        // =========================
        // UPDATE TRIP (PLANNED)
        // =========================
        trip.setPlannedDistanceKm(routing.getDistanceKm());
        trip.setPlannedDurationHours(routing.getDurationHours());

        tripRepository.save(trip);

        // =========================
        // UPDATE METRICS
        // =========================
        metrics.setTotalDistanceKm(routing.getDistanceKm());
        metrics.setTotalDurationHours(routing.getDurationHours());

        applyFuelAndCost(metrics, routing.getDistanceKm(), request.getVehicleType());
        applyDerivedMetrics(metrics);

        TripMetrics saved = tripMetricsRepository.save(metrics);

        log.info("AUTO planned metrics calculated for trip {}", tripId);

        return TripMetricsDTO.fromEntity(saved);
    }

    /* =========================================================
       MANUAL ENTRY (UI-driven)
       ========================================================= */
    @Transactional
    public TripMetricsDTO updateTripMetrics(Long tripId, TripMetricsUpdateRequest request) {
        Trip trip = getTrip(tripId);
        TripMetrics metrics = getOrCreateMetrics(trip);

        if (request.getTotalDistanceKm() != null) {
            metrics.setTotalDistanceKm(request.getTotalDistanceKm());
        }

        if (request.getTotalDurationHours() != null) {
            metrics.setTotalDurationHours(request.getTotalDurationHours());
        }

        if (request.getDelays() != null) {
            metrics.setIdleTimeHours(request.getDelays());
        }

        if (request.getTasksCompleted() != null) {
            metrics.setTasksCompleted(request.getTasksCompleted());
        }

        if (request.getRevenueAmount() != null) {
            metrics.setRevenueAmount(request.getRevenueAmount());
        }

        // Fuel/cost derived from ACTUAL distance
        if (metrics.getTotalDistanceKm() != null) {
            String vehicleType = trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null
                    ? trip.getVehicle().getVehicleType().name()
                    : null;
            applyFuelAndCost(metrics, metrics.getTotalDistanceKm(), vehicleType);
        }

        applyDerivedMetrics(metrics);

        TripMetrics saved = tripMetricsRepository.save(metrics);

        log.info("MANUAL metrics updated for trip {}", tripId);

        return TripMetricsDTO.fromEntity(saved);
    }

    @Transactional
public void updateTripMetricsFromActualOdometer(
        Long tripId,
        BigDecimal startOdo,
        BigDecimal endOdo
) {
    Trip trip = getTrip(tripId);
    TripMetrics metrics = getOrCreateMetrics(trip);

    if (startOdo != null && endOdo != null) {

        if (endOdo.compareTo(startOdo) < 0) {
            throw new IllegalArgumentException(
                    "End odometer cannot be less than start odometer"
            );
        }

        BigDecimal distance = endOdo.subtract(startOdo);

        metrics.setTotalDistanceKm(distance);

        trip.setActualDistanceKm(distance);
        tripRepository.save(trip);

        String vehicleType =
                trip.getVehicle() != null &&
                trip.getVehicle().getVehicleType() != null
                        ? trip.getVehicle().getVehicleType().name()
                        : null;

        applyFuelAndCost(metrics, distance, vehicleType);
        applyDerivedMetrics(metrics);
    }

    tripMetricsRepository.save(metrics);
    log.debug("Updated metrics from odometer for trip {}", tripId);
}

    /* =========================================================
       READ
       ========================================================= */
    @Transactional(readOnly = true)
public TripMetricsDTO getTripMetrics(Long tripId) {

    Trip trip = getTrip(tripId);

    TripMetrics metrics = tripMetricsRepository
            .findByTripId(tripId)
            .orElseGet(() -> {
                TripMetrics m = new TripMetrics();
                m.setTrip(trip);
                m.setIncidentCount(0);
                m.setTasksCompleted(0);
                return m;
            });

    return TripMetricsDTO.fromEntity(metrics);
}

    /* =========================================================
       PREVIEW ONLY (NO SAVE)
       ========================================================= */
    @Transactional(readOnly = true)
    public TripMetricsDTO calculateMetricsOnly(RouteCalculationRequestDTO request) {
        RoutingResult routing = routingService.calculateRoute(
                request.getOriginLocation(),
                request.getDestinationLocation(),
                request.getVehicleType()
        );

        BigDecimal fuelUsed = estimateFuel(routing.getDistanceKm(), request.getVehicleType());

        TripMetricsDTO dto = new TripMetricsDTO();
        dto.setOriginLocation(request.getOriginLocation());
        dto.setDestinationLocation(request.getDestinationLocation());
        dto.setVehicleType(request.getVehicleType());
        dto.setTotalDistanceKm(routing.getDistanceKm());
        dto.setTotalDurationHours(routing.getDurationHours());
        dto.setFuelUsedLiters(fuelUsed);
        dto.setCostAmount(fuelUsed.multiply(FUEL_PRICE_PER_LITER));

        if (routing.getDurationHours() != null && routing.getDurationHours().compareTo(BigDecimal.ZERO) > 0) {
            dto.setAverageSpeedKmh(
                    routing.getDistanceKm()
                            .divide(routing.getDurationHours(), 2, RoundingMode.HALF_UP)
            );
        } else {
            dto.setAverageSpeedKmh(BigDecimal.ZERO);
        }

        if (fuelUsed != null && fuelUsed.compareTo(BigDecimal.ZERO) > 0) {
            dto.setFuelEfficiencyKmPerLiter(
                    routing.getDistanceKm()
                            .divide(fuelUsed, 2, RoundingMode.HALF_UP)
            );
        } else {
            dto.setFuelEfficiencyKmPerLiter(BigDecimal.ZERO);
        }

        return dto;
    }

    /* =========================================================
       INITIALIZE
       ========================================================= */
    @Transactional
    public void initializeMetrics(Long tripId) {
        if (tripMetricsRepository.existsByTripId(tripId)) {
            log.debug("Metrics already exist for trip {}", tripId);
            return;
        }
        
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        TripMetrics metrics = new TripMetrics();
        metrics.setTrip(trip);
        metrics.setIncidentCount(0);
        metrics.setTasksCompleted(0);
        
        tripMetricsRepository.save(metrics);
        log.info("Initialized metrics for trip {}", tripId);
    }


    /* =========================================================
   CALCULATE FROM SAVED TRIP (SAFE)
   ========================================================= */
@Transactional
public void calculateMetricsForTrip(Long tripId) {

    Trip trip = getTrip(tripId);

    if (trip.getOriginLocation() == null ||
        trip.getOriginLocation().isBlank() ||
        trip.getDestinationLocation() == null ||
        trip.getDestinationLocation().isBlank()) {

        log.warn(
                "Skipping metrics calculation for trip {} because locations are missing",
                tripId
        );
        return;
    }

    try {

        RouteCalculationRequestDTO request =
                new RouteCalculationRequestDTO();

        request.setOriginLocation(trip.getOriginLocation());
        request.setDestinationLocation(trip.getDestinationLocation());

        if (trip.getVehicle() != null &&
            trip.getVehicle().getVehicleType() != null) {

            request.setVehicleType(
                    trip.getVehicle()
                            .getVehicleType()
                            .name()
            );
        }

        calculateAndSaveMetrics(tripId, request);

        log.info(
                "Successfully calculated metrics for trip {}",
                tripId
        );

    } catch (Exception e) {

        log.warn(
                "Route calculation failed for trip {}. Trip remains valid.",
                tripId,
                e
        );
    }
}
    /* =========================================================
       FINALIZATION
       ========================================================= */
    @Transactional
    public void finalizeMetrics(Long tripId) {
        Trip trip = getTrip(tripId);
        TripMetrics metrics = getOrCreateMetrics(trip);
        
        // Calculate final metrics based on actual trip data
        if (trip.getActualDistanceKm() != null) {
            metrics.setTotalDistanceKm(trip.getActualDistanceKm());
        }
        
        if (trip.getActualDurationHours() != null) {
            metrics.setTotalDurationHours(trip.getActualDurationHours());
        }
        
        // Recalculate derived metrics
        String vehicleType = trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null
                ? trip.getVehicle().getVehicleType().name()
                : null;
        
        if (metrics.getTotalDistanceKm() != null) {
            applyFuelAndCost(metrics, metrics.getTotalDistanceKm(), vehicleType);
        }
        
        applyDerivedMetrics(metrics);
        
        // Calculate variances
        if (trip.getPlannedDistanceKm() != null && metrics.getTotalDistanceKm() != null) {
            metrics.setPlannedVsActualDistanceVarianceKm(
                    metrics.getTotalDistanceKm().subtract(trip.getPlannedDistanceKm())
            );
        }
        
        if (trip.getPlannedDurationHours() != null && metrics.getTotalDurationHours() != null) {
            metrics.setPlannedVsActualDurationVarianceHours(
                    metrics.getTotalDurationHours().subtract(trip.getPlannedDurationHours())
            );
        }
        
        metrics.setFinalized(true);
        tripMetricsRepository.save(metrics);
        
        log.info("Finalized metrics for trip {}", tripId);
    }

    @Transactional
    public void lockFinalMetrics(Long tripId) {
        TripMetrics metrics = tripMetricsRepository.findByTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TripMetrics", "tripId", tripId));

        if (!metrics.isFinalized()) {
            finalizeMetrics(tripId);
            metrics = tripMetricsRepository.findByTripId(tripId).orElseThrow();
            metrics.setFinalized(true);
            tripMetricsRepository.save(metrics);
            log.info("Locked final metrics for trip {}", tripId);
        }
    }

    /* =========================================================
       HELPER METHODS
       ========================================================= */
    private Trip getTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
    }

    private TripMetrics getOrCreateMetrics(Trip trip) {
        return tripMetricsRepository.findByTripId(trip.getId())
                .orElseGet(() -> {
                    TripMetrics m = new TripMetrics();
                    m.setTrip(trip);
                    m.setIncidentCount(0);
                    m.setTasksCompleted(0);
                    return m;
                });
    }

    private void applyFuelAndCost(TripMetrics metrics, BigDecimal distance, String vehicleType) {
        BigDecimal fuel = estimateFuel(distance, vehicleType);
        metrics.setFuelUsedLiters(fuel);
        metrics.setCostAmount(fuel.multiply(FUEL_PRICE_PER_LITER));
    }

    private BigDecimal estimateFuel(BigDecimal distance, String vehicleType) {

    if (distance == null ||
        distance.compareTo(BigDecimal.ZERO) <= 0) {
        return BigDecimal.ZERO;
    }

    BigDecimal ratePer100km = DEFAULT_FUEL_RATE;

    if (vehicleType != null) {
        ratePer100km = switch (vehicleType.toUpperCase()) {
            case "VAN" -> new BigDecimal("12");
            case "CAR", "SUV" -> new BigDecimal("8");
            case "TRAILER", "SEMI" -> new BigDecimal("40");
            case "TRUCK" -> new BigDecimal("35");
            default -> DEFAULT_FUEL_RATE;
        };
    }

    return distance.multiply(ratePer100km)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
}

    private void applyDerivedMetrics(TripMetrics metrics) {
        // Calculate average speed
        if (metrics.getTotalDurationHours() != null &&
                metrics.getTotalDurationHours().compareTo(BigDecimal.ZERO) > 0 &&
                metrics.getTotalDistanceKm() != null &&
                metrics.getTotalDistanceKm().compareTo(BigDecimal.ZERO) > 0) {

            metrics.setAverageSpeedKmh(
                    metrics.getTotalDistanceKm()
                            .divide(metrics.getTotalDurationHours(), 2, RoundingMode.HALF_UP)
            );
        } else {
            metrics.setAverageSpeedKmh(BigDecimal.ZERO);
        }
        
        // Calculate fuel efficiency if applicable
        if (metrics.getFuelUsedLiters() != null && 
            metrics.getFuelUsedLiters().compareTo(BigDecimal.ZERO) > 0 &&
            metrics.getTotalDistanceKm() != null &&
            metrics.getTotalDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            
            // This would be km per liter - add to TripMetrics entity if needed
        }
    }
}
