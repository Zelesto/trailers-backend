// src/main/java/com/pgsa/trailers/dto/inventory/InventoryItemResponseDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponseDTO {
    private Long id;
    private String name;
    private String category;
    private String unitOfMeasure;
    private Boolean isConsumable;
    private BigDecimal reorderLevel;
    private Long locationId;
    private String locationName;
    private Integer quantity;
    private BigDecimal unitCost;
    private Integer minLevel;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
