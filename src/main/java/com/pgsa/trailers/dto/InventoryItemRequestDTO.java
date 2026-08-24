// src/main/java/com/pgsa/trailers/dto/inventory/InventoryItemRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemRequestDTO {
    private String sku;
    private String name;
    private String category;
    private String unitOfMeasure;
    private Boolean isConsumable;
    private BigDecimal reorderLevel;
    private Long locationId;
    private Integer quantity;
    private BigDecimal unitCost;
    private Integer minLevel;
    private String notes;
    
    // New fields
    private Boolean isActive;
    private Boolean isDriverIssuable;
    private Boolean isVehicleIssuable;
    private LocalDate returnByDate;
    private Boolean isHeld;
    private String holdCode;
    private LocalDate holdDate;
    private String holdReason;
    private String heldBy;
}
