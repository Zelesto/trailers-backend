package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
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
}
