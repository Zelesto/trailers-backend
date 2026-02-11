package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentStatsDTO {
    private Long totalIncidents;
    private Long activeIncidents;
    private Long urgentIncidents;
}
