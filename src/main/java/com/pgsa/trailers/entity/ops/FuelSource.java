package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fuel_source")
public class FuelSource extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "account_id")
    private Long accountId;

    // Remove or comment out this circular reference to avoid issues
    // @OneToMany(mappedBy = "fuelSource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<FuelSlip> fuelSlips;

    // Remove @Transient fields since they're in BaseEntity
    // @Transient
    // private String createdBy;
    //
    // @Transient
    // private String updatedBy;
}