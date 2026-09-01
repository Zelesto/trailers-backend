package com.pgsa.trailers.entity.finance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "invoice_type", length = 50)
    private String invoiceType = "RECEIVABLE";

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    @Column(name = "customer_address", columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "currency", length = 10)
    private String currency = "ZAR";

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", precision = 15, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("15.00");

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "status", length = 50)
    private String status = "DRAFT";

    @Column(name = "source", length = 50)
    private String source = "MANUAL"; // MANUAL, BILLING

    @Column(name = "reference_id", length = 50)
    private String referenceId; // loadId or tripId

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
        if (invoiceType == null) invoiceType = "RECEIVABLE";
        if (currency == null) currency = "ZAR";
        if (source == null) source = "MANUAL";
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (taxTotal == null) taxTotal = BigDecimal.ZERO;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (vatRate == null) vatRate = new BigDecimal("15.00");
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.subtotal = items.stream()
            .map(InvoiceItem::getLineTotal)
            .filter(total -> total != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.taxTotal = items.stream()
            .map(InvoiceItem::getTaxAmount)
            .filter(tax -> tax != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = this.subtotal.add(this.taxTotal);
    }

    public BigDecimal getBalance() {
        return this.totalAmount.subtract(
            this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO
        );
    }

    public boolean isPaid() {
        return "PAID".equals(status) || 
               (totalAmount != null && paidAmount != null && 
                paidAmount.compareTo(totalAmount) >= 0);
    }

    public boolean isOverdue() {
        return "OVERDUE".equals(status) || 
               (dueDate != null && 
                dueDate.isBefore(LocalDateTime.now()) && 
                !isPaid());
    }
}
