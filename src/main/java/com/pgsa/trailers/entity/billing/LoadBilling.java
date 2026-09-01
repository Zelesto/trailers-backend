package com.pgsa.trailers.entity.billing;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "load_billing", indexes = {
    @Index(name = "idx_load_billing_load", columnList = "load_id"),
    @Index(name = "idx_load_billing_customer", columnList = "customer_id"),
    @Index(name = "idx_load_billing_status", columnList = "status")
})
public class LoadBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "load_id", nullable = false, length = 50)
    private String loadId;

    @Column(name = "customer_id")
    private Long customerId;

    // Aggregated metrics
    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    @Column(name = "total_distance_km", precision = 10, scale = 2)
    private BigDecimal totalDistanceKm = BigDecimal.ZERO;

    @Column(name = "total_tonnage", precision = 10, scale = 2)
    private BigDecimal totalTonnage = BigDecimal.ZERO;

    @Column(name = "total_labour_hours", precision = 10, scale = 2)
    private BigDecimal totalLabourHours = BigDecimal.ZERO;

    @Column(name = "total_crane_hours", precision = 10, scale = 2)
    private BigDecimal totalCraneHours = BigDecimal.ZERO;

    @Column(name = "total_days")
    private Integer totalDays = 0;

    // Aggregated charges
    @Column(name = "total_distance_charge", precision = 15, scale = 2)
    private BigDecimal totalDistanceCharge = BigDecimal.ZERO;

    @Column(name = "total_tonnage_charge", precision = 15, scale = 2)
    private BigDecimal totalTonnageCharge = BigDecimal.ZERO;

    @Column(name = "total_daily_charge", precision = 15, scale = 2)
    private BigDecimal totalDailyCharge = BigDecimal.ZERO;

    @Column(name = "total_labour_charge", precision = 15, scale = 2)
    private BigDecimal totalLabourCharge = BigDecimal.ZERO;

    @Column(name = "total_crane_charge", precision = 15, scale = 2)
    private BigDecimal totalCraneCharge = BigDecimal.ZERO;

    @Column(name = "total_fixed_surcharge", precision = 15, scale = 2)
    private BigDecimal totalFixedSurcharge = BigDecimal.ZERO;

    // Totals
    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "vat", precision = 15, scale = 2)
    private BigDecimal vat = BigDecimal.ZERO;

    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Status
    @Column(name = "status", length = 50)
    private String status = "DRAFT";

    // Audit
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "calculated_by")
    private Long calculatedBy;

    @Column(name = "invoiced_at")
    private LocalDateTime invoicedAt;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
        if (totalTrips == null) totalTrips = 0;
        if (totalDistanceKm == null) totalDistanceKm = BigDecimal.ZERO;
        if (totalTonnage == null) totalTonnage = BigDecimal.ZERO;
        if (totalLabourHours == null) totalLabourHours = BigDecimal.ZERO;
        if (totalCraneHours == null) totalCraneHours = BigDecimal.ZERO;
        if (totalDays == null) totalDays = 0;
        if (totalDistanceCharge == null) totalDistanceCharge = BigDecimal.ZERO;
        if (totalTonnageCharge == null) totalTonnageCharge = BigDecimal.ZERO;
        if (totalDailyCharge == null) totalDailyCharge = BigDecimal.ZERO;
        if (totalLabourCharge == null) totalLabourCharge = BigDecimal.ZERO;
        if (totalCraneCharge == null) totalCraneCharge = BigDecimal.ZERO;
        if (totalFixedSurcharge == null) totalFixedSurcharge = BigDecimal.ZERO;
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (vat == null) vat = BigDecimal.ZERO;
        if (total == null) total = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void calculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        if (totalDistanceCharge != null) this.subtotal = this.subtotal.add(totalDistanceCharge);
        if (totalTonnageCharge != null) this.subtotal = this.subtotal.add(totalTonnageCharge);
        if (totalDailyCharge != null) this.subtotal = this.subtotal.add(totalDailyCharge);
        if (totalLabourCharge != null) this.subtotal = this.subtotal.add(totalLabourCharge);
        if (totalCraneCharge != null) this.subtotal = this.subtotal.add(totalCraneCharge);
        if (totalFixedSurcharge != null) this.subtotal = this.subtotal.add(totalFixedSurcharge);

        this.vat = this.subtotal.multiply(new BigDecimal("0.15"))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        this.total = this.subtotal.add(this.vat);
    }
}
