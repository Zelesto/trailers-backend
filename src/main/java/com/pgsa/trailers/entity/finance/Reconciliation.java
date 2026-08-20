package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reconciliation")
public class Reconciliation extends BaseEntity {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "statement_balance", precision = 15, scale = 2)
    private BigDecimal statementBalance;

    @Column(name = "system_balance", precision = 15, scale = 2)
    private BigDecimal systemBalance;

    @Column(name = "variance", precision = 15, scale = 2)
    private BigDecimal variance;

    @Column(name = "status")
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Add these fields for the FuelMonthCloseService
    @Column(name = "account_name")
    private String accountName;

    @Column(name = "slips_total", precision = 15, scale = 2)
    private BigDecimal slipsTotal;

    @Column(name = "payments_total", precision = 15, scale = 2)
    private BigDecimal paymentsTotal;

    @Column(name = "from_date")
    private LocalDateTime from;

    @Column(name = "to_date")
    private LocalDateTime to;

    // ========== GETTERS AND SETTERS ==========

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    // ========== HELPER METHODS ==========

    /**
     * Check if the reconciliation is completed
     */
    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    /**
     * Check if the reconciliation is pending
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Check if the reconciliation is in progress
     */
    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Check if the reconciliation failed or was cancelled
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    /**
     * Check if the reconciliation can be started
     */
    public boolean canBeStarted() {
        return STATUS_PENDING.equals(status);
    }

    /**
     * Check if the reconciliation can be completed
     */
    public boolean canBeCompleted() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Check if the reconciliation can be cancelled
     */
    public boolean canBeCancelled() {
        return STATUS_PENDING.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Calculate variance if not already set
     */
    public void calculateVariance() {
        if (statementBalance != null && systemBalance != null) {
            this.variance = statementBalance.subtract(systemBalance);
        }
    }

    /**
     * Check if the reconciliation is balanced (variance is zero)
     */
    public boolean isBalanced() {
        return variance != null && variance.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if the reconciliation has a variance
     */
    public boolean hasVariance() {
        return variance != null && variance.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * Get the variance as a positive or negative value
     */
    public BigDecimal getAbsoluteVariance() {
        return variance != null ? variance.abs() : BigDecimal.ZERO;
    }

    /**
     * Check if the reconciliation is within the allowed variance threshold
     * @param threshold The allowed variance threshold (e.g., 100 for R100)
     */
    public boolean isWithinThreshold(BigDecimal threshold) {
        if (variance == null || threshold == null) {
            return false;
        }
        return variance.abs().compareTo(threshold) <= 0;
    }

    /**
     * Check if the reconciliation has all required data
     */
    public boolean hasCompleteData() {
        return reconciliationDate != null && 
               accountId != null && 
               statementBalance != null && 
               systemBalance != null;
    }

    /**
     * Calculate the total difference between slips and payments
     */
    public BigDecimal calculateTotalDifference() {
        if (slipsTotal == null && paymentsTotal == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal slips = slipsTotal != null ? slipsTotal : BigDecimal.ZERO;
        BigDecimal payments = paymentsTotal != null ? paymentsTotal : BigDecimal.ZERO;
        return slips.subtract(payments);
    }

    /**
     * Get status display name (using the status value itself as display)
     */
    public String getStatusDisplay() {
        if (status == null) {
            return "UNKNOWN";
        }
        
        switch (status) {
            case STATUS_PENDING:
                return "Pending";
            case STATUS_IN_PROGRESS:
                return "In Progress";
            case STATUS_COMPLETED:
                return "Completed";
            case STATUS_FAILED:
                return "Failed";
            case STATUS_CANCELLED:
                return "Cancelled";
            default:
                return status;
        }
    }

    // ========== LIFECYCLE CALLBACKS ==========

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = STATUS_PENDING;
        }
        if (variance == null && statementBalance != null && systemBalance != null) {
            calculateVariance();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (variance == null && statementBalance != null && systemBalance != null) {
            calculateVariance();
        }
    }
}
