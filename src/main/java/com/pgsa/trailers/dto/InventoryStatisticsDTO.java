// src/main/java/com/pgsa/trailers/dto/inventory/InventoryStatisticsDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatisticsDTO {
    private Long totalItems;
    private Long activeItems;
    private Map<String, Long> categoryCounts;
    private Map<Long, Long> locationCounts;
    private BigDecimal averageReorderLevel;
}
