package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverAnalyticsRepository extends JpaRepository<Driver, Long> {

    @Query(value = """
        SELECT 
            d.first_name || ' ' || COALESCE(d.last_name, '') as driver_name,
            COALESCE(COUNT(DISTINCT t.id), 0) as trips_completed,
            COALESCE(SUM(t.distance), 0) as total_km,
            COALESCE(SUM(fs.total_amount), 0) as fuel_cost,
            CASE 
                WHEN COALESCE(SUM(fs.quantity), 0) > 0 
                THEN COALESCE(SUM(t.distance), 0) / NULLIF(SUM(fs.quantity), 0)
                ELSE 0 
            END as efficiency_score,
            COALESCE(SUM(t.revenue_amount), 0) as total_revenue,
            COALESCE(SUM(t.cost_amount), 0) as total_cost,
            COALESCE(SUM(t.revenue_amount - t.cost_amount), 0) as profit
        FROM drivers d
        LEFT JOIN trips t ON t.driver_id = d.id 
            AND t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
            AND t.is_active = true
            AND DATE(t.end_date) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
        LEFT JOIN fuel_slip fs ON fs.driver_id = d.id 
            AND fs.is_active = true
            AND DATE(fs.transaction_date) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
        WHERE d.status = 'ACTIVE' AND d.is_active = true
        GROUP BY d.id, d.first_name, d.last_name
        HAVING COALESCE(COUNT(DISTINCT t.id), 0) > 0
        ORDER BY profit DESC
        """, nativeQuery = true)
    List<Object[]> driverPerformanceRaw(
            @Param("from") String from,
            @Param("to") String to
    );
}
