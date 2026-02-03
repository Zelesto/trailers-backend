package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehicleAnalyticsRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT v FROM Vehicle v WHERE v.id = :vehicleId")
    List<Vehicle> findByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query(value = """
            SELECT 
                v.registration_number as registration,
                COALESCE(SUM(tm.total_distance), 0) as totalKm,
                COALESCE(SUM(fs.quantity), 0) as fuelLiters,
                COALESCE(SUM(fs.total_amount), 0) as fuelCost,
                CASE WHEN COALESCE(SUM(fs.quantity), 0) > 0 
                     THEN COALESCE(SUM(tm.total_distance), 0) / SUM(fs.quantity) 
                     ELSE 0 END as kmPerLiter,
                CASE WHEN COALESCE(SUM(tm.total_distance), 0) > 0 
                     THEN COALESCE(SUM(fs.total_amount), 0) / SUM(tm.total_distance) 
                     ELSE 0 END as costPerKm
            FROM vehicle v
            LEFT JOIN trip t ON t.vehicle_id = v.id
            LEFT JOIN trip_metrics tm ON tm.trip_id = t.id
            LEFT JOIN fuel_slip fs ON fs.vehicle_id = v.id 
                AND fs.transaction_date BETWEEN :from AND :to
            WHERE t.planned_start_date BETWEEN :from AND :to
            GROUP BY v.registration_number
            """, nativeQuery = true)
    List<Object[]> vehicleEfficiencyRaw(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}