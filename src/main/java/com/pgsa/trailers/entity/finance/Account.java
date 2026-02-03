package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account")
public class Account extends BaseEntity {

    // ---------------- FIELDS ----------------
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    private String provider;

    @Column(length = 3)
    private String currency; // e.g., "ZAR", "USD"

    @Column(nullable = false)
    private Boolean active = true;

    // Add this field - it's missing!
    @Column(name = "account_number", unique = true, length = 50)
    private String accountNumber;

    // ---------------- CONSTRUCTORS ----------------

    // Default no-args constructor (needed for JPA)
    public Account() { }

    // Optional constructor for convenience if you need to create with ID
    public Account(Long id) {
        this.setId(id); // uses BaseEntity's ID
    }
}