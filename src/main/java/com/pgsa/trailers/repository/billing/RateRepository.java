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

    @Query("""
    SELECT r FROM Rate r
    WHERE (:customerId  IS NULL OR r.customer IS NULL OR r.customer.id = :customerId)
      AND (:vehicleType IS NULL OR r.vehicleType IS NULL
                               OR r.vehicleType.vehicleType = :vehicleType)
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

    @Query("SELECT r FROM Rate r WHERE " +
           "(r.customer.id = :customerId) " +
           "AND (r.validFrom <= :tripDate AND r.validTo >= :tripDate) " +
           "ORDER BY r.priority DESC")
    List<Rate> findCustomerRates(
        @Param("customerId") Long customerId,
        @Param("tripDate") LocalDate tripDate
    );

    @Query("SELECT r FROM Rate r WHERE " +
           "(r.customer IS NULL) " +
           "AND (r.validFrom <= :tripDate AND r.validTo >= :tripDate) " +
           "ORDER BY r.priority DESC")
    List<Rate> findDefaultRates(@Param("tripDate") LocalDate tripDate);

    @Query("SELECT r FROM Rate r WHERE " +
           "r.vehicleType.name = :vehicleType " +
           "AND (r.validFrom <= :tripDate AND r.validTo >= :tripDate)")
    List<Rate> findRatesByVehicleType(
        @Param("vehicleType") String vehicleType,
        @Param("tripDate") LocalDate tripDate
    );
}
