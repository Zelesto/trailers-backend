package com.pgsa.trailers.dto;

import java.math.BigDecimal;

/**
 * InventoryVarianceDTO
 *
 * Represents the variance of an inventory item between system quantity and counted quantity.
 * Null-safe for reporting purposes.
 */
public record InventoryVarianceDTO(
        String itemName,
        BigDecimal systemQty,
        BigDecimal countedQty,
        BigDecimal variance
) {

        public InventoryVarianceDTO(String itemName,
                                    BigDecimal systemQty,
                                    BigDecimal countedQty,
                                    BigDecimal variance) {
                this.itemName = itemName != null ? itemName : "";
                this.systemQty = systemQty != null ? systemQty : BigDecimal.ZERO;
                this.countedQty = countedQty != null ? countedQty : BigDecimal.ZERO;
                this.variance = variance != null ? variance : BigDecimal.ZERO;
        }
}