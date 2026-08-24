// src/main/java/com/pgsa/trailers/config/ScheduledTasks.java
package com.pgsa.trailers.config;

import com.pgsa.trailers.service.TripService;  // ← Change this import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final TripService tripService;  // ← Change from TripDistanceService to TripService

    // Run every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void processPendingDistances() {
        try {
            tripService.processPendingDistanceCalculations();  // ← Use tripService
        } catch (Exception e) {
            log.error("Error in scheduled distance processing: {}", e.getMessage(), e);
        }
    }

    // Run at 2 AM daily for full recalibration
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyDistanceRecalibration() {
        log.info("🔄 Starting daily distance recalibration...");
        try {
            tripService.processPendingDistanceCalculations();
            log.info("✅ Daily distance recalibration complete");
        } catch (Exception e) {
            log.error("❌ Daily distance recalibration failed: {}", e.getMessage(), e);
        }
    }
}
