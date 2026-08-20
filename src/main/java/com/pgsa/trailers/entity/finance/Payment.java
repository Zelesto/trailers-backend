package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_PARTIALLY_PAID = "PARTIALLY_PAID";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private BigDecimal amount;

    private String reference;

    @Column(nullable = false)
    private String status;

    // ========== GETTERS AND SETTERS ==========

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    // ========== HELPER METHODS ==========

    /**
     * Check if the payment is completed (paid or refunded)
     */
    public boolean isCompleted() {
        return STATUS_PAID.equals(status) || STATUS_REFUNDED.equals(status);
    }

    /**
     * Check if the payment is pending (pending or partially paid)
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status) || STATUS_PARTIALLY_PAID.equals(status);
    }

    /**
     * Check if the payment was successful
     */
    public boolean isSuccessful() {
        return STATUS_PAID.equals(status);
    }

    /**
     * Check if the payment was refunded
     */
    public boolean isRefunded() {
        return STATUS_REFUNDED.equals(status);
    }

    /**
     * Check if the payment failed or was cancelled
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    /**
     * Check if the payment can be refunded
     */
    public boolean canBeRefunded() {
        return STATUS_PAID.equals(status) && amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if the payment can be retried (failed or pending)
     */
    public boolean canBeRetried() {
        return STATUS_FAILED.equals(status) || STATUS_PENDING.equals(status);
    }
}
