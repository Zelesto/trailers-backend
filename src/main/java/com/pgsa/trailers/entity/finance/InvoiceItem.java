package com.pgsa.trailers.entity.finance;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps onto the real {@code invoice_item} schema:
 *
 * <pre>
 * id              bigint
 * invoice_id      bigint
 * description     varchar
 * quantity        numeric
 * unit_price      numeric
 * amount          numeric   ← pre-VAT line total
 * tax_rate        numeric
 * tax_amount      numeric
 * total_amount    numeric   ← pre-VAT + VAT
 * reference_id    bigint    ← polymorphic (trip.id when type='TRIP')
 * reference_type  varchar   ← 'TRIP' | 'LOAD' | ...
 * created_at      timestamp
 * </pre>
 *
 * <b>Do not add {@code line_total}, {@code load_id}, or {@code trip_id} fields.</b>
 * Those columns do not exist in the DB and adding them causes
 * {@code column "line_total" does not exist} SQL errors at insert time.
 */
@Entity
@Table(name = "invoice_item")
@Access(AccessType.FIELD)   // ← force Hibernate to use fields only, ignore methods
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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

    /** Pre-VAT amount — maps to the real {@code amount} column. */
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;

    /** Pre-VAT + VAT — maps to the real {@code total_amount} column. */
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    /** 'TRIP', 'LOAD', or another type tag. */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    /** Numeric id matching {@link #referenceType}. */
    @Column(name = "reference_id")
    private Long referenceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =====================================================================
    // Backwards-compatible accessors.
    //
    // These are @Transient so Hibernate will NEVER try to map them as
    // columns, regardless of access type. They exist so existing code that
    // calls setTripId(...) / setLoadId(...) / getTripId() / getLoadId()
    // still compiles — internally they write to reference_type / reference_id.
    // =====================================================================

    @Transient
    public Long getTripId() {
        return "TRIP".equals(referenceType) ? referenceId : null;
    }

    @Transient
    public void setTripId(Long tripId) {
        if (tripId != null) {
            this.referenceId = tripId;
            this.referenceType = "TRIP";
        }
    }

    @Transient
    public String getLoadId() {
        return "LOAD".equals(referenceType) && referenceId != null
                ? String.valueOf(referenceId)
                : null;
    }

    @Transient
    public void setLoadId(String loadId) {
        if (loadId != null && !"TRIP".equals(this.referenceType)) {
            this.referenceType = "LOAD";
            // Do not overwrite referenceId — load references live on the Invoice row.
        }
    }
}
