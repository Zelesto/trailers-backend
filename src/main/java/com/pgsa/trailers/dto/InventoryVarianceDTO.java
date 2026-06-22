// src/main/java/com/pgsa/trailers/dto/InventoryVarianceDTO.java
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
public class InventoryVarianceDTO {
    private Long itemId;
    private String itemName;
    private BigDecimal expectedQuantity;
    private BigDecimal actualQuantity;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private String reason;
}
