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
@Table(name = "driver_ious")
public class DriverIou {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iou_number", unique = true, length = 50)
    private String iouNumber;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "driver_name", length = 200)
    private String driverName;

    @Column(name = "driver_code", length = 50)
    private String driverCode;

    @Column(name = "iou_type", length = 50)
    private String iouType;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency = "ZAR";

    @Column(name = "iou_date")
    private LocalDate iouDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "authorization_by")
    private Long authorizationBy;

    @Column(name = "authorization_date")
    private LocalDateTime authorizationDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "deducted_from_salary")
    private Boolean deductedFromSalary = false;

    @Column(name = "deduction_date")
    private LocalDate deductionDate;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 19, scale = 2)
    private BigDecimal balanceDue;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "receivable_id")
    private Long receivableId;

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
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
        if (deductedFromSalary == null) deductedFromSalary = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
