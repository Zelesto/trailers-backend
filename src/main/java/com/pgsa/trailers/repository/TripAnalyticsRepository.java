package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TripAnalyticsRepository extends Repository<Trip, Long> {

    /**
     * Get trip KPIs using DTO projection (cleaner approach)
     */
    @Query("""
        SELECT new com.pgsa.trailers.dto.TripKpiDTO(
            t.id,
            v.registrationNumber,
            m.totalDistanceKm,
            m.revenueAmount,
            m.costAmount,
            (m.revenueAmount - m.costAmount),
            CASE 
                WHEN m.totalDistanceKm > 0 
                THEN m.costAmount / m.totalDistanceKm 
                ELSE 0 
            END
        )
        FROM Trip t
        JOIN t.vehicle v
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :from AND :to
          AND t.status = 'COMPLETED'
        ORDER BY t.plannedStartDate DESC
    """)
    List<TripKpiDTO> findTripKpis(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Get trip profitability data with all details (for compatibility with existing code)
     */
    @Query("""
        SELECT 
            t.id,
           t.tripNumber, 
            v.registrationNumber,
            v.vehicleType,
            t.plannedStartDate,
            m.totalDistanceKm,
            m.revenueAmount,
            m.costAmount,
            (m.revenueAmount - m.costAmount),
            m.fuelUsedLiters,
            m.totalDurationHours,
            t.status 
        FROM Trip t
        JOIN t.vehicle v
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
        ORDER BY t.plannedStartDate DESC
    """)
    List<Object[]> findTripProfitabilityRaw(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get summary statistics for dashboard
     */
    @Query("""
        SELECT 
            COUNT(t.id) as totalTrips,
            COALESCE(SUM(m.totalDistanceKm), 0) as totalDistance,
            COALESCE(SUM(m.revenueAmount), 0) as totalRevenue,
            COALESCE(SUM(m.costAmount), 0) as totalCost,
            COALESCE(SUM(m.revenueAmount - m.costAmount), 0) as totalProfit,
            COALESCE(AVG(m.revenueAmount - m.costAmount), 0) as avgProfitPerTrip
        FROM Trip t
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
    """)
    Object[] findTripSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get trips by status for a date range
     */
    @Query("""
        SELECT 
            t.status,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(m.totalDistanceKm), 0) as totalDistance
        FROM Trip t
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
        GROUP BY t.status
    """)
    List<Object[]> findTripsByStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get top performing vehicles by profit
     */
    @Query("""
        SELECT 
            v.registrationNumber,
            v.vehicleType,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(m.revenueAmount - m.costAmount), 0) as totalProfit
        FROM Trip t
        JOIN t.vehicle v
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
        GROUP BY v.registrationNumber, v.vehicleType
        HAVING COUNT(t.id) > 0
        ORDER BY totalProfit DESC
        LIMIT 10
    """)
    List<Object[]> findTopPerformingVehicles(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    @Query(value = """
        SELECT 
            t.id as tripId,
            v.registration as vehicleRegistration,
            v.type as vehicleType,
            t.start_time as tripDate,
            t.distance_km as distance,
            t.fuel_used_liters as fuelUsed,
            t.revenue_amount as revenue,
            t.cost_amount as cost,
            (t.revenue_amount - t.cost_amount) as profit,
            t.duration_hours as duration
        FROM trip t
        JOIN vehicles v ON t.vehicle_id = v.id
        WHERE DATE(t.start_time) BETWEEN :startDate AND :endDate
        ORDER BY t.start_time DESC
        """, nativeQuery = true)
    List<Object[]> tripProfitabilityRaw(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
