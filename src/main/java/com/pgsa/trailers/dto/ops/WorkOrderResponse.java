package com.pgsa.trailers.dto.ops;

import com.pgsa.trailers.entity.ops.WorkOrder;
import com.pgsa.trailers.entity.ops.WorkOrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderResponse(
        Long id,
        String workOrderNumber,
        String title,
        Long customerId,
        String scopeOfWork,
        String jobInstructions,
        String deliverables,
        Long assignedDriverId,
        Long assignedVehicleId,
        String teamName,
        LocalDate dueDate,
        WorkOrderStatus status,
        Long sowTemplateId,
        List<WorkOrderProofResponse> proofs,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static WorkOrderResponse from(WorkOrder order) {
        return new WorkOrderResponse(
                order.getId(),
                order.getWorkOrderNumber(),
                order.getTitle(),
                order.getCustomerId(),
                order.getScopeOfWork(),
                order.getJobInstructions(),
                order.getDeliverables(),
                order.getAssignedDriverId(),
                order.getAssignedVehicleId(),
                order.getTeamName(),
                order.getDueDate(),
                order.getStatus(),
                order.getSowTemplateId(),
                order.getProofs().stream().map(WorkOrderProofResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
