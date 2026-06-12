package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "load", indexes = {
        @Index(name = "idx_load_load_number", columnList = "load_number", unique = true),
        @Index(name = "idx_load_status", columnList = "status"),
        @Index(name = "idx_load_loading_date", columnList = "loading_date")
})
public class Load extends BaseEntity {

    @Column(name = "load_number", unique = true, nullable = false, length = 50)
    private String loadNumber;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_cubic_m", precision = 10, scale = 2)
    private BigDecimal volumeCubicM;

    @Column(name = "loading_date")
    private LocalDateTime loadingDate;

    @Column(name = "unloading_date")
    private LocalDateTime unloadingDate;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "commodity_type", length = 100)
    private String commodityType;

    @Column(name = "pallet_count")
    private Integer palletCount;

    @Column(name = "container_number", length = 50)
    private String containerNumber;

    @Column(name = "hazardous_material")
    private Boolean hazardousMaterial = false;

    @Column(name = "special_handling", columnDefinition = "TEXT")
    private String specialHandling;

    @OneToMany(mappedBy = "load", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    // Helper methods
    public void addTrip(Trip trip) {
        trips.add(trip);
        trip.setLoad(this);
    }

    public void removeTrip(Trip trip) {
        trips.remove(trip);
        trip.setLoad(null);
    }

    public boolean isEmpty() {
        return trips == null || trips.isEmpty();
    }

    public BigDecimal getTotalWeight() {
        if (weightKg != null && trips != null) {
            return weightKg;
        }
        return BigDecimal.ZERO;
    }
}
