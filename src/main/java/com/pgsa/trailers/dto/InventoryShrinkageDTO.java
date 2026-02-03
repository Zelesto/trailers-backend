package com.pgsa.trailers.dto;

import java.math.BigDecimal;

/**
 * InventoryShrinkageDTO
 *
 * Represents a summary of inventory shrinkage for reporting.
 * Null-safe to avoid NPEs when data is incomplete.
 */
public record InventoryShrinkageDTO(
        String name,
        BigDecimal expected,
        BigDecimal actual,
        BigDecimal shrinkage
) {

        public InventoryShrinkageDTO(String name,
                                     BigDecimal expected,
                                     BigDecimal actual,
                                     BigDecimal shrinkage) {
                this.name = name != null ? name : "";
                this.expected = expected != null ? expected : BigDecimal.ZERO;
                this.actual = actual != null ? actual : BigDecimal.ZERO;
                this.shrinkage = shrinkage != null ? shrinkage : BigDecimal.ZERO;
        }
}
