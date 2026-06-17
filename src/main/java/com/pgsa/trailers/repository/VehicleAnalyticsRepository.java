package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleAnalyticsRepository extends JpaRepository<Vehicle, Long> {

    @Query(value = """
        SELECT 
            v.registration_number as registration,
            COALESCE(SUM(t.actual_distance_km), 0) as totalKm,
            COALESCE(SUM(fs.quantity), 0) as fuelLiters,
            COALESCE(SUM(fs.total_amount), 0) as fuelCost,
            CASE WHEN COALESCE(SUM(fs.quantity), 0) > 0 
                 THEN COALESCE(SUM(t.actual_distance_km), 0) / NULLIF(SUM(fs.quantity), 0)
                 ELSE 0 END as kmPerLiter,
            CASE WHEN COALESCE(SUM(t.actual_distance_km), 0) > 0 
                 THEN COALESCE(SUM(fs.total_amount), 0) / NULLIF(SUM(t.actual_distance_km), 0)
                 ELSE 0 END as costPerKm,
            COUNT(DISTINCT t.id) as tripCount
        FROM vehicle v
        LEFT JOIN trip t ON t.vehicle_id = v.id 
            AND t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
            AND t.is_active = true
            AND DATE(t.actual_end_date) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
        LEFT JOIN fuel_slip fs ON fs.vehicle_id = v.id 
            AND fs.is_active = true
            AND DATE(fs.transaction_date) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
        WHERE v.is_active = true
        GROUP BY v.registration_number
        ORDER BY kmPerLiter DESC
        """, nativeQuery = true)
    List<Object[]> vehicleEfficiencyRaw(
            @Param("from") String from,
            @Param("to") String to
    );

    @Query(value = """
        SELECT 
            v.registration_number,
            v.make,
            v.model,
            v.year,
            COALESCE(SUM(t.actual_distance_km), 0) as total_distance,
            COALESCE(SUM(t.revenue_amount), 0) as total_revenue,
            COALESCE(SUM(t.cost_amount), 0) as total_cost,
            COALESCE(SUM(t.revenue_amount - t.cost_amount), 0) as total_profit,
            COUNT(DISTINCT t.id) as trip_count
        FROM vehicle v
        LEFT JOIN trip t ON t.vehicle_id = v.id 
            AND t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
            AND t.is_active = true
            AND DATE(t.actual_end_date) BETWEEN CAST(:from AS DATE) AND CAST(:to AS DATE)
        WHERE v.is_active = true
        GROUP BY v.registration_number, v.make, v.model, v.year
        HAVING COUNT(DISTINCT t.id) > 0
        ORDER BY total_profit DESC
        """, nativeQuery = true)
    List<Object[]> findTopVehiclesByProfit(
            @Param("from") String from,
            @Param("to") String to
    );
}
