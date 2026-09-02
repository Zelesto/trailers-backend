package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "account")  // ✅ FIXED: singular "account"
public class Account extends BaseEntity {

    // ---------------- FIELDS ----------------
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    private String provider;

    @Column(length = 3)
    private String currency = "ZAR";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "account_number", unique = true, length = 50)
    private String accountNumber;

    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "account_sub_type", length = 50)
    private String accountSubType;

    @Column(name = "provider_contact", length = 100)
    private String providerContact;

    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "opening_balance_date")
    private java.time.LocalDate openingBalanceDate;

    @Column(name = "last_reconciliation_date")
    private java.time.LocalDateTime lastReconciliationDate;

    @Column(name = "next_reconciliation_date")
    private java.time.LocalDateTime nextReconciliationDate;

    @Column(name = "reconciliation_frequency", length = 20)
    private String reconciliationFrequency;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---------------- CONSTRUCTORS ----------------

    public Account() { }

    public Account(Long id) {
        this.setId(id);
    }

    // ---------------- HELPER METHODS ----------------

    public boolean isActive() {
        return active != null && active;
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        if (balance == null || amount == null) {
            return false;
        }
        return balance.compareTo(amount) >= 0;
    }

    public boolean hasCreditAvailable() {
        if (creditLimit == null || balance == null) {
            return false;
        }
        return balance.add(creditLimit).compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getAvailableBalance() {
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        if (creditLimit == null) {
            return balance;
        }
        return balance.add(creditLimit);
    }

    public String getDisplayName() {
        return name + " (" + accountNumber + ")";
    }

    public boolean isBankAccount() {
        return "BANK".equals(type) || "ASSET".equals(type);
    }

    public boolean isFuelAccount() {
        return "FUEL".equals(type);
    }

    public boolean isLiability() {
        return "LIABILITY".equals(type);
    }

    public boolean isAsset() {
        return "ASSET".equals(type);
    }

    public String getFormattedBalance() {
        return currency + " " + (balance != null ? balance.toString() : "0.00");
    }
}
