package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncidentRequest {
    
    @NotBlank(message = "Incident type is required")
    private String incidentType;
    
    @NotBlank(message = "Severity is required")
    private String severity;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String location;
    
    private Boolean requiresAssistance;
    
    // ====== PAYMENT FIELDS ======
    
    private BigDecimal amount;
    
    private String paymentMethod;
    
    private String referenceNumber;
    
    private String voucherType;
    
    private String eventType;
    
    private String direction;
    
    private String additionalNotes;
}
