package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverIssueRequestDTO {
    private Long driverId;
    private Long tripId;
    private LocalDateTime issueDate;
    private String status;         
    private String notes;
    private List<DriverIssueItemRequestDTO> items;
}
