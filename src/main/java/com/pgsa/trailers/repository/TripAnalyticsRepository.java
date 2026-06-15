package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Find trip summaries by status with city and zip code fields
     */
    @Query("SELECT new com.pgsa.trailers.dto.TripSummaryDTO(" +
           "t.id, t.tripNumber, t.status, v.registrationNumber, " +
           "CONCAT(COALESCE(d.firstName, ''), ' ', COALESCE(d.lastName, '')), " +
           "t.plannedStartDate, t.actualEndDate, " +
           "t.originLocation, t.destinationLocation, " +
           "t.originCity, t.destinationCity, " +
           "t.originZipCode, t.destinationZipCode, " +
           "m.totalDistanceKm, t.plannedDistanceKm) " +
           "FROM Trip t " +
           "LEFT JOIN t.vehicle v " +
           "LEFT JOIN t.driver d " +
           "LEFT JOIN t.metrics m " +
           "WHERE (:status IS NULL OR t.status = :status)")
    List<TripSummaryDTO> findTripSummariesByStatus(@Param("status") TripStatus status);

    /**
     * Find trip summaries by status with pagination
     */
    @Query("SELECT new com.pgsa.trailers.dto.TripSummaryDTO(" +
           "t.id, t.tripNumber, t.status, v.registrationNumber, " +
           "CONCAT(COALESCE(d.firstName, ''), ' ', COALESCE(d.lastName, '')), " +
           "t.plannedStartDate, t.actualEndDate, " +
           "t.originLocation, t.destinationLocation, " +
           "t.originCity, t.destinationCity, " +
           "t.originZipCode, t.destinationZipCode, " +
           "m.totalDistanceKm, t.plannedDistanceKm) " +
           "FROM Trip t " +
           "LEFT JOIN t.vehicle v " +
           "LEFT JOIN t.driver d " +
           "LEFT JOIN t.metrics m " +
           "WHERE (:search IS NULL OR LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.originCity) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:city IS NULL OR LOWER(t.originCity) = LOWER(:city) OR LOWER(t.destinationCity) = LOWER(:city))")
    Page<TripSummaryDTO> findTripSummariesWithFilters(
            @Param("search") String search,
            @Param("status") TripStatus status,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Get trip profitability data with all details (for compatibility with existing code)
     * FIXED: Changed parameters from LocalDate to String to match service layer
     */
    @Query(value = """
        SELECT 
            t.id,
            t.trip_number,
            t.status,
            t.planned_start_date,
            COALESCE(tm.total_distance_km, 0) as total_distance_km,
            COALESCE(t.revenue_amount, 0) as revenue_amount,
            COALESCE(t.cost_amount, 0) as cost_amount,
            COALESCE(t.revenue_amount - t.cost_amount, 0) as profit,
            COALESCE(t.fuel_consumed_liters, 0) as fuel_used
        FROM trip t
        LEFT JOIN trip_metrics tm ON tm.trip_id = t.id
        WHERE t.actual_end_date BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp)
           OR t.planned_start_date BETWEEN CAST(:startDate AS timestamp) AND CAST(:endDate AS timestamp)
        ORDER BY t.planned_start_date DESC
        """, nativeQuery = true)
    List<Object[]> findTripProfitabilityRaw(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
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
            COALESCE(SUM(m.revenueAmount - m.costAmount), 0) as totalProfit,
            COALESCE(AVG(m.revenueAmount - m.costAmount), 0) as avgProfitPerTrip
        FROM Trip t
        JOIN t.vehicle v
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
        GROUP BY v.registrationNumber, v.vehicleType
        HAVING COUNT(t.id) > 0
        ORDER BY totalProfit DESC
    """)
    List<Object[]> findTopPerformingVehicles(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get trip profitability by city
     */
    @Query("""
        SELECT 
            t.originCity,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(m.totalDistanceKm), 0) as totalDistance,
            COALESCE(SUM(m.revenueAmount), 0) as totalRevenue,
            COALESCE(SUM(m.costAmount), 0) as totalCost,
            COALESCE(SUM(m.revenueAmount - m.costAmount), 0) as totalProfit
        FROM Trip t
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
          AND t.originCity IS NOT NULL
        GROUP BY t.originCity
        ORDER BY totalProfit DESC
    """)
    List<Object[]> findProfitabilityByCity(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get monthly trip statistics
     */
    @Query("""
        SELECT 
            FUNCTION('DATE_FORMAT', t.plannedStartDate, '%Y-%m') as month,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(m.totalDistanceKm), 0) as totalDistance,
            COALESCE(SUM(m.revenueAmount), 0) as totalRevenue,
            COALESCE(SUM(m.costAmount), 0) as totalCost,
            COALESCE(SUM(m.revenueAmount - m.costAmount), 0) as totalProfit
        FROM Trip t
        JOIN t.metrics m
        WHERE t.plannedStartDate BETWEEN :startDate AND :endDate
        GROUP BY FUNCTION('DATE_FORMAT', t.plannedStartDate, '%Y-%m')
        ORDER BY month DESC
    """)
    List<Object[]> findMonthlyStatistics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
