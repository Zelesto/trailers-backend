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
    private String referenceType; // "INVOICE", "PURCHASE_ORDER", "RETURN", "ADJUSTMENT", "MAINTENANCE", "OTHER"
    private String performedBy;
    private Long tripId;
    private Long fuelSlipId;
    private Boolean requiresApproval;
    
    // Add these fields for immediate approval
    private String approvalStatus; // "PENDING", "APPROVED", "REJECTED"
    private String approvedBy;
    private String approvalNotes;
}
