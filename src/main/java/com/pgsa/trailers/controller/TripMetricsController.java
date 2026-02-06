package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.RouteCalculationRequestDTO;
import com.pgsa.trailers.dto.TripMetricsDTO;
import com.pgsa.trailers.dto.TripMetricsUpdateRequest;
import com.pgsa.trailers.service.TripMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trip-metrics")
@RequiredArgsConstructor
public class TripMetricsController {

    private final TripMetricsService tripMetricsService;

    /**
     * Create trip metrics (calculate and save) without a path variable.
     * POST {{baseUrl}}/api/trip-metrics
     */
    @PostMapping
    public ResponseEntity<TripMetricsDTO> createMetrics(
            @RequestBody RouteCalculationRequestDTO request
    ) {
        if (request.getTripId() == null) {
            return ResponseEntity.badRequest().build();
        }

        TripMetricsDTO metrics =
                tripMetricsService.calculateAndSaveMetrics(
                        request.getTripId(),
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(metrics);
    }


    /**
     * Get trip metrics for a specific trip.
     * GET {{baseUrl}}/api/trip-metrics/{tripId}
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<TripMetricsDTO> getMetrics(@PathVariable Long tripId) {
        TripMetricsDTO metrics = tripMetricsService.getTripMetrics(tripId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Update existing trip metrics.
     * PUT {{baseUrl}}/api/trip-metrics/{tripId}
     */
    @PutMapping("/{tripId}")
    public ResponseEntity<TripMetricsDTO> updateMetrics(
            @PathVariable Long tripId,
            @RequestBody TripMetricsUpdateRequest request
    ) {
        TripMetricsDTO updatedMetrics = tripMetricsService.updateTripMetrics(tripId, request);
        return ResponseEntity.ok(updatedMetrics);
    }
    /**
 * Calculate metrics preview without saving (no tripId needed).
 * POST {{baseUrl}}/api/trip-metrics/preview
 */
@PostMapping("/preview")
public ResponseEntity<TripMetricsDTO> previewMetrics(
        @RequestBody RouteCalculationRequestDTO request
) {
    TripMetricsDTO metrics = tripMetricsService.calculateMetricsOnly(request);
    return ResponseEntity.ok(metrics);
}

    /**
     * Calculate and save trip metrics for a specific trip ID.
     * POST {{baseUrl}}/api/trip-metrics/{tripId}/calculate
     */
    
    @PostMapping("/{tripId}/calculate")
    public ResponseEntity<TripMetricsDTO> calculateMetrics(
            @PathVariable Long tripId,
            @RequestBody RouteCalculationRequestDTO request
    ) {
        TripMetricsDTO metrics =
                tripMetricsService.calculateAndSaveMetrics(tripId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(metrics);
    }

}
