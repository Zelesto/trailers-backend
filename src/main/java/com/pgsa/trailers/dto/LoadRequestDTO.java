// src/main/java/com/pgsa/trailers/dto/LoadRequestDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadRequestDTO {
    // ====== CORE IDENTIFIERS ======
    private String loadNumber;        // Auto-generated if null
    private String referenceNumber;   // Used to link trips to same load
    
    // ====== CUSTOMER & DESCRIPTION ======
    private Long customerId;
    private String description;
    
    // ====== MEASUREMENTS ======
    private BigDecimal weightKg;
    private BigDecimal volumeCubicM;
    private Integer palletCount;
    
    // ====== DATES ======
    private LocalDateTime loadingDate;
    private LocalDateTime unloadingDate;
    
    // ====== STATUS & PRIORITY ======
    private String status;      // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    private String priority;    // LOW, NORMAL, HIGH, URGENT
    
    // ====== COMMODITY & CARGO ======
    private String commodityType;
    private String containerNumber;
    
    // ====== SPECIAL HANDLING ======
    private Boolean hazardousMaterial;
    private String specialHandling;
    private String handlingInstructions;
    private String packagingType;
    private String hazardClass;
    private String temperatureRequirements;
    
    // ====== LOCATION ======
    private String originLocation;
    private String destinationLocation;
    
    // ====== FINANCIAL ======
    private BigDecimal estimatedValue;
    private BigDecimal actualValue;
    
    // ====== INSURANCE & CUSTOMS ======
    private String insurancePolicyNumber;
    private LocalDate insuranceExpiry;
    private String customsClearanceStatus;
    
    // ====== RELATIONSHIPS ======
    private Long warehouseId;
    private Long supervisorId;
    
    // ====== TRIPS ======
    private List<Long> tripIds;  // Trips to add to this load
}
