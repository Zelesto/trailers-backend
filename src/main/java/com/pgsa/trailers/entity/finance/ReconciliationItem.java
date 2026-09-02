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
@Table(name = "reconciliation_items")
public class ReconciliationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reconciliation_id")
    private Long reconciliationId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "statement_line_date")
    private LocalDate statementLineDate;

    @Column(name = "statement_description", columnDefinition = "TEXT")
    private String statementDescription;

    @Column(name = "statement_amount", precision = 19, scale = 2)
    private BigDecimal statementAmount;

    @Column(name = "matched")
    private Boolean matched = false;

    @Column(name = "match_type", length = 20)
    private String matchType;

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
        if (matched == null) matched = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
