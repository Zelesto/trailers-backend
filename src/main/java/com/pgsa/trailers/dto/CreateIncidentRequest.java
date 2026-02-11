package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateIncidentRequest {
    @NotBlank(message = "Incident type is required")
    private String incidentType;
    
    @NotBlank(message = "Severity is required")
    private String severity;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private String location;
    
    private Boolean requiresAssistance = false;
}
