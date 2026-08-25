// src/main/java/com/pgsa/trailers/controller/DistanceController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.BatchDistanceService;
import com.pgsa.trailers.service.LoadService;
import com.pgsa.trailers.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*; 
import org.springframework.http.ResponseEntity;
import java.util.Map; 

    
@RestController
@RequestMapping("/api/distance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DistanceController {

    private final TripService tripService;
    private final LoadService loadService;
    private final BatchDistanceService batchDistanceService;

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

    @PostMapping("/load/{loadNumber}")
    public ResponseEntity<Map<String, Object>> calculateLoadDistances(@PathVariable String loadNumber) {
        log.info("📡 Manual trigger: Calculate load distances for ID: {}", loadNumber);
        
        try {
            loadService.updateLoadDistances(loadNumber);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Load distance calculation triggered for " + loadNumber,
                    "loadNumber", loadNumber
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }


    @PostMapping("/load/{loadNumber}")
    public ResponseEntity<Map<String, Object>> recalculateLoad(@PathVariable String loadNumber) {
        log.info("📡 Manual trigger: Recalculating load distances for: {}", loadNumber);
        
        try {
            loadService.updateLoadDistances(loadNumber);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Load distances recalculated for " + loadNumber
            ));
        } catch (Exception e) {
            log.error("❌ Failed to recalculate load {}: {}", loadNumber, e.getMessage(), e);
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
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/recalculate-all")
    public ResponseEntity<Map<String, Object>> recalculateAllDistances() {
        log.info("📡 Manual trigger: Recalculating all trip distances");
        
        try {
            var result = batchDistanceService.recalculateAllTripDistances();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Batch distance recalculation started",
                    "jobId", result.join().getJobId()
            ));
        } catch (Exception e) {
            log.error("❌ Failed to start batch recalculation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/progress/{jobId}")
    public ResponseEntity<Map<String, Object>> getProgress(@PathVariable String jobId) {
        var progress = batchDistanceService.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
                "jobId", progress.getJobId(),
                "totalTrips", progress.getTotalTrips(),
                "processed", progress.getProcessed(),
                "succeeded", progress.getSucceeded(),
                "failed", progress.getFailed(),
                "completed", progress.isCompleted(),
                "percentage", progress.getProgressPercentage(),
                "message", progress.getMessage()
        ));
    }
}
