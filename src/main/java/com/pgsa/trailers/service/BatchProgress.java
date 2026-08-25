// src/main/java/com/pgsa/trailers/service/BatchProgress.java
package com.pgsa.trailers.service;

import java.time.LocalDateTime;

/**
 * BatchProgress - Tracks the progress of a batch job
 * 
 * This DTO tracks the progress of a batch distance recalculation job,
 * including how many trips have been processed, succeeded, failed,
 * and the overall progress percentage.
 */
public class BatchProgress {
    
    private final String jobId;
    private int totalTrips;
    private int processed;
    private int succeeded;
    private int failed;
    private boolean completed;
    private String message;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    public BatchProgress(String jobId) {
        this.jobId = jobId;
        this.startTime = LocalDateTime.now();
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

    public int getProcessed() {
        return processed;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    // ============================================================
    // SETTERS
    // ============================================================

    public void setTotalTrips(int totalTrips) {
        this.totalTrips = totalTrips;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public void setSucceeded(int succeeded) {
        this.succeeded = succeeded;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.endTime = LocalDateTime.now();
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // ============================================================
    // CALCULATED FIELDS
    // ============================================================

    /**
     * Calculate progress percentage
     * @return progress percentage (0-100)
     */
    public double getProgressPercentage() {
        return totalTrips > 0 ? (double) processed / totalTrips * 100 : 0;
    }

    /**
     * Get duration in seconds
     * @return duration in seconds
     */
    public long getDurationSeconds() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }

    /**
     * Get formatted duration string (e.g., "2h 30m 15s")
     * @return formatted duration
     */
    public String getFormattedDuration() {
        long seconds = getDurationSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Get formatted progress string
     * @return formatted progress (e.g., "45/100 (45.0%)")
     */
    public String getFormattedProgress() {
        return String.format("%d/%d (%.1f%%)", processed, totalTrips, getProgressPercentage());
    }

    /**
     * Get status summary
     * @return status summary
     */
    public String getStatusSummary() {
        if (completed) {
            return String.format("✅ Completed: %d succeeded, %d failed out of %d trips in %s",
                    succeeded, failed, totalTrips, getFormattedDuration());
        } else {
            return String.format("⏳ Processing: %s - %s", getFormattedProgress(), getFormattedDuration());
        }
    }

    @Override
    public String toString() {
        return String.format("BatchProgress{jobId='%s', totalTrips=%d, processed=%d, succeeded=%d, failed=%d, completed=%s, percentage=%.1f%%}",
                jobId, totalTrips, processed, succeeded, failed, completed, getProgressPercentage());
    }
}
