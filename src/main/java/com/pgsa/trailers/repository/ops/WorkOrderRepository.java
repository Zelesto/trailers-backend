package com.pgsa.trailers.repository.ops;

import com.pgsa.trailers.entity.ops.WorkOrder;
import com.pgsa.trailers.entity.ops.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
    List<WorkOrder> findByStatusOrderByCreatedAtDesc(WorkOrderStatus status);
}
