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
public class StockOnHandDTO {
    private Long id;
    private Long itemId;
    private String itemName;
    private String itemSku;
    private String category;
    private String unitOfMeasure;
    
    // Holder information
    private String holderType; // VEHICLE, DRIVER, LOCATION
    private Long holderId;
    private String holderName;          // Driver name or Vehicle registration
    private String holderIdentifier;    // Vehicle registration, driver name, location name
    private String holderDetails;       // Additional details like vehicle make/model
    
    // Stock information
    private Integer quantityOnHand;
    private Integer quantityIssued;
    private Integer quantityReturned;
    private Integer quantityOutstanding;
    private BigDecimal unitCost;
    private BigDecimal totalValue;
    
    // Status
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK, ON_HOLD, ISSUED
    private Boolean isHeld;
    private String holdReason;
    private String condition;
    
    // Timestamps
    private LocalDateTime issueDate;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime lastUpdated;
    private String notes;
    
    // For grouping
    private String holderDisplayName;   // Formatted display for the holder
}
