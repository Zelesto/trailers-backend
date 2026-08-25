// src/main/java/com/pgsa/trailers/controller/BatchDistanceController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.BatchDistanceService;
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
public class BatchDistanceController {

    private final BatchDistanceService batchDistanceService;

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
