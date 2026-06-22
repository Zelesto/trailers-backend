// src/main/java/com/pgsa/trailers/dto/inventory/StockMovementResponseDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponseDTO {
    private Long id;
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private String movementType;
    private String reason;
    private String notes;
    private String referenceNumber;
    private String referenceType;
    private String performedBy;
    private LocalDateTime createdAt;
    private Long tripId;
    private Long fuelSlipId;
    
    // Approval fields
    private Boolean requiresApproval;
    private String approvalStatus; // PENDING, APPROVED, REJECTED
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String approvalNotes;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}
