// src/main/java/com/pgsa/trailers/dto/inventory/StockMovementRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementRequestDTO {
    private Long itemId;
    private Integer quantity;
    private String movementType; // "IN", "OUT", "ADJUSTMENT"
    private String reason;
    private String notes;
    private String referenceNumber;
    private Long tripId;
    private Long fuelSlipId;
}
