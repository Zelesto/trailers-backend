package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.InventoryShrinkageDTO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAnalyticsRepository {

    @Query(value = """
            select
                    i.name as itemName,
            sum(
            case when sm.movement_type='IN' then sm.quantity
            when sm.movement_type='OUT' then -sm.quantity
            else 0 end
    ) as expected,
    scl.counted_quantity as actual,
    scl.variance as shrinkage
    from stock_count_line scl
    join inventory_item i on i.id = scl.item_id
    join stock_movement sm on sm.item_id = i.id
    group by i.name, scl.counted_quantity, scl.variance
    """, nativeQuery = true)
    List<InventoryShrinkageDTO> shrinkage();
}
