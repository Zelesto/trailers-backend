// src/main/java/com/pgsa/trailers/entity/ops/Customer.java
package com.pgsa.trailers.entity.ops;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", unique = true, nullable = false, length = 50)
    private String customerCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;

    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @OneToMany(mappedBy = "customer")
    private List<Trip> trips = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<Load> loads = new ArrayList<>();

    // Helper method to generate customer code
    @PrePersist
    public void prePersist() {
        if (customerCode == null || customerCode.isEmpty()) {
            customerCode = "CUST-" + System.currentTimeMillis();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (paymentTerms == null) {
            paymentTerms = "30 Days";
        }
        if (country == null) {
            country = "South Africa";
        }
    }
}
