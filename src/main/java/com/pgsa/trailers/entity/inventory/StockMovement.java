package com.pgsa.trailers.entity.inventory;

import com.pgsa.trailers.enums.StockMovementType;
import com.pgsa.trailers.enums.StockCountStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------- RELATIONSHIPS ----------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    // ---------------- MOVEMENT INFO ----------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType movementType;  // e.g., ADJUSTMENT

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false)
    private LocalDateTime movement_date = LocalDateTime.now();

    /**
     * Generic reference to source document, e.g., STOCK_COUNT, PURCHASE_ORDER
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockCountStatus status = StockCountStatus.DRAFT;

    /**
     * ID of the source document
     */
    @Column(name = "reference_id")
    private Long referenceId;

    // ---------------- CONSTRUCTORS ----------------

    public StockMovement() { }

    public StockMovement(InventoryItem item, InventoryLocation location,
                         StockMovementType movementType, BigDecimal quantity,
                         String referenceType, Long referenceId) {
        this.item = item;
        this.location = location;
        this.movementType = movementType;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.movement_date = LocalDateTime.now();
        this.status = StockCountStatus.DRAFT;
    }

    // ---------------- GETTERS & SETTERS ----------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem item) { this.item = item; }

    public InventoryLocation getLocation() { return location; }
    public void setLocation(InventoryLocation location) { this.location = location; }

    public StockMovementType getMovementType() { return movementType; }
    public void setMovementType(StockMovementType movementType) { this.movementType = movementType; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity != null ? quantity : BigDecimal.ZERO; }

    public LocalDateTime getDate() { return movement_date; }
    public void setDate(LocalDateTime date) { this.movement_date = date; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public StockCountStatus getStatus() { return status; }
    public void setStatus(StockCountStatus status) { this.status = status; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
}
