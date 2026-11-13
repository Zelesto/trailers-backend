package com.pgsa.trailers.dto.ops;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record WorkOrderRequest(
        @NotBlank String title,
        Long customerId,
        @NotBlank String scopeOfWork,
        String jobInstructions,
        String deliverables,
        Long assignedDriverId,
        Long assignedVehicleId,
        String teamName,
        LocalDate dueDate,
        Long sowTemplateId) {
}
