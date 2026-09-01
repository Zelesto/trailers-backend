package com.pgsa.trailers.entity.ops;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "commodities")
public class Commodity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "default_labour_hours_per_unit", precision = 10, scale = 2)
    private BigDecimal defaultLabourHoursPerUnit = new BigDecimal("2.0");

    @Column(name = "hazard_level", length = 20)
    private String hazardLevel = "NONE";

    @Column(name = "requires_special_handling")
    private Boolean requiresSpecialHandling = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (defaultLabourHoursPerUnit == null) defaultLabourHoursPerUnit = new BigDecimal("2.0");
        if (hazardLevel == null) hazardLevel = "NONE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
