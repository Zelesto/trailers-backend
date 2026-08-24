// src/main/java/com/pgsa/trailers/entity/inventory/InventoryItem.java
package com.pgsa.trailers.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

     @Column(name = "sku", unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "is_consumable")
    private Boolean isConsumable;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "min_level")
    private Integer minLevel;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_driver_issuable")
    private Boolean isDriverIssuable;

    @Column(name = "is_vehicle_issuable")
    private Boolean isVehicleIssuable;

    @Column(name = "hold_code")
    private String holdCode;

    @Column(name = "hold_reason")
    private String holdReason;

    @Column(name = "hold_date")
    private LocalDateTime holdDate;

    @Column(name = "held_by")
    private String heldBy;

    @Column(name = "return_by_date")
    private LocalDate returnByDate;

    // IMPORTANT: Use Boolean object type, not primitive boolean
    @Column(name = "is_held")
    private Boolean isHeld;

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

    // ====== EXPLICIT GETTERS AND SETTERS ======
    // This ensures the methods exist even if Lombok fails

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

    public Boolean getIsConsumable() { return isConsumable; }
    public void setIsConsumable(Boolean isConsumable) { this.isConsumable = isConsumable; }

    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public Integer getMinLevel() { return minLevel; }
    public void setMinLevel(Integer minLevel) { this.minLevel = minLevel; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getIsDriverIssuable() { return isDriverIssuable; }
    public void setIsDriverIssuable(Boolean isDriverIssuable) { this.isDriverIssuable = isDriverIssuable; }

    public Boolean getIsVehicleIssuable() { return isVehicleIssuable; }
    public void setIsVehicleIssuable(Boolean isVehicleIssuable) { this.isVehicleIssuable = isVehicleIssuable; }

    public String getHoldCode() { return holdCode; }
    public void setHoldCode(String holdCode) { this.holdCode = holdCode; }

    public String getHoldReason() { return holdReason; }
    public void setHoldReason(String holdReason) { this.holdReason = holdReason; }

    public LocalDateTime getHoldDate() { return holdDate; }
    public void setHoldDate(LocalDateTime holdDate) { this.holdDate = holdDate; }

    public String getHeldBy() { return heldBy; }
    public void setHeldBy(String heldBy) { this.heldBy = heldBy; }

    public LocalDate getReturnByDate() { return returnByDate; }
    public void setReturnByDate(LocalDate returnByDate) { this.returnByDate = returnByDate; }

    // IMPORTANT: Getter for isHeld - MUST be named getIsHeld() for Jackson/Lombok
    public Boolean getIsHeld() { return isHeld; }
    public void setIsHeld(Boolean isHeld) { this.isHeld = isHeld; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
