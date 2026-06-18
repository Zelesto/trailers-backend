package com.pgsa.trailers.service.listeners;

import com.pgsa.trailers.dto.RouteCalculationRequestDTO;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.auto.TripMetricsCalculationEvent;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.service.TripMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripMetricsCalculationListener {

    private final TripRepository tripRepository;
    private final TripMetricsService tripMetricsService;

    @Async
    @EventListener
    public void handle(TripMetricsCalculationEvent event) {

        try {

            Trip trip = tripRepository.findById(event.tripId())
                    .orElseThrow();

            RouteCalculationRequestDTO request =
                    new RouteCalculationRequestDTO();

            request.setOriginLocation(
                    trip.getOriginLocation()
            );

            request.setDestinationLocation(
                    trip.getDestinationLocation()
            );

            if (trip.getVehicle() != null &&
                trip.getVehicle().getVehicleType() != null) {

                request.setVehicleType(
                        trip.getVehicle()
                                .getVehicleType()
                                .name()
                );
            }

            tripMetricsService.calculateAndSaveMetrics(
                    trip.getId(),
                    request
            );

            log.info(
                    "Route metrics calculated for trip {}",
                    trip.getId()
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to calculate metrics for trip {}",
                    event.tripId(),
                    ex
            );
        }
    }
}
