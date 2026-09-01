package com.pgsa.trailers.entity.finance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_item")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity", precision = 15, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("15.00");

    @Column(name = "line_total", precision = 15, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "load_id", length = 50)
    private String loadId;

    @PrePersist
    @PreUpdate
    protected void calculateTotals() {
        if (quantity != null && unitPrice != null) {
            this.lineTotal = quantity.multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP);
            
            if (taxRate != null) {
                this.taxAmount = this.lineTotal
                    .multiply(taxRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}
