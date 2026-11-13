package com.pgsa.trailers.dto.ops;

import com.pgsa.trailers.entity.ops.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record WorkOrderStatusRequest(@NotNull WorkOrderStatus status) {
}
