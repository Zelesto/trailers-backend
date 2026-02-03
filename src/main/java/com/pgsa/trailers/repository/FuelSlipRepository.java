package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.ops.FuelSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuelSlipRepository extends JpaRepository<FuelSlip, Long> {

    // ✅ Works fine
    @Query("SELECT f FROM FuelSlip f WHERE f.transactionDate BETWEEN :startDate AND :endDate")
    List<FuelSlip> findByTransactionDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // ✅ Refactored name to avoid derived query parsing
    @Query("""
        SELECT f FROM FuelSlip f
        WHERE f.fuelSource.accountId = :accountId
          AND f.transactionDate BETWEEN :start AND :end
          AND f.finalized IS FALSE
    """)
    List<FuelSlip> findFuelSlipsForAccountWithinDateRangeNotFinalized(
            @Param("accountId") Long accountId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT f FROM FuelSlip f WHERE f.vehicle.id = :vehicleId AND f.transactionDate BETWEEN :start AND :end")
    List<FuelSlip> findByVehicleIdAndDateBetween(
            @Param("vehicleId") Long vehicleId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COALESCE(SUM(f.totalAmount), 0) FROM FuelSlip f WHERE f.transactionDate BETWEEN :from AND :to")
    BigDecimal totalFuelCost(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
    // Add this method
    Long countByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(f.quantity), 0) FROM FuelSlip f WHERE f.vehicle.id = :vehicleId")
    BigDecimal totalFuelLitersByVehicle(@Param("vehicleId") Long vehicleId);

    // Simple derived queries (these work)
    List<FuelSlip> findByDriverId(Long driverId);
    List<FuelSlip> findByVehicleId(Long vehicleId);

    @Query("SELECT f FROM FuelSlip f WHERE f.driver.id = :driverId AND f.vehicle.id = :vehicleId")
    List<FuelSlip> findByDriverIdAndVehicleId(
            @Param("driverId") Long driverId,
            @Param("vehicleId") Long vehicleId
    );


    // Find by trip ID
    List<FuelSlip> findByTripId(Long tripId);

    // Find slips without trip ID (manual entries)
    List<FuelSlip> findByTripIdIsNull();

    // Find slips with trip ID (trip-based entries)
    List<FuelSlip> findByTripIdIsNotNull();


}
