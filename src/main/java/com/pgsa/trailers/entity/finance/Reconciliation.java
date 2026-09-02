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
@Table(name = "reconciliations")
public class Reconciliation extends BaseEntity {

    // ============================================================
    // CONSTANTS
    // ============================================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ============================================================
    // FIELDS - Matches database columns
    // ============================================================
    
    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "statement_balance", precision = 19, scale = 2)
    private BigDecimal statementBalance;

    @Column(name = "system_balance", precision = 19, scale = 2)
    private BigDecimal systemBalance;

    @Column(name = "variance", precision = 19, scale = 2)
    private BigDecimal variance;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ✅ These fields exist in your database but not in the entity - ADD THEM
    @Column(name = "reconciled")
    private Boolean reconciled = false;

    @Column(name = "reconciled_by")
    private Long reconciledBy;

    @Column(name = "reconciled_date")
    private LocalDateTime reconciledDate;

    @Column(name = "from_date")
    private LocalDateTime fromDate;

    @Column(name = "to_date")
    private LocalDateTime toDate;

    @Column(name = "slips_total", precision = 19, scale = 2)
    private BigDecimal slipsTotal;

    @Column(name = "payments_total", precision = 19, scale = 2)
    private BigDecimal paymentsTotal;

    // ============================================================
    // METHODS THAT DON'T REQUIRE reconciliationNumber
    // ============================================================

    public boolean isCompleted() {
        return "COMPLETED".equals(status) || "BALANCED".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status) || "IN_PROGRESS".equals(status);
    }

    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    public boolean isBalanced() {
        return variance != null && variance.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean hasVariance() {
        return variance != null && variance.compareTo(BigDecimal.ZERO) != 0;
    }

    public BigDecimal getAbsoluteVariance() {
        return variance != null ? variance.abs() : BigDecimal.ZERO;
    }

    public void calculateVariance() {
        if (statementBalance != null && systemBalance != null) {
            this.variance = statementBalance.subtract(systemBalance);
        }
    }

    public boolean isWithinThreshold(BigDecimal threshold) {
        if (variance == null || threshold == null) {
            return false;
        }
        return variance.abs().compareTo(threshold) <= 0;
    }

    public BigDecimal calculateTotalDifference() {
        if (slipsTotal == null && paymentsTotal == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal slips = slipsTotal != null ? slipsTotal : BigDecimal.ZERO;
        BigDecimal payments = paymentsTotal != null ? paymentsTotal : BigDecimal.ZERO;
        return slips.subtract(payments);
    }

    public String getStatusDisplay() {
        if (status == null) return "UNKNOWN";
        return switch (status) {
            case "PENDING" -> "Pending";
            case "IN_PROGRESS" -> "In Progress";
            case "COMPLETED" -> "Completed";
            case "BALANCED" -> "Balanced";
            case "FAILED" -> "Failed";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    // ============================================================
    // LIFECYCLE CALLBACKS
    // ============================================================

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "PENDING";
        }
        if (reconciled == null) {
            reconciled = false;
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
