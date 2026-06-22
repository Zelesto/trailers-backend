// src/main/java/com/pgsa/trailers/dto/inventory/InventoryItemRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemRequestDTO {
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
}
