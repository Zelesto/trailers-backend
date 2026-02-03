package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.DriverKpiDTO;
import com.pgsa.trailers.entity.assets.DriverMetrics;
import com.pgsa.trailers.entity.assets.Driver;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface DriverAnalyticsRepository extends Repository<DriverMetrics, Long> {

    @Query(value = "...", nativeQuery = true)
    List<Object[]> driverPerformanceRaw();


    @Query(value = """
            select
                    d.first_name || ' ' || d.last_name as driver,
            sum(tm.total_distance) as totalKm,
            sum(fs.total_amount) as fuelCost,
            0 as adverseEvents
    from driver d
    join fuel_slip fs on fs.driver_id = d.id
    join trip t on t.driver_id = d.id
    join trip_metrics tm on tm.trip_id = t.id
    where t.status = 'CLOSED'
    group by driver
    """, nativeQuery = true)
    List<DriverKpiDTO> driverPerformance();
}
