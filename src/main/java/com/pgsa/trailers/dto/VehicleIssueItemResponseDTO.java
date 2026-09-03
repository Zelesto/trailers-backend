// src/main/java/com/pgsa/trailers/dto/VehicleIssueItemResponseDTO.java
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
public class VehicleIssueItemResponseDTO {
    private Long id;
    private Long itemId;
    private String itemName;
    private String itemCategory;
    private BigDecimal quantityIssued;
    private BigDecimal quantityReturned;
    private BigDecimal quantityOutstanding;
    private String conditionIssued;
    private String conditionReturned;
    private String notes;
    
    // Swap-related fields (added for swap functionality)
    private Boolean isSwap;
    private String swapReason;
    private Long swapIssueId;
}
