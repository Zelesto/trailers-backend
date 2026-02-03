package com.pgsa.trailers.entity.finance;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_transaction")
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "direction", nullable = false)
    private String direction; // DEBIT / CREDIT

    @Column(name = "description")
    private String description;

    @Column(name = "reconciled")
    private Boolean reconciled;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;



    // Getters and setters

    // Getter
    public Long getId() {
        return id;
    }

    // ✅ Add setter
    public void setId(Long id) {
        this.id = id;
    }

}
