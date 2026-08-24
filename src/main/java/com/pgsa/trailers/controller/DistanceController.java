// src/main/java/com/pgsa/trailers/controller/DistanceController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.LoadService;
import com.pgsa.trailers.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/distance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DistanceController {

    private final TripService tripService; 
    private final LoadService loadService;

    @PostMapping("/trip/{tripId}")
    public ResponseEntity<Map<String, Object>> calculateTripDistance(@PathVariable Long tripId) {
        log.info("📡 Manual trigger: Calculate trip distance for ID: {}", tripId);
        
        try {
            tripService.calculateTripDistance(tripId); 
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Distance calculation triggered for trip " + tripId,
                    "tripId", tripId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/load/{loadId}")
    public ResponseEntity<Map<String, Object>> calculateLoadDistances(@PathVariable Long loadId) {
        log.info("📡 Manual trigger: Calculate load distances for ID: {}", loadId);
        
        try {
            loadService.updateLoadDistances(loadId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Load distance calculation triggered for " + loadId,
                    "loadId", loadId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/pending")
    public ResponseEntity<Map<String, Object>> processPending() {
        log.info("📡 Manual trigger: Process pending distance calculations");
        
        try {
            tripService.processPendingDistanceCalculations();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pending distance processing triggered"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        try {
            // You'll need to add this method to TripRepository
            long count = tripService.getPendingDistanceCount();
            return ResponseEntity.ok(Map.of(
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
