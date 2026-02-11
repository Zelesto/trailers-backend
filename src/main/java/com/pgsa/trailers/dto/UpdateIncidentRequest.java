package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class UpdateIncidentRequest {
    private Boolean resolved;
    private String resolutionNotes;
}
