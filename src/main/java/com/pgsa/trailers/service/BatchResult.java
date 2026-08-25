// src/main/java/com/pgsa/trailers/service/BatchResult.java
package com.pgsa.trailers.service;

import java.time.LocalDateTime;

/**
 * BatchResult - Result of a batch job
 * 
 * This DTO contains the final results of a batch distance recalculation job,
 * including success/failure counts and load update statistics.
 */
public class BatchResult {
    
    private final String jobId;
    private final int totalTrips;
    private final int succeeded;
    private final int failed;
    private final int loadUpdates;
    private final int loadFailures;
    private final LocalDateTime completedAt;

    public BatchResult(String jobId, int totalTrips, int succeeded, int failed) {
        this(jobId, totalTrips, succeeded, failed, 0, 0);
    }

    public BatchResult(String jobId, int totalTrips, int succeeded, int failed, int loadUpdates, int loadFailures) {
        this.jobId = jobId;
        this.totalTrips = totalTrips;
        this.succeeded = succeeded;
        this.failed = failed;
        this.loadUpdates = loadUpdates;
        this.loadFailures = loadFailures;
        this.completedAt = LocalDateTime.now();
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public String getJobId() {
        return jobId;
    }

    public int getTotalTrips() {
        return totalTrips;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public int getLoadUpdates() {
        return loadUpdates;
    }

    public int getLoadFailures() {
        return loadFailures;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    // ============================================================
    // CALCULATED FIELDS
    // ============================================================

    /**
     * Check if all trips succeeded
     * @return true if no failures
     */
    public boolean isAllSucceeded() {
        return failed == 0 && totalTrips > 0;
    }

    /**
     * Check if the batch had any failures
     * @return true if there were failures
     */
    public boolean hasFailures() {
        return failed > 0 || loadFailures > 0;
    }

    /**
     * Get success rate percentage
     * @return success rate (0-100)
     */
    public double getSuccessRate() {
        return totalTrips > 0 ? (double) succeeded / totalTrips * 100 : 0;
    }

    /**
     * Get load success rate percentage
     * @return load update success rate (0-100)
     */
    public double getLoadSuccessRate() {
        int totalLoads = loadUpdates + loadFailures;
        return totalLoads > 0 ? (double) loadUpdates / totalLoads * 100 : 0;
    }

    /**
     * Get summary message
     * @return summary string
     */
    public String getSummary() {
        return String.format("Batch %s: %d trips processed, %d succeeded, %d failed, %d loads updated, %d load failures",
                jobId, totalTrips, succeeded, failed, loadUpdates, loadFailures);
    }

    /**
     * Get short summary for alerts
     * @return short summary
     */
    public String getShortSummary() {
        if (isAllSucceeded() && loadFailures == 0) {
            return String.format("✅ All %d trips processed successfully!", totalTrips);
        } else if (hasFailures()) {
            return String.format("⚠️ %d succeeded, %d failed out of %d trips", succeeded, failed, totalTrips);
        } else {
            return String.format("✅ %d trips processed", totalTrips);
        }
    }

    @Override
    public String toString() {
        return String.format("BatchResult{jobId='%s', totalTrips=%d, succeeded=%d, failed=%d, loadUpdates=%d, loadFailures=%d}",
                jobId, totalTrips, succeeded, failed, loadUpdates, loadFailures);
    }
}
