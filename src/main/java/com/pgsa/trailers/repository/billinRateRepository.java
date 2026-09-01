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

    @Query("SELECT r FROM Rate r WHERE " +
           "(:customerId IS NULL OR r.customer.id = :customerId OR r.customer IS NULL) " +
           "AND (:vehicleType IS NULL OR r.vehicleType.name = :vehicleType OR r.vehicleType IS NULL) " +
           "AND (:destination IS NULL OR r.destinationPattern IS NULL OR UPPER(:destination) LIKE UPPER(CONCAT('%', r.destinationPattern, '%'))) " +
           "AND (:commodity IS NULL OR r.commodity.name = :commodity OR r.commodity IS NULL) " +
           "AND (r.validFrom <= :tripDate AND r.validTo >= :tripDate) " +
           "ORDER BY r.priority DESC")
    List<Rate> findBestMatchingRates(
        @Param("customerId") Long customerId,
        @Param("vehicleType") String vehicleType,
        @Param("destination") String destination,
        @Param("commodity") String commodity,
        @Param("tripDate") LocalDate tripDate
    );

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
