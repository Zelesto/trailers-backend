package com.pgsa.trailers.entity.billing;

import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.Trip;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trip_billing", indexes = {
    @Index(name = "idx_trip_billing_trip", columnList = "trip_id"),
    @Index(name = "idx_trip_billing_customer", columnList = "customer_id"),
    @Index(name = "idx_trip_billing_status", columnList = "status")
})
public class TripBilling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_id")
    private Rate rate;

    // ADD THIS - Direct customer ID field
    @Column(name = "customer_id")
    private Long customerId;

    // Keep this relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    // Trip metrics used
    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "tonnage", precision = 10, scale = 2)
    private BigDecimal tonnage;

    @Column(name = "days")
    private Integer days = 1;

    @Column(name = "labour_hours", precision = 10, scale = 2)
    private BigDecimal labourHours;

    @Column(name = "crane_hours", precision = 10, scale = 2)
    private BigDecimal craneHours = BigDecimal.ZERO;

    // Calculated charges
    @Column(name = "base_rate", precision = 15, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "distance_charge", precision = 15, scale = 2)
    private BigDecimal distanceCharge;

    @Column(name = "tonnage_charge", precision = 15, scale = 2)
    private BigDecimal tonnageCharge;

    @Column(name = "daily_rate_charge", precision = 15, scale = 2)
    private BigDecimal dailyRateCharge;

    @Column(name = "labour_charge", precision = 15, scale = 2)
    private BigDecimal labourCharge;

    @Column(name = "crane_charge", precision = 15, scale = 2)
    private BigDecimal craneCharge;

    @Column(name = "fixed_surcharge", precision = 15, scale = 2)
    private BigDecimal fixedSurcharge;

    @Type(JsonType.class)
    @Column(name = "applied_sliding_tier", columnDefinition = "jsonb")
    private Map<String, Object> appliedSlidingTier;

    // Totals
    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat", precision = 15, scale = 2)
    private BigDecimal vat;

    @Column(name = "total", precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("15.00");

    @Column(name = "is_vat_applicable")
    private Boolean isVatApplicable = true;

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
        if (vatRate == null) vatRate = new BigDecimal("15.00");
        if (isVatApplicable == null) isVatApplicable = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper to calculate totals
    public void calculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        if (distanceCharge != null) this.subtotal = this.subtotal.add(distanceCharge);
        if (tonnageCharge != null) this.subtotal = this.subtotal.add(tonnageCharge);
        if (dailyRateCharge != null) this.subtotal = this.subtotal.add(dailyRateCharge);
        if (labourCharge != null) this.subtotal = this.subtotal.add(labourCharge);
        if (craneCharge != null) this.subtotal = this.subtotal.add(craneCharge);
        if (fixedSurcharge != null) this.subtotal = this.subtotal.add(fixedSurcharge);

        if (isVatApplicable && vatRate != null) {
            this.vat = this.subtotal.multiply(vatRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            this.vat = BigDecimal.ZERO;
        }

        this.total = this.subtotal.add(vat).setScale(2, RoundingMode.HALF_UP);
    }
}
