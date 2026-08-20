// src/main/java/com/pgsa/trailers/dto/LoadResponseDTO.java
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
public class LoadResponseDTO {
    // ====== CORE IDENTIFIERS ======
    private Long id;
    private String loadNumber;
    private String referenceNumber;
    
    // ====== CUSTOMER & DESCRIPTION ======
    private Long customerId;
    private String customerName;
    private String description;
    
    // ====== MEASUREMENTS ======
    private BigDecimal weightKg;
    private BigDecimal volumeCubicM;
    private Integer palletCount;
    
    // ====== DATES ======
    private LocalDateTime loadingDate;
    private LocalDateTime unloadingDate;
    private LocalDateTime lastStatusUpdate;
    
    // ====== STATUS & PRIORITY ======
    private String status;
    private String priority;
    
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
    private String warehouseName;
    private Long supervisorId;
    private String supervisorName;
    
    // ====== STATISTICS ======
    private Integer tripsCount;
    private Integer completedTrips;
    private Integer pendingTrips;
    private Integer inProgressTrips;
    private Integer totalDistanceKm;
    private Integer totalHoursActive;
    private Integer incidentsLogged;
    
    // ====== DEPOT TRACKING ======
    private BigDecimal totalFromDepotKm;
    private BigDecimal totalToDepotKm;
    private BigDecimal totalDepotKm;
    
    // ====== CALCULATED FIELDS ======
    private BigDecimal totalWeight;
    private BigDecimal totalValue;
    private String statusDisplay;
    private Boolean isActive;
    private Boolean canAcceptTrip;
    
    // ====== MERGE SUGGESTION ======
    private Boolean mergeSuggestion;
    private String mergeMessage;
    
    // ====== AUDIT ======
    private String auditTrail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ====== RELATIONSHIPS ======
    private List<TripSummaryDTO> trips;
}
