package com.pgsa.trailers.entity.billing;

import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.assets.VehicleType;
import com.pgsa.trailers.entity.ops.Commodity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "rates", indexes = {
    @Index(name = "idx_rates_customer", columnList = "customer_id"),
    @Index(name = "idx_rates_vehicle_type", columnList = "vehicle_type_id"),
    @Index(name = "idx_rates_valid_dates", columnList = "valid_from, valid_to"),
    @Index(name = "idx_rates_priority", columnList = "priority DESC")
})
public class Rate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @Column(name = "destination_pattern")
    private String destinationPattern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commodity_id")
    private Commodity commodity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private RateSchedule schedule;

    @Column(name = "fixed_amount", precision = 15, scale = 2)
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(name = "per_km", precision = 10, scale = 2)
    private BigDecimal perKm;

    @Column(name = "per_ton", precision = 10, scale = 2)
    private BigDecimal perTon;

    @Column(name = "per_day", precision = 15, scale = 2)
    private BigDecimal perDay;

    @Column(name = "labour_hourly_rate", precision = 15, scale = 2)
    private BigDecimal labourHourlyRate;

    @Column(name = "crane_hourly_rate", precision = 15, scale = 2)
    private BigDecimal craneHourlyRate;

    @Type(JsonType.class)
    @Column(name = "sliding_scale", columnDefinition = "jsonb")
    private Map<String, Object> slidingScale;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (fixedAmount == null) fixedAmount = BigDecimal.ZERO;
        if (priority == null) priority = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
