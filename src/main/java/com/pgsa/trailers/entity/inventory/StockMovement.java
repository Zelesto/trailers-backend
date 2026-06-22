// src/main/java/com/pgsa/trailers/entity/inventory/StockMovement.java
package com.pgsa.trailers.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType; // "IN", "OUT", "ADJUSTMENT"

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "fuel_slip_id")
    private Long fuelSlipId;
}
