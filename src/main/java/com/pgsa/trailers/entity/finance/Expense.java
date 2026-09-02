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
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_number", unique = true, length = 50)
    private String expenseNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "sub_category", length = 50)
    private String subCategory;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "ZAR";

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "vendor_contact", length = 100)
    private String vendorContact;

    @Column(name = "vendor_tax_number", length = 50)
    private String vendorTaxNumber;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "PENDING";

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "reimbursement_eligible")
    private Boolean reimbursementEligible = false;

    @Column(name = "reimbursement_date")
    private LocalDate reimbursementDate;

    @Column(name = "reimbursement_amount", precision = 19, scale = 2)
    private BigDecimal reimbursementAmount;

    @Column(name = "tax_deductible")
    private Boolean taxDeductible = true;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("15.00");

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "trip_id")
    private Long tripId;

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
        if (approvalStatus == null) approvalStatus = "PENDING";
        if (taxDeductible == null) taxDeductible = true;
        if (reimbursementEligible == null) reimbursementEligible = false;
        if (taxRate == null) taxRate = new BigDecimal("15.00");
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
