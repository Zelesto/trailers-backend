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
     * Get trip summaries by status - FIXED: Removed isActive filter
     */
    @Query("SELECT new com.pgsa.trailers.dto.TripSummaryDTO(" +
           "t.id, " +
           "t.tripNumber, " +
           "t.status, " +
           "v.registrationNumber, " +
           "CONCAT(COALESCE(d.firstName, ''), ' ', COALESCE(d.lastName, '')), " +
           "t.plannedStartDate, " +
           "t.actualEndDate, " +
           "t.originLocation, " +
           "t.destinationLocation, " +
           "t.originCity, " +
           "t.destinationCity, " +
           "t.originZipCode, " +
           "t.destinationZipCode, " +
           "t.actualDistanceKm, " +
           "t.plannedDistanceKm) " +
           "FROM Trip t " +
           "LEFT JOIN t.vehicle v " +
           "LEFT JOIN t.driver d " +
           "WHERE (:status IS NULL OR t.status = :status)")
    List<TripSummaryDTO> findTripSummariesByStatus(@Param("status") TripStatus status);

    /**
     * Get trip summaries with pagination and filters - FIXED: Removed isActive
     */
    @Query("SELECT new com.pgsa.trailers.dto.TripSummaryDTO(" +
           "t.id, " +
           "t.tripNumber, " +
           "t.status, " +
           "v.registrationNumber, " +
           "CONCAT(COALESCE(d.firstName, ''), ' ', COALESCE(d.lastName, '')), " +
           "t.plannedStartDate, " +
           "t.actualEndDate, " +
           "t.originLocation, " +
           "t.destinationLocation, " +
           "t.originCity, " +
           "t.destinationCity, " +
           "t.originZipCode, " +
           "t.destinationZipCode, " +
           "t.actualDistanceKm, " +
           "t.plannedDistanceKm) " +
           "FROM Trip t " +
           "LEFT JOIN t.vehicle v " +
           "LEFT JOIN t.driver d " +
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
     * Get trip KPIs using DTO projection - FIXED: Removed isActive
     */
    @Query("""
        SELECT new com.pgsa.trailers.dto.TripKpiDTO(
            t.id,
            v.registrationNumber,
            t.actualDistanceKm,
            t.revenueAmount,
            t.costAmount,
            (t.revenueAmount - t.costAmount),
            CASE 
                WHEN t.actualDistanceKm > 0 
                THEN t.costAmount / t.actualDistanceKm 
                ELSE 0 
            END
        )
        FROM Trip t
        JOIN t.vehicle v
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
          AND t.status = 'COMPLETED'
        ORDER BY t.actualEndDate DESC
    """)
    List<TripKpiDTO> findTripKpis(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get trip profitability data - FIXED: Using correct column names
     */
    @Query(value = """
        SELECT 
            t.id,
            t.trip_number,
            t.status,
            t.planned_start_date,
            COALESCE(t.actual_distance_km, 0) as total_distance_km,
            COALESCE(t.revenue_amount, 0) as revenue_amount,
            COALESCE(t.cost_amount, 0) as cost_amount,
            COALESCE(t.revenue_amount - t.cost_amount, 0) as profit,
            COALESCE(t.fuel_consumed_liters, 0) as fuel_used
        FROM trips t
        WHERE t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
            AND DATE(t.actual_end_date) BETWEEN CAST(:startDate AS DATE) AND CAST(:endDate AS DATE)
        ORDER BY t.actual_end_date DESC
        """, nativeQuery = true)
    List<Object[]> findTripProfitabilityRaw(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * Get summary statistics for dashboard - FIXED: Removed isActive
     */
    @Query("""
        SELECT 
            COUNT(t.id) as totalTrips,
            COALESCE(SUM(t.actualDistanceKm), 0) as totalDistance,
            COALESCE(SUM(t.revenueAmount), 0) as totalRevenue,
            COALESCE(SUM(t.costAmount), 0) as totalCost,
            COALESCE(SUM(t.revenueAmount - t.costAmount), 0) as totalProfit,
            COALESCE(AVG(t.revenueAmount - t.costAmount), 0) as avgProfitPerTrip
        FROM Trip t
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
          AND t.status = 'COMPLETED'
    """)
    Object[] findTripSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get trips by status for a date range - FIXED: Removed isActive
     */
    @Query("""
        SELECT 
            t.status,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(t.actualDistanceKm), 0) as totalDistance
        FROM Trip t
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
        GROUP BY t.status
    """)
    List<Object[]> findTripsByStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get top performing vehicles by profit - FIXED: Removed isActive
     */
    @Query("""
        SELECT 
            v.registrationNumber,
            v.vehicleType,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(t.revenueAmount - t.costAmount), 0) as totalProfit,
            COALESCE(AVG(t.revenueAmount - t.costAmount), 0) as avgProfitPerTrip
        FROM Trip t
        JOIN t.vehicle v
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
          AND t.status = 'COMPLETED'
        GROUP BY v.registrationNumber, v.vehicleType
        HAVING COUNT(t.id) > 0
        ORDER BY totalProfit DESC
    """)
    List<Object[]> findTopPerformingVehicles(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get trip profitability by city - FIXED: Removed isActive
     */
    @Query("""
        SELECT 
            t.originCity,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(t.actualDistanceKm), 0) as totalDistance,
            COALESCE(SUM(t.revenueAmount), 0) as totalRevenue,
            COALESCE(SUM(t.costAmount), 0) as totalCost,
            COALESCE(SUM(t.revenueAmount - t.costAmount), 0) as totalProfit
        FROM Trip t
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
          AND t.status = 'COMPLETED'
          AND t.originCity IS NOT NULL
        GROUP BY t.originCity
        ORDER BY totalProfit DESC
    """)
    List<Object[]> findProfitabilityByCity(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get monthly trip statistics - FIXED for PostgreSQL
     */
    @Query(value = """
        SELECT 
            TO_CHAR(t.actual_end_date, 'YYYY-MM') as month,
            COUNT(t.id) as tripCount,
            COALESCE(SUM(t.actual_distance_km), 0) as totalDistance,
            COALESCE(SUM(t.revenue_amount), 0) as totalRevenue,
            COALESCE(SUM(t.cost_amount), 0) as totalCost,
            COALESCE(SUM(t.revenue_amount - t.cost_amount), 0) as totalProfit
        FROM trips t
        WHERE t.status = 'COMPLETED'
            AND DATE(t.actual_end_date) BETWEEN CAST(:startDate AS DATE) AND CAST(:endDate AS DATE)
        GROUP BY TO_CHAR(t.actual_end_date, 'YYYY-MM')
        ORDER BY month DESC
        """, nativeQuery = true)
    List<Object[]> findMonthlyStatistics(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}
