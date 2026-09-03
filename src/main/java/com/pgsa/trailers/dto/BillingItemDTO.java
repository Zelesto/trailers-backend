// src/main/java/com/pgsa/trailers/dto/BillingItemDTO.java
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
public class BillingItemDTO {
    private Long id;
    private Long inventoryItemId;
    private String itemName;
    private String itemCategory;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
}
