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
import java.time.LocalDateTime;
import java.util.List;

public interface TripAnalyticsRepository extends Repository<Trip, Long> {

    /**
     * Get trip summaries by status
     */
    @Query("""
        SELECT new com.pgsa.trailers.dto.TripSummaryDTO(
            t.id,
            t.tripNumber,
            CAST(t.status AS string),
            v.registrationNumber,
            CONCAT(COALESCE(d.firstName, ''), ' ', COALESCE(d.lastName, '')),
            t.plannedStartDate,
            t.actualEndDate,
            t.originLocation,
            t.destinationLocation,
            t.originCity,
            t.destinationCity,
            t.originZipCode,
            t.destinationZipCode
        )
        FROM Trip t
        LEFT JOIN t.vehicle v
        LEFT JOIN t.driver d
        WHERE (:status IS NULL OR t.status = :status)
        AND t.isActive = true
    """)
    List<TripSummaryDTO> findTripSummariesByStatus(@Param("status") TripStatus status);

    /**
     * Get trip summaries with filters + pagination
     */
    @Query("""
        SELECT new com.pgsa.trailers.dto.TripSummaryDTO(
            t.id,
            t.tripNumber,
            CAST(t.status AS string),
            v.registrationNumber,
            CONCAT(COALESCE(d.firstName, ''), ' ', COALESCE(d.lastName, '')),
            t.plannedStartDate,
            t.actualEndDate,
            t.originLocation,
            t.destinationLocation,
            t.originCity,
            t.destinationCity,
            t.originZipCode,
            t.destinationZipCode
        )
        FROM Trip t
        LEFT JOIN t.vehicle v
        LEFT JOIN t.driver d
        WHERE (:search IS NULL OR LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR t.status = :status)
        AND (:city IS NULL OR LOWER(t.originCity) = LOWER(:city)
                          OR LOWER(t.destinationCity) = LOWER(:city))
        AND t.isActive = true
    """)
    Page<TripSummaryDTO> findTripSummariesWithFilters(
            @Param("search") String search,
            @Param("status") TripStatus status,
            @Param("city") String city,
            Pageable pageable
    );

    /**
     * Trip KPIs
     */
    @Query("""
        SELECT new com.pgsa.trailers.dto.TripKpiDTO(
            t.id,
            t.tripNumber,
            CAST(t.status AS string),
            t.plannedStartDate,
            COALESCE(t.actualDistanceKm, 0),
            COALESCE(t.fuelConsumedLiters, 0),
            COALESCE(t.revenueAmount, 0),
            COALESCE(t.costAmount, 0),
            COALESCE(t.revenueAmount, 0) - COALESCE(t.costAmount, 0),
            CASE
                WHEN COALESCE(t.revenueAmount, 0) > 0 THEN
                    ((COALESCE(t.revenueAmount, 0) - COALESCE(t.costAmount, 0)) * 100)
                    / COALESCE(t.revenueAmount, 0)
                ELSE 0
            END
        )
        FROM Trip t
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
        AND t.status = :status
        AND t.isActive = true
        ORDER BY t.actualEndDate DESC
    """)
    List<TripKpiDTO> findTripKpis(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") TripStatus status
    );

    /**
     * Native profitability query
     */
    @Query(value = """
        SELECT 
            t.id,
            t.trip_number,
            t.status,
            t.planned_start_date,
            COALESCE(t.actual_distance_km, 0),
            COALESCE(t.revenue_amount, 0),
            COALESCE(t.cost_amount, 0),
            COALESCE(t.revenue_amount - t.cost_amount, 0),
            COALESCE(t.fuel_consumed_liters, 0)
        FROM trips t
        WHERE t.is_active = true
        AND t.status IN ('COMPLETED', 'CLOSED', 'FINALIZED')
        AND DATE(t.actual_end_date) BETWEEN CAST(:startDate AS DATE) AND CAST(:endDate AS DATE)
        ORDER BY t.actual_end_date DESC
    """, nativeQuery = true)
    List<Object[]> findTripProfitabilityRaw(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * Dashboard summary
     */
    @Query("""
        SELECT 
            COUNT(t.id),
            COALESCE(SUM(t.actualDistanceKm), 0),
            COALESCE(SUM(t.revenueAmount), 0),
            COALESCE(SUM(t.costAmount), 0),
            COALESCE(SUM(t.revenueAmount - t.costAmount), 0),
            COALESCE(AVG(t.revenueAmount - t.costAmount), 0)
        FROM Trip t
        WHERE t.actualEndDate BETWEEN :startDate AND :endDate
        AND t.status = 'COMPLETED'
        AND t.isActive = true
    """)
    Object findTripSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
