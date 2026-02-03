package com.pgsa.trailers.entity.ops.auto;


import com.pgsa.trailers.dto.RouteCalculationRequestDTO;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.service.TripMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripMetricsEventListener {

    private final TripRepository tripRepository;
    private final TripMetricsService metricsService;

    @EventListener
    @Transactional
    public void onTripPlanned(TripPlannedEvent event) {
        Trip trip = loadTrip(event.getTripId());

        log.info("Calculating estimated metrics for trip {}", trip.getId());

        metricsService.calculateAndSaveMetrics(
                trip.getId(),
                RouteCalculationRequestDTO.fromTrip(trip)
        );

    }

    @EventListener
    @Transactional
    public void onTripCompleted(TripCompletedEvent event) {
        Trip trip = loadTrip(event.getTripId());

        log.info("Finalizing metrics for completed trip {}", trip.getId());

        metricsService.lockFinalMetrics(trip.getId());
    }

    private Trip loadTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }
}

