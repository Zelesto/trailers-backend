package com.pgsa.trailers.entity.finance;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "receivables")
public class Receivable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receivable_number", unique = true, length = 50)
    private String receivableNumber;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_reference", length = 100)
    private String sourceReference;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "driver_name", length = 200)
    private String driverName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "original_amount", precision = 19, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 19, scale = 2)
    private BigDecimal balanceDue;

    @Column(name = "currency", length = 10)
    private String currency = "ZAR";

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "interest_accrued", precision = 19, scale = 2)
    private BigDecimal interestAccrued = BigDecimal.ZERO;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "collection_attempts")
    private Integer collectionAttempts = 0;

    @Column(name = "last_collection_date")
    private LocalDateTime lastCollectionDate;

    @Column(name = "next_followup_date")
    private LocalDate nextFollowupDate;

    @Column(name = "assigned_to")
    private Long assignedTo;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (priority == null) priority = "NORMAL";
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
        if (interestAccrued == null) interestAccrued = BigDecimal.ZERO;
        if (collectionAttempts == null) collectionAttempts = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
