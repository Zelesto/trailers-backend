package com.pgsa.trailers.repository.billing;

import com.pgsa.trailers.entity.billing.Rate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RateRepository extends JpaRepository<Rate, Long> {

    /**
     * ✅ Primary method used by {@link com.pgsa.trailers.service.billing.RateResolverService}.
     *
     * Returns all rates whose {@code validFrom}/{@code validTo} range covers
     * {@code tripDate}. Deliberately simple: it only constrains on the date
     * columns, so NULL-customer / NULL-vehicle-type defaults (e.g. your
     * production rows with {@code priority = -100}) are always included.
     *
     * All other matching (customer, vehicle type, destination, commodity,
     * schedule) is done in Java inside {@code RateResolverService} so that
     * NULL-on-the-rate-side consistently means "matches anything".
     *
     * Ordered by priority (highest first) then id, so the result is
     * deterministic even when multiple rates match.
     */
    @Query("""
        SELECT r FROM Rate r
        WHERE r.validFrom <= :tripDate
          AND (r.validTo IS NULL OR r.validTo >= :tripDate)
        ORDER BY r.priority DESC, r.id ASC
    """)
    List<Rate> findAllValidAtDate(@Param("tripDate") LocalDate tripDate);

    // ==================================================================
    // Legacy methods — kept for backwards compatibility with any other
    // callers. Not used by RateResolverService anymore, but safe to keep.
    // ==================================================================

    /**
     * NULL-safe best-match query. Superseded by {@link #findAllValidAtDate}
     * + Java-side filtering, but retained in case other code paths still
     * call it.
     */
    @Query("""
        SELECT r FROM Rate r
        WHERE (:customerId  IS NULL OR r.customer IS NULL OR r.customer.id = :customerId)
          AND (:vehicleType IS NULL OR r.vehicleType IS NULL
                                   OR r.vehicleType.name = :vehicleType)
          AND (r.destinationPattern IS NULL
               OR :destination IS NULL
               OR LOWER(:destination) LIKE LOWER(CONCAT('%', r.destinationPattern, '%')))
          AND (r.commodity IS NULL
               OR :commodityId IS NULL
               OR r.commodity.id = :commodityId)
          AND r.validFrom <= :tripDate
          AND (r.validTo IS NULL OR r.validTo >= :tripDate)
        ORDER BY
          CASE WHEN r.customer IS NULL THEN 1 ELSE 0 END,
          r.priority DESC
    """)
    List<Rate> findBestMatchingRates(
            @Param("customerId")  Long customerId,
            @Param("vehicleType") String vehicleType,
            @Param("destination") String destination,
            @Param("commodityId") Long commodityId,
            @Param("tripDate")    LocalDate tripDate);

    /**
     * Customer-specific rates valid on a given date.
     */
    @Query("""
        SELECT r FROM Rate r
        WHERE r.customer.id = :customerId
          AND r.validFrom <= :tripDate
          AND (r.validTo IS NULL OR r.validTo >= :tripDate)
        ORDER BY r.priority DESC
    """)
    List<Rate> findCustomerRates(
            @Param("customerId") Long customerId,
            @Param("tripDate")   LocalDate tripDate);

    /**
     * Default (customer-agnostic) rates valid on a given date.
     */
    @Query("""
        SELECT r FROM Rate r
        WHERE r.customer IS NULL
          AND r.validFrom <= :tripDate
          AND (r.validTo IS NULL OR r.validTo >= :tripDate)
        ORDER BY r.priority DESC
    """)
    List<Rate> findDefaultRates(@Param("tripDate") LocalDate tripDate);

    /**
     * Rates matching a specific vehicle type, valid on a given date.
     * Note: uses {@code r.vehicleType.name} — the correct property on the
     * {@code VehicleType} entity. The previous version referenced
     * {@code r.vehicleType.vehicleType}, which does not exist.
     */
    @Query("""
        SELECT r FROM Rate r
        WHERE r.vehicleType.name = :vehicleType
          AND r.validFrom <= :tripDate
          AND (r.validTo IS NULL OR r.validTo >= :tripDate)
    """)
    List<Rate> findRatesByVehicleType(
            @Param("vehicleType") String vehicleType,
            @Param("tripDate")    LocalDate tripDate);
}
