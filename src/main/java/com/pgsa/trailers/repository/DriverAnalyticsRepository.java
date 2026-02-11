package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.DriverKpiDTO;
import com.pgsa.trailers.entity.assets.DriverMetrics;
import com.pgsa.trailers.entity.assets.Driver;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface DriverAnalyticsRepository extends JpaRepository<Driver, Long> {

   @Query(value = """
    SELECT 
        d.first_name || ' ' || d.last_name as driver_name,
        COALESCE(COUNT(DISTINCT t.id), 0) as trips_completed,
        COALESCE(SUM(tm.total_distance_km), 0) as total_km,
        COALESCE(SUM(fs.total_amount), 0) as fuel_cost,
        COALESCE(AVG(tm.total_distance_km / NULLIF(fs.quantity, 0)), 0) as efficiency_score,
        COALESCE(SUM(t.revenue_amount), 0) as total_revenue,
        COALESCE(SUM(t.cost_amount), 0) as total_cost,
        COALESCE(SUM(t.revenue_amount - t.cost_amount), 0) as profit
    FROM driver d
    LEFT JOIN trip t ON t.driver_id = d.id 
        AND t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
        AND DATE(t.actual_end_date) BETWEEN :from AND :to
    LEFT JOIN trip_metrics tm ON tm.trip_id = t.id
    LEFT JOIN fuel_slip fs ON fs.driver_id = d.id 
        AND DATE(fs.transaction_date) BETWEEN :from AND :to
    WHERE d.status = 'ACTIVE'
    GROUP BY d.id, d.first_name, d.last_name
    """, nativeQuery = true)
List<Object[]> driverPerformanceRaw(
        @Param("from") String from,  // ✅ String parameters
        @Param("to") String to
);
}
