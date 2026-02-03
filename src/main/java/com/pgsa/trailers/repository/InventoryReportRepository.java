package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.InventoryVarianceDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryReportRepository {

    @Query(value = """
            select
                    i.name as itemName,
            sum(
            case when sm.movement_type='IN' then sm.quantity
            when sm.movement_type='OUT' then -sm.quantity
            else 0 end
    ) as systemQty,
    scl.counted_quantity as countedQty,
    scl.variance as variance
    from stock_count_line scl
    join inventory_item i on i.id = scl.item_id
    join stock_movement sm on sm.item_id = i.id
    where scl.stock_count_id = :countId
    group by i.name, scl.counted_quantity, scl.variance
    """, nativeQuery = true)
    List<InventoryVarianceDTO> stockVariance(@Param("countId") Long countId);
}
