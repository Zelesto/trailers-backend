// src/main/java/com/pgsa/trailers/controller/DistanceController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.LoadService;
import com.pgsa.trailers.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            log.error("❌ Error calculating trip distance: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/load/{loadNumber}")
    public ResponseEntity<Map<String, Object>> calculateLoadDistances(@PathVariable String loadNumber) {
        log.info("📡 Manual trigger: Calculate load distances for: {}", loadNumber);
        
        try {
            loadService.updateLoadDistances(loadNumber);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Load distance calculation triggered for " + loadNumber,
                    "loadNumber", loadNumber
            ));
        } catch (Exception e) {
            log.error("❌ Error calculating load distance: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/recalculate-all-loads")
    public ResponseEntity<Map<String, Object>> recalculateAllLoads() {
        log.info("📡 Manual trigger: Recalculating all load distances");
        
        try {
            loadService.updateAllLoadDistances();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All load distances recalculated successfully"
            ));
        } catch (Exception e) {
            log.error("❌ Failed to recalculate load distances: {}", e.getMessage(), e);
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
            log.error("❌ Error processing pending: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Object>> getPendingCount() {
        try {
            long count = tripService.getPendingDistanceCount();
            return ResponseEntity.ok(Map.of(
                    "count", count
            ));
        } catch (Exception e) {
            log.error("❌ Error getting pending count: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
