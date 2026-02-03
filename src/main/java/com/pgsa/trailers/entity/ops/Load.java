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
@Table(name = "load")
public class Load extends BaseEntity {

    @Column(name = "load_number", unique = true, nullable = false)
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

    @OneToMany(mappedBy = "load", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();
}
