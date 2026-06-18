package com.pgsa.trailers.service.listeners;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripMetricsCalculationListener {

    @Async
    @EventListener
    public void handle(TripMetricsCalculationEvent event) {
        // This is now handled by TripMetricsEventListener.onTripPlanned()
        log.debug("TripMetricsCalculationEvent received for trip {} - metrics will be calculated by TripMetricsEventListener", 
            event.tripId());
        // Do nothing - let TripMetricsEventListener handle it
    }
}
