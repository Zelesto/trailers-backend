package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehicleAnalyticsRepository extends JpaRepository<Vehicle, Long> {

    @Query(value = """
        SELECT 
            v.registration_number as registration,
            COALESCE(SUM(tm.total_distance_km), 0) as totalKm,
            COALESCE(SUM(fs.quantity), 0) as fuelLiters,
            COALESCE(SUM(fs.total_amount), 0) as fuelCost,
            CASE WHEN COALESCE(SUM(fs.quantity), 0) > 0 
                 THEN COALESCE(SUM(tm.total_distance_km), 0) / SUM(fs.quantity) 
                 ELSE 0 END as kmPerLiter,
            CASE WHEN COALESCE(SUM(tm.total_distance_km), 0) > 0 
                 THEN COALESCE(SUM(fs.total_amount), 0) / SUM(tm.total_distance_km) 
                 ELSE 0 END as costPerKm
        FROM vehicle v
        LEFT JOIN trip t ON t.vehicle_id = v.id 
            AND t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
            AND DATE(t.actual_end_date) BETWEEN :from AND :to
        LEFT JOIN trip_metrics tm ON tm.trip_id = t.id
        LEFT JOIN fuel_slip fs ON fs.vehicle_id = v.id 
            AND DATE(fs.transaction_date) BETWEEN :from AND :to
        GROUP BY v.registration_number
        """, nativeQuery = true)
    List<Object[]> vehicleEfficiencyRaw(
            @Param("from") String from,  // ✅ Change to String
            @Param("to") String to       // ✅ Change to String
    );
}
