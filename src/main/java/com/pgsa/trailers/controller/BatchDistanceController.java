// src/main/java/com/pgsa/trailers/controller/BatchDistanceController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.BatchDistanceService;
import com.pgsa.trailers.service.BatchProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            // Generate a job ID immediately
            String jobId = UUID.randomUUID().toString();
            
            // ✅ Start the job asynchronously - don't wait
            batchDistanceService.recalculateAllTripDistancesAsync(jobId);
            
            // Return immediately with the job ID
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Batch distance recalculation started",
                    "jobId", jobId
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
        BatchProgress progress = batchDistanceService.getProgress(jobId);
        if (progress == null) {
            return ResponseEntity.ok(Map.of(
                    "jobId", jobId,
                    "totalTrips", 0,
                    "processed", 0,
                    "succeeded", 0,
                    "failed", 0,
                    "completed", false,
                    "percentage", 0,
                    "message", "Job not found"
            ));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", progress.getJobId());
        response.put("totalTrips", progress.getTotalTrips());
        response.put("processed", progress.getProcessed());
        response.put("succeeded", progress.getSucceeded());
        response.put("failed", progress.getFailed());
        response.put("completed", progress.isCompleted());
        response.put("percentage", progress.getProgressPercentage());
        response.put("formattedProgress", progress.getFormattedProgress());
        response.put("durationSeconds", progress.getDurationSeconds());
        response.put("formattedDuration", progress.getFormattedDuration());
        response.put("statusSummary", progress.getStatusSummary());
        response.put("message", progress.getMessage());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/progress/all")
    public ResponseEntity<List<BatchProgress>> getAllProgress() {
        return ResponseEntity.ok(batchDistanceService.getAllProgress());
    }
}
