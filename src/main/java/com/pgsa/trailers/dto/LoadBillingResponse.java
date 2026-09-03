// src/main/java/com/pgsa/trailers/dto/LoadBillingResponse.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;  // ✅ ADD THIS IMPORT

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadBillingResponse {
    private Long id;
    private String loadNumber;
    private Long vehicleId;
    private Long driverId;
    private Long tripId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String currency;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String notes;
    private List<BillingItemDTO> items;  // Requires import
    private List<BillingChargeDTO> charges;  // Requires import
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
