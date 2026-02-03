package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reconciliation")
public class Reconciliation extends BaseEntity {

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "statement_balance", precision = 15, scale = 2)
    private BigDecimal statementBalance;

    @Column(name = "system_balance", precision = 15, scale = 2)
    private BigDecimal systemBalance;

    @Column(name = "variance", precision = 15, scale = 2)
    private BigDecimal variance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReconciliationStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Add these fields for the FuelMonthCloseService
    @Column(name = "account_name")
    private String accountName;

    @Column(name = "slips_total", precision = 15, scale = 2)
    private BigDecimal slipsTotal;

    @Column(name = "payments_total", precision = 15, scale = 2)
    private BigDecimal paymentsTotal;

    @Column(name = "from_date")
    private LocalDateTime from;

    @Column(name = "to_date")
    private LocalDateTime to;
}