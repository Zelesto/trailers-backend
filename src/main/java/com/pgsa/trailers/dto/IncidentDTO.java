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
public class IncidentDTO {
    
    private Long id;
    private Long tripId;
    private String tripNumber;
    private String incidentType;
    private String severity;
    private String description;
    private String location;
    private Boolean requiresAssistance;
    private Boolean resolved;
    private String resolutionNotes;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // ====== PAYMENT FIELDS ======
    
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNumber;
    private String voucherType;
    private String eventType;
    private String direction;
    private String additionalNotes;
}
