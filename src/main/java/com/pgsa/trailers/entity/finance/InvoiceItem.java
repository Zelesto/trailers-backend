package com.pgsa.trailers.entity.finance;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonBackReference
    private Invoice invoice;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "quantity", precision = 19, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 2)
    private BigDecimal unitPrice;

    /** Pre-tax line amount — maps to the real {@code amount} column. */
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;

    /** Pre-tax + tax — maps to the real {@code total_amount} column. */
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    /** Polymorphic reference type: 'TRIP', 'LOAD', 'MANUAL', etc. */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    /** Polymorphic reference id (trip.id when type='TRIP'). */
    @Column(name = "reference_id")
    private Long referenceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =====================================================================
    // Backwards-compatible accessors — no @Column annotation so Hibernate
    // ignores them under field access. Existing code calling
    // setTripId(...) / setLoadId(...) / getTripId() / getLoadId() keeps
    // working; they now write to reference_type / reference_id.
    // =====================================================================

    public Long getTripId() {
        return "TRIP".equals(referenceType) ? referenceId : null;
    }

    public void setTripId(Long tripId) {
        if (tripId != null) {
            this.referenceId = tripId;
            this.referenceType = "TRIP";
        }
    }

    public String getLoadId() {
        return "LOAD".equals(referenceType) && referenceId != null
                ? String.valueOf(referenceId)
                : null;
    }

    public void setLoadId(String loadId) {
        if (loadId != null && !"TRIP".equals(this.referenceType)) {
            this.referenceType = "LOAD";
            // Do not overwrite referenceId — load references live on the Invoice.
        }
    }

    /** Explicit numeric load PK when the caller has it. */
    public void setLoadReferenceId(Long loadReferenceId) {
        if (loadReferenceId != null) {
            this.referenceId = loadReferenceId;
            this.referenceType = "LOAD";
        }
    }
}
