package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.RouteCalculationRequestDTO;
import com.pgsa.trailers.dto.TripMetricsDTO;
import com.pgsa.trailers.dto.TripMetricsUpdateRequest;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import com.pgsa.trailers.repository.TripMetricsRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripMetricsService {

    private static final BigDecimal FUEL_PRICE_PER_LITER = new BigDecimal("23.50");

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

        RoutingService.RoutingResult routing = routingService.calculateRoute(
                request.getOriginLocation(),
                request.getDestinationLocation(),
                request.getVehicleType()
        );

        // =========================
        // UPDATE TRIP (PLANNED)
        // =========================
        trip.setPlannedDistanceKm(routing.getDistance());
        trip.setPlannedDurationHours(routing.getDuration());

        tripRepository.save(trip);

        // =========================
        // UPDATE METRICS
        // =========================
        metrics.setTotalDistanceKm(routing.getDistance());
        metrics.setTotalDurationHours(routing.getDuration());

        applyFuelAndCost(metrics, routing.getDistance(), request.getVehicleType());
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
            applyFuelAndCost(
                    metrics,
                    metrics.getTotalDistanceKm(),
                    trip.getVehicle() != null
                            ? trip.getVehicle().getVehicleType().name()
                            : null
            );
        }

        applyDerivedMetrics(metrics);

        TripMetrics saved = tripMetricsRepository.save(metrics);

        log.info("MANUAL metrics updated for trip {}", tripId);

        return TripMetricsDTO.fromEntity(saved);
    }

    @Transactional
    public void updateTripMetricsFromActualOdometer(Long tripId, BigDecimal startOdo, BigDecimal endOdo) {
        TripMetrics metrics = tripMetricsRepository.findByTripId(tripId)
                .orElseGet(() -> {
                    Trip trip = new Trip();
                    trip.setId(tripId);
                    TripMetrics m = new TripMetrics();
                    m.setTrip(trip);
                    return m;
                });

        if (startOdo != null && endOdo != null) {
            BigDecimal distance = endOdo.subtract(startOdo);
            metrics.setTotalDistanceKm(distance);

            // Recalculate fuel and derived metrics
            applyFuelAndCost(metrics, distance, null); // vehicle type optional
            applyDerivedMetrics(metrics);
        }

        tripMetricsRepository.save(metrics);
    }


    /* =========================================================
       READ
       ========================================================= */
    @Transactional(readOnly = true)
    public TripMetricsDTO getTripMetrics(Long tripId) {
        return tripMetricsRepository.findByTripId(tripId)
                .map(TripMetricsDTO::fromEntity)
                .orElse(null);
    }

    /* =========================================================
       PREVIEW ONLY (NO SAVE)
       ========================================================= */
    @Transactional(readOnly = true)
    public TripMetricsDTO calculateMetricsOnly(RouteCalculationRequestDTO request) {
        RoutingService.RoutingResult routing = routingService.calculateRoute(
                request.getOriginLocation(),
                request.getDestinationLocation(),
                request.getVehicleType()
        );

        BigDecimal fuelUsed = estimateFuel(routing.getDistance(), request.getVehicleType());

        TripMetricsDTO dto = new TripMetricsDTO();
        dto.setOriginLocation(request.getOriginLocation());
        dto.setDestinationLocation(request.getDestinationLocation());
        dto.setVehicleType(request.getVehicleType());
        dto.setTotalDistanceKm(routing.getDistance());
        dto.setTotalDurationHours(routing.getDuration());
        dto.setFuelUsedLiters(fuelUsed);
        dto.setCostAmount(fuelUsed.multiply(FUEL_PRICE_PER_LITER));

        // Updated setters to match DTO
        if (routing.getDuration().compareTo(BigDecimal.ZERO) > 0) {
            dto.setAverageSpeedKmh(
                    routing.getDistance()
                            .divide(routing.getDuration(), 2, RoundingMode.HALF_UP)
            );
        } else {
            dto.setAverageSpeedKmh(BigDecimal.ZERO);
        }

        if (fuelUsed.compareTo(BigDecimal.ZERO) > 0) {
            dto.setFuelEfficiencyKmPerLiter(
                    routing.getDistance()
                            .divide(fuelUsed, 2, RoundingMode.HALF_UP)
            );
        } else {
            dto.setFuelEfficiencyKmPerLiter(BigDecimal.ZERO);
        }

        return dto;
    }


    @Transactional
    public void initializeMetrics(Long tripId) {
        if (tripMetricsRepository.existsByTripId(tripId)) {
            return;
        }
        Trip trip = tripRepository.getReferenceById(tripId);
        TripMetrics metrics = new TripMetrics();
        metrics.setTrip(trip);
        tripMetricsRepository.save(metrics);
    }


    /* =========================================================
       FINALIZATION
       ========================================================= */
    @Transactional
    public void lockFinalMetrics(Long tripId) {
        TripMetrics metrics = tripMetricsRepository.findByTripId(tripId)
                .orElseThrow(() -> new RuntimeException("Metrics not found"));

        if (!metrics.isFinalized()) {
            metrics.setFinalized(true);
            tripMetricsRepository.save(metrics);
        }
    }

    /* =========================================================
       HELPERS
       ========================================================= */
    private Trip getTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }

    private TripMetrics getOrCreateMetrics(Trip trip) {
        return tripMetricsRepository.findByTripId(trip.getId())
                .orElseGet(() -> {
                    TripMetrics m = new TripMetrics();
                    m.setTrip(trip);
                    return m;
                });
    }

    private void applyFuelAndCost(TripMetrics metrics, BigDecimal distance, String vehicleType) {
        BigDecimal fuel = estimateFuel(distance, vehicleType);
        metrics.setFuelUsedLiters(fuel);
        metrics.setCostAmount(fuel.multiply(FUEL_PRICE_PER_LITER));
    }

    private BigDecimal estimateFuel(BigDecimal distance, String vehicleType) {
        if (distance == null || vehicleType == null) return BigDecimal.ZERO;

        BigDecimal ratePer100km = switch (vehicleType.toUpperCase()) {
            case "VAN" -> new BigDecimal("12");
            case "CAR" -> new BigDecimal("8");
            case "TRAILER" -> new BigDecimal("40");
            default -> new BigDecimal("35"); // TRUCK fallback
        };

        return distance.multiply(ratePer100km)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void applyDerivedMetrics(TripMetrics metrics) {
        if (metrics.getTotalDurationHours() != null &&
                metrics.getTotalDurationHours().compareTo(BigDecimal.ZERO) > 0 &&
                metrics.getTotalDistanceKm() != null) {

            metrics.setAverageSpeedKmh(
                    metrics.getTotalDistanceKm()
                            .divide(metrics.getTotalDurationHours(), 2, RoundingMode.HALF_UP)
            );
        } else {
            metrics.setAverageSpeedKmh(BigDecimal.ZERO);
        }
    }
}
